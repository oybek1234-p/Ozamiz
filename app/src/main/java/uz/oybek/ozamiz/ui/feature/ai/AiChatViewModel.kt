package uz.oybek.ozamiz.ui.feature.ai

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import uz.oybek.ozamiz.data.ai.aiChat.AiChatRepository
import uz.oybek.ozamiz.data.ai.aiChat.ChatMessage
import uz.oybek.ozamiz.data.ai.aiChat.DietologAnswers
import uz.oybek.ozamiz.data.config.AppPreference
import uz.oybek.ozamiz.data.user.UserRepository
import uz.oybek.ozamiz.data.user.local.user
import uz.oybek.ozamiz.data.utils.DataWrapper
import uz.oybek.ozamiz.data.utils.dateApiFormat
import uz.oybek.ozamiz.ui.feature.premium.isPremium
import java.util.Date

class AiChatViewModel : ViewModel() {

    companion object {
        const val FREE_MESSAGE_LIMIT = 5
    }

    private val todayStr = Date().dateApiFormat()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val loadingMessage = ChatMessage(text = "", isUser = false, isThinking = true)
    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())

    private var chatIdDeferred = CompletableDeferred<String?>()
    val suggestedQuestions: StateFlow<List<String>> = AiChatRepository.suggestedReplies

    val messages: StateFlow<List<ChatMessage>> =
        combine(_messages, _isLoading) { msgs, loading ->
            if (loading) {
                msgs + loadingMessage
            } else {
                msgs
            }
        }.stateIn(
            viewModelScope,
            SharingStarted.Lazily,
            emptyList()
        )

    fun canSendMessage(): Boolean {
        if (user.isPremium()) return true
        return AppPreference.current.dietologMessageCount < FREE_MESSAGE_LIMIT
    }

    init {
        UserRepository.setUnreadNotificationsZero()
    }

    private fun startNewChatSession() {
        val chatId = "${user.uid}_$todayStr"
        chatIdDeferred = CompletableDeferred()
        chatIdDeferred.complete(chatId)
    }

    init {
        startNewChat()
    }
    
    fun startNewChat() {
        startNewChatSession()
        viewModelScope.launch {
            AiChatRepository.clearHistory()
            _messages.value += ChatMessage(
                text = DietologAnswers.getInitialGreeting(),
                isUser = false,
                isThinking = false
            )
        }
    }

    fun sendMessage(
        text: String,
        done: () -> Unit = {}
    ) {
        if (text.isBlank()) return
        if (!canSendMessage()) return

        val userMessage = ChatMessage(text = text, isUser = true)
        _messages.value += userMessage
        AiChatRepository.clearReplies()
        _isLoading.value = true

        viewModelScope.launch {
            val chatId = chatIdDeferred.await()
            AiChatRepository.sendMessage(
                text = text,
                chatId = chatId
            ).collect { result ->
                if (result is DataWrapper.Success) {
                    _isLoading.value = false
                    if (UserRepository.user.isPremium().not()) {
                        AppPreference.incrementDietologMessageCount()
                    }
                    val displayedText = result.data
                    val currentList = _messages.value.toMutableList()
                    val lastMsg = currentList.lastOrNull()
                    if (lastMsg != null && !lastMsg.isUser) {
                        currentList[currentList.lastIndex] = lastMsg.copy(text = displayedText)
                    } else {
                        currentList.add(ChatMessage(text = displayedText, isUser = false))
                    }
                    _messages.value = currentList
                    done()
                } else if (result is DataWrapper.Error) {
                    _isLoading.value = false
                    done()
                }
            }
        }
    }
}