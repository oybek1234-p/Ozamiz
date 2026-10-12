package uz.oybek.ozamiz.ui.feature.ai

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import uz.oybek.ozamiz.data.ai.aiChat.AiChatRepository
import uz.oybek.ozamiz.data.ai.aiChat.ChatMessage
import uz.oybek.ozamiz.data.limit.api.LimitManagerRepo
import uz.oybek.ozamiz.data.limit.api.LimitUpdater
import uz.oybek.ozamiz.data.user.UserProfileRepository
import uz.oybek.ozamiz.data.user.local.user
import uz.oybek.ozamiz.common.result.BaseResult
import uz.oybek.ozamiz.ui.feature.premium.isPremium
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class AiChatViewModel @Inject constructor(
    private val limitManagerRepo: LimitManagerRepo,
    private val limitUpdater: LimitUpdater
) : ViewModel() {

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val messages: StateFlow<List<ChatMessage>> = _messages.asStateFlow()

    private var currentChatId: String = generateChatId()

    private val _remainingMessages = MutableStateFlow(getRemainingMessages())
    val remainingMessages: StateFlow<Int> = _remainingMessages.asStateFlow()

    private var sendJob: Job? = null
    private var resetJob: Job? = null

    fun getRemainingMessages(): Int = limitManagerRepo.getLimits().remainingChatLimit
    fun canSendMessage(): Boolean = limitManagerRepo.getLimits().canSendMessages

    fun refreshLimitState() {
        _remainingMessages.value = getRemainingMessages()
    }

    private fun generateChatId(): String {
        return "${user.uid}_${System.currentTimeMillis()}_${UUID.randomUUID().toString().take(8)}"
    }

    init {
        UserProfileRepository.setUnreadNotificationsZero()
        refreshLimitState()
        resetJob = viewModelScope.launch {
            AiChatRepository.clearHistory()
        }
    }

    fun startNewChat(clearHistory: Boolean = true) {
        sendJob?.cancel()
        resetJob?.cancel()
        _isLoading.value = false
        currentChatId = generateChatId()
        refreshLimitState()
        if (clearHistory) {
            _messages.value = emptyList()
            resetJob = viewModelScope.launch {
                AiChatRepository.clearHistory()
            }
        }
    }

    fun sendMessage(
        text: String,
        ignoreLimit: Boolean = false,
        done: () -> Unit = {}
    ) {
        val trimmed = text.trim()
        if (trimmed.isBlank() || _isLoading.value) return
        if (!ignoreLimit && !canSendMessage()) return

        resetJob?.cancel()
        sendJob?.cancel()

        viewModelScope.launch {
            limitUpdater.incrementChatMessageCount()
            _remainingMessages.value = getRemainingMessages()
        }

        val userMessage = ChatMessage(
            id = UUID.randomUUID().toString(),
            text = trimmed,
            isUser = true
        )
        val aiMessage = ChatMessage(
            id = UUID.randomUUID().toString(),
            text = "",
            isUser = false,
            isThinking = true
        )
        _messages.value = _messages.value + listOf(userMessage, aiMessage)
        android.util.Log.d(
            "AiChatDebug",
            "sendMessage: _messages.value SET, new size=${_messages.value.size}"
        )
        AiChatRepository.clearReplies()
        _isLoading.value = true

        val chatId = currentChatId

        sendJob = viewModelScope.launch {
            try {
                AiChatRepository.sendMessage(
                    text = trimmed,
                    chatId = chatId,
                    scope = this
                ).collect { result ->
                    if (result is BaseResult.Success) {
                        val displayedText = result.data
                        val currentList = _messages.value.toMutableList()
                        val lastAiIndex = currentList.indexOfLast { !it.isUser }
                        if (lastAiIndex != -1) {
                            currentList[lastAiIndex] = currentList[lastAiIndex].copy(
                                text = displayedText,
                                isThinking = false
                            )
                        } else {
                            currentList.add(
                                ChatMessage(
                                    id = UUID.randomUUID().toString(),
                                    text = displayedText,
                                    isUser = false,
                                    isThinking = false
                                )
                            )
                        }
                        _messages.value = currentList
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("AiChatViewModel", "Error while sending chat message", e)
                val currentList = _messages.value.toMutableList()
                val lastAiIndex = currentList.indexOfLast { !it.isUser }
                val errorMsg = "Kechirasiz, xatolik yuz berdi. Qaytadan urinib ko'ring."
                if (lastAiIndex != -1) {
                    currentList[lastAiIndex] = currentList[lastAiIndex].copy(
                        text = errorMsg,
                        isThinking = false
                    )
                } else {
                    currentList.add(
                        ChatMessage(
                            id = UUID.randomUUID().toString(),
                            text = errorMsg,
                            isUser = false,
                            isThinking = false
                        )
                    )
                }
                _messages.value = currentList
            } finally {
                _isLoading.value = false
                done()
            }
        }
    }

    fun onFeedbackClick(message: ChatMessage, isLiked: Boolean, userQuestion: String) {
        val feedbackType = if (isLiked) "like" else "dislike"
        val currentList = _messages.value.toMutableList()
        val index =
            currentList.indexOfFirst { it.id == message.id || (it.timestamp == message.timestamp && !it.isUser) }
        if (index != -1) {
            currentList[index] = currentList[index].copy(feedback = feedbackType)
            _messages.value = currentList
        }

        viewModelScope.launch {
            AiChatRepository.saveFeedback(
                userQuestion = userQuestion,
                aiResponse = message.text,
                isLiked = isLiked
            )
        }
    }
}