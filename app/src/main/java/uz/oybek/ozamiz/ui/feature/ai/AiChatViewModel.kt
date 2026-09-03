package uz.oybek.ozamiz.ui.feature.ai

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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
import uz.oybek.ozamiz.data.config.AppPreference
import uz.oybek.ozamiz.data.food.limit.FoodLimitManager
import uz.oybek.ozamiz.data.user.UserProfileRepository
import uz.oybek.ozamiz.data.user.local.user
import uz.oybek.ozamiz.data.utils.DataWrapper
import uz.oybek.ozamiz.ui.feature.premium.isPremium
import java.util.UUID

class AiChatViewModel : ViewModel() {

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val messages: StateFlow<List<ChatMessage>> = _messages.asStateFlow()

    private var currentChatId: String = generateChatId()

    private val _remainingMessages = MutableStateFlow(getRemainingMessages())
    val remainingMessages: StateFlow<Int> = _remainingMessages.asStateFlow()

    private var sendJob: Job? = null
    private var resetJob: Job? = null

    fun getMaxDailyMessages(): Int = FoodLimitManager.getMaxDailyChatMessages(user)
    fun getTodayMessageCount(): Int = FoodLimitManager.getTodayChatMessageCount()
    fun getRemainingMessages(): Int = FoodLimitManager.getRemainingChatMessages(user)
    fun canSendMessage(): Boolean = FoodLimitManager.canSendChatMessage(user)

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
            AppPreference.incrementAiChatLimitCount()
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
        android.util.Log.d("AiChatDebug", "sendMessage: _messages.value SET, new size=${_messages.value.size}")
        AiChatRepository.clearReplies()
        _isLoading.value = true

        val chatId = currentChatId

        sendJob = viewModelScope.launch {
            try {
                AiChatRepository.sendMessage(
                    text = trimmed,
                    chatId = chatId
                ).collect { result ->
                    if (result is DataWrapper.Success) {
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
}