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
import uz.oybek.ozamiz.data.ai.AiHistoryRepository
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

    private val _messages = MutableStateFlow(listOf(
        ChatMessage(
            text = "Assalomu Aleykum ${UserRepository.user.name}! 😊 Men sizning shaxsiy dietologingizman. Bugun nima yemoqchisiz, mashqlar bo'yicha maslahatlar yoki vazn tashlash haqida har qanday savolingiz bo'lsa, bemalol so'rashingiz mumkin",
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
        _suggestedQuestions.value = emptyList()
    }

    fun sendMessage(text: String, isInitialContext: Boolean = false) {
        if (text.isBlank()) return

        val userMessage = ChatMessage(text = text, isUser = true)
        _messages.value = _messages.value + userMessage
        _showQuestions.value = false

        if (!isInitialContext) {
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
            val systemInstruction = buildSystemInstruction(text, user) // Pass the user object

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

                    // Auto-save menu if it was an initial recommendation request
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
                text = "Assalomu Aleykum ${UserRepository.user.name}! 😊 Ozamiz hamjamiyatiga xush kelibsiz. Sizning maqsadingiz — **${UserRepository.user.targetWeight?.toInt() ?: 60} kg** vaznga sog'lom yo'l bilan erishish.\n\nMen sizning shaxsiy AI-dietologingizman. Bugun nima yeyishni rejalashtirish, mashqlar bo'yicha maslahatlar yoki vazn tashlash jarayoni haqida har qanday savolingiz bo'lsa, bemalol so'rashingiz mumkin. Qayerdan boshlaymiz?",
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
                - Maqsadi: ${user.targetWeight ?: "vazn saqlash"} kg
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
                - Maqsadi: ${user.targetWeight ?: "vazn saqlash"} kg
                - Yeganlari: $foodNames. (Yegan miqdori: $totalCalories kkal. Kunlik me'yor: $targetCalories kkal)
                - Ichgan suvi: $waterConsumed ml (Kunlik me'yor: $targetWater ml)
                - Mashqi: $exerciseDetails
            """.trimIndent()
        }

        val isFullMenuRequest = userMessage.contains("menyu", ignoreCase = true) || userMessage.contains("ovqat", ignoreCase = true)
        val isNonushtaRequest = userMessage.contains("nonushta", ignoreCase = true) || userMessage.contains("ertalab", ignoreCase = true)
        val isTushlikRequest = userMessage.contains("tushlik", ignoreCase = true)
        val isKechkiOvqatRequest = userMessage.contains("kechki ovqat", ignoreCase = true) || userMessage.contains("kechki", ignoreCase = true)
        val isMenuRequest = isFullMenuRequest || isNonushtaRequest || isTushlikRequest || isKechkiOvqatRequest

        var historyContext = ""
        if (isMenuRequest) {
            val lastMenus = AiMenuRepository.getLastMenus(3)
            if (lastMenus.isNotEmpty()) {
                historyContext = "\n🔴 OLDINGI MENYULAR (ULARDAGI TAOMLARNI TAKRORLAMANG):\n" +
                        lastMenus.joinToString("\n---\n") + "\n"
            }
        }

        if (isMenuRequest) {
            val baseMenuInstruction = """
                🔴 QAT'IY QOIDALAR (BUZILISHI TAQIQLANADI):
                1. SALOMLASHMANG! "Mana menyu", "Yoqimli ishtaha" kabi ortiqcha gaplarni UMUMAN yozmang. Faqat londa va aniq ma'lumot bering.
                2. ILMIY SO'ZLAR TAQIQLANADI! "Kaloriya", "Uglevod" demang.
                3. EMOJILAR QOIDASI: Har bir mahal uchun 🌅, ☀️, 🌙 emojilarini, menyu variantlari uchun ♦ emojisini, maslahat layfxaklari uchun esa 📌 emojisini ishlating. Boshqa emojilarni ishlatmang.
                4. XILMA-XILLIK: Oldingi berilgan menyulardagi taomlarni takrorlamang! Har safar yangi va qiziqarli variantlar taklif qiling.
                
            """.trimIndent() + "\n" + userInfoSection + historyContext
            
            val specificMenuInstruction = if (isNonushtaRequest || isTushlikRequest || isKechkiOvqatRequest) {
                val mealName = when {
                    isNonushtaRequest -> "Nonushta"
                    isTushlikRequest -> "Tushlik"
                    isKechkiOvqatRequest -> "Kechki ovqat"
                    else -> "" // Should not happen
                }
                val mealEmoji = when {
                    isNonushtaRequest -> "🌅"
                    isTushlikRequest -> "☀️"
                    isKechkiOvqatRequest -> "🌙"
                    else -> "" // Should not happen
                }
                """
                
                🔴 MENYU TUZISH QOIDALARI:
                - BOSHIDA HECH QANDAY UMUMIY ORTIQCHA GAPLR BO'LMASIN, QISQA BULSIN!
                - O'ZBEKONA TAOMLAR: Mastava, sho'rva, dimlama, grechka, tuxum, qatiq kabi oddiy ovqatlar bo'lsin.
                - PORSIYA MIQDORI: Har doim faqat "1" raqamini ishlatmang! Ovqatga mos keladigan, tushunarli va xilma-xil o'lchovlardan foydalaning. Masalan: "ikki dona", "ozgina", "yarim piyola", "bir kosa", "bir nechta bo'lak", "o'rtacha bir dona", "150 gramm" kabi.
                - MAJBURIY: ${mealEmoji} **${mealName} uchun variantlar:** sarlavhasi ostida har doim **5 TADAN variant** bering. Undan kamiga yo'l qo'ymang!
                - TAVSIYA (LAYFXAK): Har doim menyu oxirida bitta "💡 Foydali usul" berish MAJBURIY.
                
                JAVOBINGIZ AYNAN SHU FORMATDA BO'LSIN (Hech narsani tushirib qoldirmang):
    
                ${mealEmoji} **${mealName} uchun variantlar:**
                ♦ 1-variant: [Ovqat nomi va tushunarli, xilma-xil miqdori]
                ♦ 2-variant: [Ovqat nomi va tushunarli, xilma-xil miqdori]
                ♦ 3-variant: [Ovqat nomi va tushunarli, xilma-xil miqdori]
                ♦ 4-variant: [Ovqat nomi va tushunarli, xilma-xil miqdori]
                ♦ 5-variant: [Ovqat nomi va tushunarli, xilma-xil miqdori]
    
                💡 **Foydali usul:** 
                [Bu yerga 1 ta juda qisqa va aniq amaliy layfxak yozing]
                """.trimIndent()
            } else { 
                """
                
                🔴 MENYU TUZISH QOIDALARI:
                - BOSHIDA HECH QANDAY UMUMIY ORTIQCHA GAPLR BO'LMASIN, QISQA BULSIN!
                - MAJBURIY: Hech qaysi mahalni tushirib qoldirmang! Faqat maslahat berib qutulmang!
                - O'ZBEKONA TAOMLAR: Mastava, sho'rva, dimlama, grechka, tuxum, qatiq kabi oddiy ovqatlar bo'lsun.
                - PORSIYA MIQDORI: Har doim faqat "1" raqamini ishlatmang! Ovqatga mos keladigan, tushunarli va xilma-xil o'lchovlardan foydalaning. Masalan: "ikki dona", "ozgina", "yarim piyola", "bir kosa", "bir nechta bo'lak", "o'rtacha bir dona", "150 gramm" kabi.
                - HAR BIR MAHAL (Nonushta, Tushlik, Kechki ovqat) UCHUN MAJBURIY 5 TADAN variant bering.
                - TAVSIYA (LAYFXAK): Har doim menyu oxirida bitta "💡 Foydali usul" berish MAJBURIY.
                
                JAVOBINGIZ AYNAN SHU FORMATDA BO'LSIN (Hech narsani tushirib qoldirmang):
    
                🌅 **Nonushta, Tushlik, Kechki ovqat:**
                ♦ 1-variant: [Ovqat nomi va tushunarli, xilma-xil miqdori]
                ♦ 2-variant: [Ovqat nomi va tushunarli, xilma-xil miqdori]
                ♦ 3-variant: [Ovqat nomi va tushunarli, xilma-xil miqdori]
                ♦ 4-variant: [Ovqat nomi va tushunarli, xilma-xil miqdori]
                ♦ 5-variant: [Ovqat nomi va tushunarli, xilma-xil miqdori]
    
                ☀️ **Tushlik:**
                ♦ 1-variant: [Ovqat nomi, tushunarli, xilma-xil miqdori va salat turi]
                ♦ 2-variant: [Ovqat nomi, tushunarli, xilma-xil miqdori va salat turi]
                ♦ 3-variant: [Ovqat nomi, tushunarli, xilma-xil miqdori va salat turi]
                ♦ 4-variant: [Ovqat nomi, tushunarli, xilma-xil miqdori va salat turi]
                ♦ 5-variant: [Ovqat nomi, tushunarli, xilma-xil miqdori va salat turi]
    
                🌙 **Kechki ovqat:**
                ♦ 1-variant: [Yengil ovqat nomi va tushunarli, xilma-xil miqdori]
                ♦ 2-variant: [Yengil ovqat nomi va tushunarli, xilma-xil miqdori]
                ♦ 3-variant: [Yengil ovqat nomi va tushunarli, xilma-xil miqdori]
                ♦ 4-variant: [Yengil ovqat nomi va tushunarli, xilma-xil miqdori]
                ♦ 5-variant: [Yengil ovqat nomi va tushunarli, xilma-xil miqdori]
    
                💡 **Foydali usul:** 
                [Bu yerga 1 ta juda qisqa va aniq amaliy layfxak yozing]
                """.trimIndent()
            }
            return baseMenuInstruction + specificMenuInstruction
        } else {
            return """
                Sizning vazifangiz: Foydalanuvchiga do'stona, g'amxo'r va malakali hamda oddiy til bilan tushuntiruvchi inson-diyetolog sifatida suhbat qurish.
                Sizdan menyu so'ralmagan, shuning uchun hech qanday qat'iy formatlar, ro'yxatlar kerak emas! Huddi yaqin do'stdek odamdek gaplashing qisqa, sodda qilib javob bering. Oddiy savollariga javob bering yoki maslahat bering. 
                Suhbatda ochiq va do'stona ohangdan (masalan: "Yaxshi ketyapsiz", "Hechqisi yo'q, davom etamiz", "Ajoyib!") foydalaning. Emojilarni o'z o'rnida ishlating.
                
            """.trimIndent() + "\n" + userInfoSection + "\n" + """
                
                ESLATMA:
                - Haqiqiy odamdek gaplashing.
                - Har doim foydalanuvchiga hurmat bilan "Siz" deb murojaat qiling ("qildingiz", "qiling", "yaxshimisiz"). Aslo "sen" deb gapirmang!
                - Hech qanday "1, 2, 3" formatda yoki qattiq algoritmda javob bermang, qisqa odmi matn yozing!
                - Juda chuqur ilmiy so'zlar, kaloriya va grammlar bilan foydalanuvchini charchatmang. Iloji boricha oddiy piyola, qoshiq kabi o'zbekona o'lchovlar ishlating!
            """.trimIndent()
        }
    }

    companion object {
        private val ALL_QUESTIONS = emptyList<String>()
    }
}