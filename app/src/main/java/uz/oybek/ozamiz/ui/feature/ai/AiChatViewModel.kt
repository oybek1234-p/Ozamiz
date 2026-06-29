package uz.oybek.ozamiz.ui.feature.ai

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import uz.oybek.ozamiz.appContext
import uz.oybek.ozamiz.data.ai.AiChatRepository
import uz.oybek.ozamiz.data.ai.AiMenuRepository
import uz.oybek.ozamiz.data.ai.Dietolog
import uz.oybek.ozamiz.data.ai.model.ChatMessage
import uz.oybek.ozamiz.data.config.LocalPreferencesRepository
import uz.oybek.ozamiz.data.logs.DailyLogRepository
import uz.oybek.ozamiz.data.logs.model.DailySummary
import uz.oybek.ozamiz.data.user.UserRepository
import uz.oybek.ozamiz.data.user.model.User
import uz.oybek.ozamiz.data.utils.DataWrapper
import uz.oybek.ozamiz.data.utils.DateUtils
import java.util.Date

class AiChatViewModel : ViewModel() {

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private fun getInitialMessage(): String {
        val greetings = listOf(
            "Ozish haqida xohlagan savolingizni bering, javob berishga tayyorman!",
            "Bugun o'zingizni qanday his qilyapsiz? Yordamim kerakmi?",
            "Men sizning shaxsiy dietologingizman, tortinmay savol beravering.",
            "Nima yesam ozaman deb o'ylayapsizmi? Darhol so'rang!",
            "Vazn tashlash qiyinmi? Qayerda xato qilyapsiz, keling topamiz.",
            "Men doim shu yerdaman. Qaysi ovqatni yeyishga ikkilanyapsiz?"
        )
        return greetings.random()
    }
    
    private val _messages = MutableStateFlow(
        listOf(
            ChatMessage(
                text = getInitialMessage(),
                isUser = false
            )
        )
    )
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

    val isUserPremium: Boolean
        get() = user.premium

    private var chatIdDeferred = kotlinx.coroutines.CompletableDeferred<String?>()

    init {
        UserRepository.setUnreadNotificationsZero()
        refreshSuggestions()

        val cached = loadMessagesFromCache()
        if (cached != null) {
            _messages.value = cached
            AiChatRepository.syncHistory(cached)
        }
        startNewChatSession()

        viewModelScope.launch {
            Dietolog.latestMessage.collect { advice ->
                if (!advice.isNullOrBlank()) {
                    val currentMsgs = _messages.value
                    // Prevent duplicate injections
                    if (!currentMsgs.any { it.text == advice }) {
                        val newMsgs = currentMsgs + ChatMessage(text = advice, isUser = false)
                        _messages.value = newMsgs
                        saveMessagesToCache(newMsgs)

                        launch {
                            val chatId = chatIdDeferred.await()
                            if (chatId != null) {
                                AiChatRepository.saveMessageToFirestore(
                                    chatId,
                                    advice,
                                    false,
                                    System.currentTimeMillis()
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    private fun startNewChatSession() {
        val todayStr = DateUtils.getFormattedDateString(Date(), "yyyy-MM-dd")
        val chatId = "${user.uid}_$todayStr"
        chatIdDeferred = kotlinx.coroutines.CompletableDeferred()
        chatIdDeferred.complete(chatId)
    }

    private fun refreshSuggestions() {
        _suggestedQuestions.value = emptyList()
    }

    fun sendMessage(text: String, isInitialContext: Boolean = false, done: () -> Unit = {}) {
        if (text.isBlank()) return

        val userMessage = ChatMessage(text = text, isUser = true)
        val currentWithUser = _messages.value + userMessage
        saveMessagesToCache(currentWithUser)
        _messages.value = currentWithUser
        _showQuestions.value = false
        _isLoading.value = true

        viewModelScope.launch {
            val systemInstruction = buildSystemInstruction(text, user) // Pass the user object

            val chatId = chatIdDeferred.await()
            if (chatId != null) {
                AiChatRepository.saveMessageToFirestore(
                    chatId,
                    text,
                    true,
                    System.currentTimeMillis()
                )
            }

            var aiMessageAdded = false
            var currentAiMessageText = ""
            var lastUpdateTimestamp = 0L

            AiChatRepository.sendMessageStream(text, systemInstruction).collect { result ->
                when (result) {
                    is DataWrapper.Success -> {
                        currentAiMessageText = result.data
                        val now = System.currentTimeMillis()

                        if (now - lastUpdateTimestamp > 100 || !aiMessageAdded) {
                            lastUpdateTimestamp = now
                            _isLoading.value = false

                            if (!aiMessageAdded) {
                                val newMessage =
                                    ChatMessage(text = currentAiMessageText, isUser = false)
                                _messages.value += newMessage
                                aiMessageAdded = true
                            } else {
                                val currentList = _messages.value.toMutableList()
                                if (currentList.isNotEmpty()) {
                                    val lastIdx = currentList.size - 1
                                    currentList[lastIdx] =
                                        currentList[lastIdx].copy(text = currentAiMessageText)
                                    _messages.value = currentList
                                }
                            }
                            done()
                        }
                    }

                    is DataWrapper.Error -> {
                        _isLoading.value = false
                    }

                }
            }

            var finalAiMessage: ChatMessage? = null
            if (aiMessageAdded) {
                val currentList = _messages.value.toMutableList()
                val lastIdx = currentList.size - 1
                val updated = currentList[lastIdx].copy(text = currentAiMessageText)
                currentList[lastIdx] = updated
                _messages.value = currentList
                saveMessagesToCache(currentList)
                finalAiMessage = updated
            }

            finalAiMessage?.let { aiMsg ->
                val chatId = chatIdDeferred.await() ?: return@let
                try {
                    AiChatRepository.saveMessageToFirestore(
                        chatId,
                        aiMsg.text,
                        false,
                        System.currentTimeMillis()
                    )

                    if (isInitialContext && (text.contains("menyu", true) || text.contains(
                            "ovqat",
                            true
                        ))
                    ) {
                        AiMenuRepository.saveMenu(appContext, aiMsg.text)
                    }
                } catch (_: Exception) {
                }
            }
        }
    }

    private fun saveMessagesToCache(messages: List<ChatMessage>) {
        val todayStr = DateUtils.getFormattedDateString(Date(), "yyyy-MM-dd")
        val jsonArray = JSONArray()
        messages.forEach {
            val obj = JSONObject()
            obj.put("text", it.text)
            obj.put("isUser", it.isUser)
            jsonArray.put(obj)
        }
        LocalPreferencesRepository.setAiChatData(todayStr, jsonArray.toString())
    }

    private fun loadMessagesFromCache(): List<ChatMessage>? {
        val todayStr = DateUtils.getFormattedDateString(Date(), "yyyy-MM-dd")
        val jsonStr = LocalPreferencesRepository.getAiChatData(todayStr) ?: return null

        val list = mutableListOf<ChatMessage>()
        try {
            val jsonArray = JSONArray(jsonStr)
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                list.add(
                    ChatMessage(
                        text = obj.getString("text"),
                        isUser = obj.getBoolean("isUser")
                    )
                )
            }
            return if (list.isEmpty()) null else list
        } catch (e: Exception) {
            return null
        }
    }

    fun startNewChat() {
        val cached = loadMessagesFromCache()
        if (cached != null) {
            _messages.value = cached
            _showQuestions.value = false
            refreshSuggestions()
            AiChatRepository.syncHistory(cached)
            startNewChatSession()
        } else {
            startNewChatSession()
            _isLoading.value = true
            _messages.value = listOf(ChatMessage(text = "", isUser = false, isThinking = true))

            viewModelScope.launch {
                val summaryWrapper = DailyLogRepository.observeDailySummary(Date()).first()
                val sum =
                    if (summaryWrapper is DataWrapper.Success) summaryWrapper.data else DailySummary()
                val curHour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)

                val contextGreeting = Dietolog.generateContextualGreeting(
                    goal = user.goal, targetWeight = user.targetWeight?.toFloat(),
                    targetCalories = user.targetCalories, consumedCalories = sum.consumedCalories,
                    targetWater = user.targetWaterMl, consumedWater = sum.consumedWaterMl,
                    targetExercise = 400, burnedExercise = sum.burnedCalories,
                    currentHour = curHour
                )

                val greetingText = contextGreeting?.first ?: getInitialMessage()
                val initialList = listOf(ChatMessage(text = greetingText, isUser = false))

                _messages.value = initialList
                saveMessagesToCache(initialList)
                _showQuestions.value = true
                _isLoading.value = false

                if (contextGreeting != null && contextGreeting.second.isNotEmpty()) {
                    _suggestedQuestions.value = contextGreeting.second
                } else {
                    val staticQuestions = listOf(
                        "Qorin yo'qotish uchun ovqatlar",
                        "Shirinlik o'rniga nima yesam bo'ladi?",
                        "Kunlik energiya uchun tanlov"
                    )
                    _suggestedQuestions.value = staticQuestions
                }
                AiChatRepository.syncHistory(initialList)
            }
        }
    }

    fun saveMenu(text: String) {
        viewModelScope.launch {
            AiMenuRepository.saveMenu(appContext, text)
        }
    }

    private suspend fun buildSystemInstruction(
        userMessage: String,
        user: User
    ): String {
        var targetDate = Date()
        var dateContextText = "BUGUNGI HOLATI"
        var isWeekAnalysis = false

        if (userMessage.contains("Kechagi", ignoreCase = true)) {
            val cal = java.util.Calendar.getInstance()
            cal.add(java.util.Calendar.DAY_OF_YEAR, -1)
            targetDate = cal.time
            dateContextText = "KECHAGI HOLATI"
        } else if (userMessage.contains(
                "hafta",
                ignoreCase = true
            ) || userMessage.contains("haftalik", ignoreCase = true)
        ) {
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

            val summary =
                if (summaryWrapper is DataWrapper.Success) summaryWrapper.data else DailySummary()
            val logs = if (logsWrapper is DataWrapper.Success) logsWrapper.data else null

            val foodNames = logs?.foodLogs?.values?.map { it.description }?.joinToString(", ")
                ?: "Hali ovqat kiritilmadi"
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
                Sizning vazifangiz: Foydalanuvchiga do'stona, g'amxo'r, lekin o'ta qisqa va qat'iy inson-diyetolog sifatida suhbat qurish.
                Suhbatda "Assalomu aleykum", foydalanuvchining ismini aytish yoki "Sog'lom turmush tarzi", "a'lo natija", "shijoat" kabi ortiqcha va balandparvoz so'zlarni UMUMAN ISHLATMANG!
                To'g'ridan-to'g'ri maslahatga o'ting.
                
            """.trimIndent() + "\n" + userInfoSection + "\n" + """
                
                ESLATMA:
                - Ilmiy so'zlarni (protein, uglevod, murakkab uglevod, lipid) ishlatmang! Ularning o'rniga oddiy xalq tushunadigan so'zlarni yarating (masalan: go'sht, tuxum, sabzavot, meva, yog'li, yog'siz).
                - Har doim javobingiz juda qisqa va londa bo'lsin. Hech qanday "1, 2, 3" formatda javob bermang, qisqa odmi matn yozing!
                - Agar menyu so'ralsa, uni juda oddiy tilda va bitta yoki ikkita tavsiya sifatida oddiy matn ko'rinishida yozing.
                - Hech qanday maxsus formatlar (masalan: * yoki # belgilar) ishlata ko'rmang, faqat oddiy matnda yozing! Markdown taqiqlanadi!
            """.trimIndent()
    }
}