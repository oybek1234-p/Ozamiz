package uz.oybek.ozamiz.ui.feature.ai

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import uz.oybek.ozamiz.R
import uz.oybek.ozamiz.appContext
import uz.oybek.ozamiz.data.ai.AiChatRepository
import uz.oybek.ozamiz.data.ai.AiMenuRepository
import uz.oybek.ozamiz.data.ai.AiPromptBuilder
import uz.oybek.ozamiz.data.ai.Dietolog
import uz.oybek.ozamiz.data.ai.local.AiChatDatabase
import uz.oybek.ozamiz.data.ai.local.AiChatMessageEntity
import uz.oybek.ozamiz.data.ai.model.ChatMessage
import uz.oybek.ozamiz.data.config.LocalPreferencesRepository
import uz.oybek.ozamiz.data.user.UserRepository
import uz.oybek.ozamiz.data.utils.DataWrapper
import uz.oybek.ozamiz.data.utils.DateUtils
import uz.oybek.ozamiz.ui.feature.premium.isPremiumOrTrial
import java.util.Date

class AiChatViewModel : ViewModel() {

    companion object {
        const val FREE_DAILY_MESSAGE_LIMIT = 5
    }

    private val aiChatDao = AiChatDatabase.getDatabase(appContext).aiChatDao()
    private val todayStr = DateUtils.getFormattedDateString(Date(), "yyyy-MM-dd")

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private fun getInitialMessage(): String {
        val regDate = UserRepository.user.registerDateLong
        if (regDate != null) {
            val oneHourMs = 60 * 60 * 1000L
            if (System.currentTimeMillis() - regDate <= oneHourMs) {
                return appContext.getString(R.string.new_user_welcome_message)
            }
        }
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

    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
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
        get() = UserRepository.user.isPremiumOrTrial()

    fun getDailyAiMessageCount(): Int {
        return LocalPreferencesRepository.getDailyAiMessageCount(todayStr)
    }

    fun canSendAiMessage(): Boolean {
        return isUserPremium || getDailyAiMessageCount() < FREE_DAILY_MESSAGE_LIMIT
    }

    private var chatIdDeferred = kotlinx.coroutines.CompletableDeferred<String?>()

    init {
        UserRepository.setUnreadNotificationsZero()
        refreshSuggestions()

        viewModelScope.launch(Dispatchers.IO) {
            aiChatDao.clearOldMessages(todayStr)
            val cachedEntities = aiChatDao.getMessagesForDateSync(todayStr)
            val cachedMessages =
                cachedEntities.map { ChatMessage(text = it.text, isUser = it.isUser) }

            withContext(Dispatchers.Main) {
                if (cachedMessages.isNotEmpty()) {
                    _messages.value = cachedMessages
                    AiChatRepository.syncHistory(cachedMessages)
                    startNewChatSession()
                } else {
                    startNewChat()
                }
            }
        }

        viewModelScope.launch {
            // No longer automatically collecting Dietolog.latestMessage here to avoid cluttering chat history
            /*
            Dietolog.latestMessage.collect { advice ->
                if (!advice.isNullOrBlank()) {
                    val currentMsgs = _messages.value
                    if (!currentMsgs.any { it.text == advice }) {
                        val newMsgs = currentMsgs + ChatMessage(text = advice, isUser = false)
                        _messages.value = newMsgs
                    }
                }
            }
            */
        }
    }

    private fun startNewChatSession() {
        val chatId = "${user.uid}_$todayStr"
        chatIdDeferred = kotlinx.coroutines.CompletableDeferred()
        chatIdDeferred.complete(chatId)
    }

    private fun refreshSuggestions() {
        _suggestedQuestions.value = emptyList()
    }

    fun startNewChat() {
        startNewChatSession()
        _isLoading.value = true
        _messages.value = listOf(ChatMessage(text = "", isUser = false, isThinking = true))

        viewModelScope.launch {
            aiChatDao.clearAllMessages()
            AiChatRepository.clearHistory()

            val greetingText = getInitialMessage()
            val initialList = listOf(ChatMessage(text = greetingText, isUser = false))

            _messages.value = initialList
            saveMessageToRoom(greetingText, false)

            _showQuestions.value = true
            _isLoading.value = false

            val staticQuestions = listOf(
                "Kaloriya qanday sanaladi?",
                "Qanchada natijaga erishaman?",
                "Uglevod, Oqsil, Yog' nimalar ?",
                "Shirinlik o'rniga nima yesam bo'ladi?",
                "Kaloriya sanash ishonchlimi?"
            )
            _suggestedQuestions.value = staticQuestions
            AiChatRepository.syncHistory(initialList)
        }
    }

    private suspend fun saveMessageToRoom(text: String, isUser: Boolean) {
        val entity = AiChatMessageEntity(
            text = text,
            isUser = isUser,
            timestamp = System.currentTimeMillis(),
            dateString = todayStr
        )
        aiChatDao.insertMessage(entity)
    }

    fun sendMessage(text: String, isInitialContext: Boolean = false, done: () -> Unit = {}) {
        if (text.isBlank()) return
        if (!canSendAiMessage()) return

        val userMessage = ChatMessage(text = text, isUser = true)
        _messages.value += userMessage
        _showQuestions.value = false
        _isLoading.value = true

        viewModelScope.launch {
            saveMessageToRoom(text, true)

            val chatId = chatIdDeferred.await()
            if (chatId != null) {
                AiChatRepository.saveMessageToFirestore(
                    chatId,
                    text, true,
                    System.currentTimeMillis()
                )
            }
            var aiMessageAdded = false
            var currentAiMessageText = ""

            AiChatRepository.sendMessageStream(systemPrompt, text).collect { result ->
                if (result is DataWrapper.Success) {
                    currentAiMessageText = result.data
                    _isLoading.value = false

                    val displayedText = extractStreamingJsonMessage(currentAiMessageText)

                    if (!aiMessageAdded) {
                        if (!isUserPremium) {
                            LocalPreferencesRepository.incrementDailyAiMessageCount(todayStr)
                        }
                        _messages.value += ChatMessage(
                            text = displayedText,
                            isUser = false
                        )
                        aiMessageAdded = true
                    } else {
                        val currentList = _messages.value.toMutableList()
                        currentList[currentList.lastIndex] =
                            currentList.last().copy(text = displayedText)
                        _messages.value = currentList
                    }
                    done()
                }
            }

            if (aiMessageAdded) {
                val finalParsed = extractJsonMessage(currentAiMessageText) ?: extractStreamingJsonMessage(currentAiMessageText)
                val currentList = _messages.value.toMutableList()
                currentList[currentList.lastIndex] = currentList.last().copy(text = finalParsed)
                _messages.value = currentList

                saveMessageToRoom(finalParsed, false)

                val parsedQuestions = extractJsonReplies(currentAiMessageText)
                if (parsedQuestions.isNotEmpty()) {
                    _showQuestions.value = true
                    _suggestedQuestions.value = parsedQuestions
                }

                if (chatId != null) {
                    AiChatRepository.saveMessageToFirestore(
                        chatId,
                        finalParsed,
                        false,
                        System.currentTimeMillis()
                    )
                    if (isInitialContext && (text.contains("menyu", true) || text.contains(
                            "ovqat",
                            true
                        ))
                    ) {
                        AiMenuRepository.saveMenu(appContext, finalParsed)
                    }
                }
            }
        }
    }

    private fun extractStreamingJsonMessage(text: String): String {
        val keyIndex = text.indexOf("\"message\"")
        if (keyIndex != -1) {
            val colonIndex = text.indexOf(":", keyIndex + 9)
            if (colonIndex != -1) {
                val quoteStart = text.indexOf("\"", colonIndex + 1)
                if (quoteStart != -1) {
                    val builder = StringBuilder()
                    var escaped = false
                    var i = quoteStart + 1
                    while (i < text.length) {
                        val c = text[i]
                        if (escaped) {
                            builder.append(c)
                            escaped = false
                        } else if (c == '\\') {
                            escaped = true
                        } else if (c == '"') {
                            break
                        } else {
                            builder.append(c)
                        }
                        i++
                    }
                    return builder.toString()
                }
            }
        }
        val trimmed = text.trim()
        if (trimmed.startsWith("{") || trimmed.startsWith("[")) {
            return ""
        }
        return text
    }

    private fun extractJsonMessage(text: String): String? {
        return try {
            val jsonStart = text.indexOf("{")
            val jsonEnd = text.lastIndexOf("}")
            if (jsonStart != -1 && jsonEnd != -1) {
                val jsonString = text.substring(jsonStart, jsonEnd + 1)
                val obj = org.json.JSONObject(jsonString)
                obj.getString("message")
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }

    private fun extractJsonReplies(text: String): List<String> {
        return try {
            val jsonStart = text.indexOf("{")
            val jsonEnd = text.lastIndexOf("}")
            if (jsonStart != -1 && jsonEnd != -1) {
                val jsonString = text.substring(jsonStart, jsonEnd + 1)
                val obj = org.json.JSONObject(jsonString)
                val arr = obj.getJSONArray("replies")
                val result = mutableListOf<String>()
                for (i in 0 until arr.length()) {
                    result.add(arr.getString(i))
                }
                result
            } else {
                emptyList()
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun saveMenu(text: String) {
        viewModelScope.launch {
            AiMenuRepository.saveMenu(appContext, text)
        }
    }
}