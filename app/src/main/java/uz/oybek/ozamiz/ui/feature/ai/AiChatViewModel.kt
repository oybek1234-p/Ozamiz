package uz.oybek.ozamiz.ui.feature.ai

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import uz.oybek.ozamiz.data.ai.AiChatRepository
import uz.oybek.ozamiz.data.ai.AiHistoryRepository
import uz.oybek.ozamiz.data.ai.model.ChatMessage
import uz.oybek.ozamiz.data.logs.DailyLogRepository
import uz.oybek.ozamiz.data.logs.model.DailySummary
import uz.oybek.ozamiz.data.user.UserRepository
import uz.oybek.ozamiz.data.utils.DataWrapper
import java.util.Date

class AiChatViewModel : ViewModel() {

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val messages: StateFlow<List<ChatMessage>> = combine(_messages, _isLoading) { msgs, loading ->
        if (loading) {
            msgs + ChatMessage(text = "", isUser = false, isThinking = true)
        } else {
            msgs
        }
    }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    private val _showQuestions = MutableStateFlow(true)
    val showQuestions: StateFlow<Boolean> = _showQuestions.asStateFlow()

    private val _suggestedQuestions = MutableStateFlow<List<String>>(emptyList())
    val suggestedQuestions: StateFlow<List<String>> = _suggestedQuestions.asStateFlow()

    val userName: String = UserRepository.user.name

    private var dailySummary: DailySummary? = null
    private val user = UserRepository.user

    private var chatIdDeferred = kotlinx.coroutines.CompletableDeferred<String?>()

    init {
        observeDailySummary()
        refreshSuggestions()
        startNewChatSession()
    }

    private fun startNewChatSession() {
        chatIdDeferred = kotlinx.coroutines.CompletableDeferred()
        viewModelScope.launch {
            try {
                val id = AiHistoryRepository.createNewChat(user.uid)
                chatIdDeferred.complete(id)
            } catch (_: Exception) {
                chatIdDeferred.complete(null)
            }
        }
    }

    private fun refreshSuggestions() {
        _suggestedQuestions.value = ALL_QUESTIONS.shuffled().take(5)
    }

    private fun observeDailySummary() {
        viewModelScope.launch {
            DailyLogRepository.observeDailySummary(Date()).collect { wrapper ->
                if (wrapper is DataWrapper.Success) {
                    dailySummary = wrapper.data
                }
            }
        }
    }

    fun sendMessage(text: String, isInitialContext: Boolean = false) {
        if (text.isBlank()) return

        if (!isInitialContext) {
            val userMessage = ChatMessage(text = text, isUser = true)
            _messages.value = _messages.value + userMessage
            _showQuestions.value = false

            viewModelScope.launch {
                val chatId = chatIdDeferred.await() ?: return@launch
                try {
                    AiHistoryRepository.saveMessage(user.uid, chatId, userMessage)
                    if (_messages.value.count { it.isUser } == 1) {
                        val title = text.take(40)
                        AiHistoryRepository.updateChatTitle(user.uid, chatId, title)
                    }
                } catch (_: Exception) { }
            }
        }
        
        _isLoading.value = true
        
        viewModelScope.launch {
            val systemInstruction = buildSystemInstruction()
            
            var aiMessageAdded = false
            var currentAiMessage: ChatMessage? = null

            AiChatRepository.sendMessageStream(text, systemInstruction).collect { result ->
                when (result) {
                    is DataWrapper.Success -> {
                        _isLoading.value = false
                        
                        if (!aiMessageAdded) {
                            val newMessage = ChatMessage(text = result.data, isUser = false)
                            currentAiMessage = newMessage
                            _messages.value = _messages.value + newMessage
                            aiMessageAdded = true
                        } else {
                            currentAiMessage?.let { msg ->
                                val updatedMessage = msg.copy(text = result.data)
                                val currentList = _messages.value.toMutableList()
                                val index = currentList.indexOf(msg)
                                if (index != -1) {
                                    currentList[index] = updatedMessage
                                    _messages.value = currentList
                                    currentAiMessage = updatedMessage
                                }
                            }
                        }
                    }
                    is DataWrapper.Error -> {
                        _isLoading.value = false
                    }
                    else -> {}
                }
            }

            currentAiMessage?.let { aiMsg ->
                val chatId = chatIdDeferred.await() ?: return@let
                try {
                    AiHistoryRepository.saveMessage(user.uid, chatId, aiMsg)
                } catch (_: Exception) { }
            }
        }
    }

    fun startNewChat() {
        _messages.value = emptyList()
        _showQuestions.value = true
        refreshSuggestions()
        AiChatRepository.clearHistory()
        startNewChatSession()
    }

    private fun buildSystemInstruction(): String {
        val summary = dailySummary
        val kcalLeft = user.targetCalories - (summary?.consumedCalories ?: 0)
        val proteinConsumed = summary?.consumedProtein ?: 0.0
        val fatConsumed = summary?.consumedFat ?: 0.0
        val carbsConsumed = summary?.consumedCarbs ?: 0.0
        val waterConsumed = summary?.consumedWaterMl ?: 0
        val burnedKcal = summary?.burnedCalories ?: 0
        val currentStreak = user.streakInfo?.currentStreak ?: 0

        val genderStr = if (user.gender == 0) "Erkak" else if (user.gender == 1) "Ayol" else "Noma'lum"
        val activityStr = when(user.activityLevel) {
            "SEDENTARY" -> "Kam harakat"
            "LIGHTLY_ACTIVE" -> "Yengil faol"
            "MODERATELY_ACTIVE" -> "O'rtacha faol"
            "VERY_ACTIVE" -> "Juda faol"
            "EXTRA_ACTIVE" -> "Ekstremal faol"
            else -> user.activityLevel
        }

        return """
            Sen O'zbekistondagi eng tajribali diyetologsan.
            
            🔴 FOYDALANUVCHI PROFILI:
            - Ismi: ${user.name}
            - Yosh: ${user.age}
            - Jins: $genderStr
            - Bo'yi: ${user.height} sm
            - Hozirgi vazni: ${user.weight} kg
            - Boshlang'ich vazni: ${user.startWeight ?: "noma'lum"} kg
            - Maqsadi: ${user.targetWeight ?: "vazn saqlash"} kg
            - Kunlik harakati: $activityStr
            - Kasalliklar/Muammolar: ${user.problems?.joinToString(", ") ?: "yo'q"}
            - Hudud: ${user.region}
 
            🔴 BUGUNGI HOLAT:
            - Tanovul qilingan: ${summary?.consumedCalories ?: 0} kkal
            - Limit: ${user.targetCalories} kkal
            - Qolgan limit: $kcalLeft kkal
            - Sarflangan (mashq): $burnedKcal kkal
            - Makrolar: Protein: $proteinConsumed g, Yog': $fatConsumed g, Uglevod: $carbsConsumed g
            - Suv: $waterConsumed / ${user.targetWaterMl} ml
            - Streak (davomiylik): $currentStreak kun
 
            🔴 OLTIN QOIDALAR:
            . Yuqoridagi barcha ma'lumotlarni (vazn, maqsad, makrolar) inobatga olgan holda aniq maslahat ber.
            . O'zbekistonning mahalliy sharoiti va milliy taomlarini (lekin sog'lom usulda) yaxshi bilishingni ko'rsat.
            . Doimo motivatsiya ber va uni qo'llab-quvvatla.
            
            🔴 FORMATLASH QOIDALARI:
            - FAQAT qisqa va lo'nda javob ber. Uzun matnlardan qoch.
            - Muhim so'zlarni **bold** (ikki yulduzcha) bilan belgi.
            - Jadvallar (|), uchta yulduzcha (***) yoki murakkab belgilarni ISHLATMA.
            - Yangi qatorlar (\n) va ro'yxatlardan unumli foydalan.
            - Ortiqcha gapirmasdan, darhol maqsadga o't.
            O'zbeklar chunadigan suzlarni ishlat, muloyim bo'l
            
            Muloqot uslubi: Do'stona, professional va juda lo'nda.
        """.trimIndent()
    }

    companion object {
        private val ALL_QUESTIONS = listOf(
            // Eng "og'riqli" savollar (Non, xamir, shirinlik)
            "Nonni butunlay yemasligim shartmi?",
            "Somsa yoki manti yesam bo'ladimi?",
            "Shirinlik yegim kelyapti, nima qilay?",
            "Palovni tushlikda yesam semiramanmi?",
            "Qaysi mevalar semirtiradi?",
            "Hozir nima yesam bo'ladi?",
            "Kechga nima ovqat qilsam semirmayman?",
            // Muammoli vaziyatlar (Ochlik, ko'p yeb qo'yish)
            "Ko'p yeb qo'ydim, endi nima qilay?",
            "Kechasi qornim ochib ketyapti...",
            "To'yga borayapman, nima maslahat berasan?",
            // Qorin va ozish sirlari
            "Faqat qorinni yo'qotish uchun nima yey?",
            "Emizikli ayollarga qanday ozish mumkin?",
            "Uyda mashq qilmasdan ozsa bo'ladimi?",
            "18:00 dan keyin suv ichsam shishmaymanmi?",
            "Ozish uchun eng arzon menyu qaysi?"
        )
    }
}
