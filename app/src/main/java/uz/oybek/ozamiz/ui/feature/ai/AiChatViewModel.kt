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
import uz.oybek.ozamiz.appContext
import uz.oybek.ozamiz.data.ai.AiChatRepository
import kotlinx.coroutines.flow.first
import uz.oybek.ozamiz.data.ai.AiMenuRepository
import uz.oybek.ozamiz.data.ai.model.ChatMessage
import uz.oybek.ozamiz.data.logs.DailyLogRepository
import uz.oybek.ozamiz.data.logs.model.DailyLogs
import uz.oybek.ozamiz.data.logs.model.DailySummary
import uz.oybek.ozamiz.data.user.UserRepository
import uz.oybek.ozamiz.data.user.model.User
import uz.oybek.ozamiz.data.utils.DataWrapper
import java.util.Date

class AiChatViewModel : ViewModel() {

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private fun getInitialMessage(): String {
        val u = UserRepository.user
        val goalText = if (u.goal == "gain_weight") "sog'lom semirish (vazn yig'ish)" else "sog'lom ozish"
        val targetWeight = u.targetWeight?.toInt() ?: 60
        return "Assalomu Aleykum ${u.name}! 😊 Ozamiz ilovasiga xush kelibsiz. Sizning maqsadingiz — **$targetWeight kg** vaznga erishish va $goalText.\n\nMen sizning shaxsiy AI-dietologingizman. Bugun nima yeyishni rejalashtirish, mashqlar bo'yicha maslahatlar yoki maqsadingizga erishish haqida har qanday savolingiz bo'lsa, bemalol so'rashingiz mumkin. Qayerdan boshlaymiz?"
    }

    private val _messages = MutableStateFlow(listOf(
        ChatMessage(
            text = getInitialMessage(),
            isUser = false
        )
    ))
    val messages: StateFlow<List<ChatMessage>> = combine(_messages, _isLoading) { msgs, loading ->
        if (loading) {
            msgs + ChatMessage(text = "", isUser = false, isThinking = true)
        } else {
            msgs
        }
    }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    private val _showQuestions = MutableStateFlow(false)
    val showQuestions: StateFlow<Boolean> = _showQuestions.asStateFlow()

    private val _suggestedQuestions = MutableStateFlow<List<String>>(emptyList())
    val suggestedQuestions: StateFlow<List<String>> = _suggestedQuestions.asStateFlow()

    val userName: String = UserRepository.user.name

    private val user = UserRepository.user

    private var chatIdDeferred = kotlinx.coroutines.CompletableDeferred<String?>()

    init {
        refreshSuggestions()
        // startNewChatSession() is now called by the UI when it wants a fresh chat
        // or we could load the last active chat here.
        // For now, let's ensure a session exists if messages are empty
        if (_messages.value.isEmpty()) {
            startNewChat()
        }
    }

    private fun startNewChatSession() {
        chatIdDeferred = kotlinx.coroutines.CompletableDeferred()
    }

    private fun refreshSuggestions() {
        _suggestedQuestions.value = emptyList()
    }

    fun sendMessage(text: String, isInitialContext: Boolean = false) {
        if (text.isBlank()) return

        val userMessage = ChatMessage(text = text, isUser = true)
        _messages.value = _messages.value + userMessage
        _showQuestions.value = false
        _isLoading.value = true

        viewModelScope.launch {
            val systemInstruction = buildSystemInstruction(text, user) // Pass the user object

            var aiMessageAdded = false
            var currentAiMessageText = ""
            var lastUpdateTimestamp = 0L

            AiChatRepository.sendMessageStream(text, systemInstruction).collect { result ->
                when (result) {
                    is DataWrapper.Success -> {
                        currentAiMessageText = result.data
                        val now = System.currentTimeMillis()
                        
                        // Throttle updates to ~10fps during streaming for UI stability
                        if (now - lastUpdateTimestamp > 100 || !aiMessageAdded) {
                            lastUpdateTimestamp = now
                            _isLoading.value = false

                            if (!aiMessageAdded) {
                                val newMessage = ChatMessage(text = currentAiMessageText, isUser = false)
                                _messages.value = _messages.value + newMessage
                                aiMessageAdded = true
                            } else {
                                val currentList = _messages.value.toMutableList()
                                if (currentList.isNotEmpty()) {
                                    val lastIdx = currentList.size - 1
                                    currentList[lastIdx] = currentList[lastIdx].copy(text = currentAiMessageText)
                                    _messages.value = currentList
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

            // Final update to ensure the last chunk is displayed
            var finalAiMessage: ChatMessage? = null
            if (aiMessageAdded) {
                val currentList = _messages.value.toMutableList()
                val lastIdx = currentList.size - 1
                val updated = currentList[lastIdx].copy(text = currentAiMessageText)
                currentList[lastIdx] = updated
                _messages.value = currentList
                finalAiMessage = updated
            }

            finalAiMessage?.let { aiMsg ->
                val chatId = chatIdDeferred.await() ?: return@let
                try {

                    if (isInitialContext && (text.contains("menyu", true) || text.contains("ovqat", true))) {
                        AiMenuRepository.saveMenu(appContext, aiMsg.text)
                    }
                } catch (_: Exception) { }
            }
        }
    }

    fun startNewChat() {
        _messages.value = listOf(
            ChatMessage(
                text = getInitialMessage(),
                isUser = false
            )
        )
        _showQuestions.value = true
        refreshSuggestions()
        AiChatRepository.clearHistory()
        startNewChatSession()
    }

    fun saveMenu(text: String) {
        viewModelScope.launch {
            AiMenuRepository.saveMenu(appContext, text)
        }
    }

    private suspend fun buildSystemInstruction(userMessage: String, user: User): String { // Accept user object here
        var targetDate = Date()
        var dateContextText = "BUGUNGI HOLATI"
        var isWeekAnalysis = false

        if (userMessage.contains("Kechagi", ignoreCase = true)) {
            val cal = java.util.Calendar.getInstance()
            cal.add(java.util.Calendar.DAY_OF_YEAR, -1)
            targetDate = cal.time
            dateContextText = "KECHAGI HOLATI"
        } else if (userMessage.contains("hafta", ignoreCase = true) || userMessage.contains("haftalik", ignoreCase = true)) {
            isWeekAnalysis = true
        }

        val userInfoSection: String
        if (isWeekAnalysis) {
            val cal = java.util.Calendar.getInstance()
            var totalCal = 0
            var totalWater = 0
            var totalBurned = 0
            for (i in 0..6) {
                val d = cal.time
                val summaryWrapper = DailyLogRepository.observeDailySummary(d).first()
                if (summaryWrapper is DataWrapper.Success) {
                    totalCal += summaryWrapper.data.consumedCalories
                    totalWater += summaryWrapper.data.consumedWaterMl
                    totalBurned += summaryWrapper.data.burnedCalories
                }
                cal.add(java.util.Calendar.DAY_OF_YEAR, -1)
            }
            val avgCal = totalCal / 7
            val avgWater = totalWater / 7
            val avgBurned = totalBurned / 7

            userInfoSection = """
                🔴 FOYDALANUVCHINING OXIRGI HAFTADAGI O'RTACHA HOLATI:
                - Maqsadi: ${user.goal} (Maqsad vazn: ${user.targetWeight ?: "saqlash"} kg)
                - O'rtacha kunlik yegan miqdori: $avgCal kkal. (Me'yor: ${user.targetCalories} kkal)
                - O'rtacha kunlik ichgan suvi: $avgWater ml (Kunlik me'yor: ${user.targetWaterMl} ml)
                - O'rtacha kunlik mashq qilingan yog': $avgBurned kkal
            """.trimIndent()
        } else {
            val summaryWrapper = DailyLogRepository.observeDailySummary(targetDate).first()
            val logsWrapper = DailyLogRepository.observeDailyLogs(targetDate).first()

            val summary = if (summaryWrapper is DataWrapper.Success) summaryWrapper.data else DailySummary()
            val logs = if (logsWrapper is DataWrapper.Success) logsWrapper.data else null

            val foodNames = logs?.foodLogs?.values?.map { it.description }?.joinToString(", ") ?: "Hali ovqat kiritilmadi"
            val totalCalories = summary?.consumedCalories ?: 0
            val targetCalories = user.targetCalories
            val waterConsumed = summary?.consumedWaterMl ?: 0
            val targetWater = user.targetWaterMl

            val exerciseDetails = logs?.exerciseLogs?.values?.joinToString(", ") {
                "${it.exerciseType} (${it.durationMin} daq)"
            } ?: "Mashq qilinmadi"

            userInfoSection = """
                🔴 FOYDALANUVCHINING $dateContextText:
                - Maqsadi: ${user.goal} (Maqsad vazn: ${user.targetWeight ?: "saqlash"} kg)
                - Yeganlari: $foodNames. (Yegan miqdori: $totalCalories kkal. Kunlik me'yor: $targetCalories kkal)
                - Ichgan suvi: $waterConsumed ml (Kunlik me'yor: $targetWater ml)
                - Mashqi: $exerciseDetails
            """.trimIndent()
        }

        return """
                Sizning vazifangiz: Foydalanuvchiga do'stona, g'amxo'r va malakali hamda oddiy til bilan tushuntiruvchi inson-diyetolog sifatida suhbat qurish.
                Foydalanuvchi bilan huddi yaqin do'stdek gaplashing, qisqa va sodda javob bering. Ovqatlar yoki mashqlar bo'yicha savollariga javob bering yoki maslahat bering. 
                Suhbatda ochiq va do'stona ohangdan (masalan: "Yaxshi ketyapsiz", "Hechqisi yo'q, davom etamiz", "Ajoyib!") foydalaning. Emojilarni o'z o'rnida ishlating.
                
            """.trimIndent() + "\n" + userInfoSection + "\n" + """
                
                ESLATMA:
                - Haqiqiy odamdek gaplashing.
                - Har doim foydalanuvchiga hurmat bilan "Siz" deb murojaat qiling. Aslo "sen" deb gapirmang!
                - Hech qanday "1, 2, 3" formatda yoki qattiq algoritmda javob bermang, qisqa odmi matn yozing!
                - Juda chuqur ilmiy so'zlar, kaloriya va grammlar bilan foydalanuvchini charchatmang.
                - Agar menyu so'ralsa, uni 5 xil variantda emas, balki bitta yoki ikkita tavsiya sifatida oddiy matn ko'rinishida yozing.
            """.trimIndent()
    }
}