package uz.oybek.ozamiz.ui.feature.ai

import androidx.annotation.StringRes
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import uz.oybek.ozamiz.R
import uz.oybek.ozamiz.data.ai.aiChat.ChatMessage
import uz.oybek.ozamiz.data.dietolog.api.DietologRepository
import uz.oybek.ozamiz.data.dietolog.model.AnalyzedFoodPayload
import uz.oybek.ozamiz.data.limit.api.LimitManagerRepo
import uz.oybek.ozamiz.data.limit.api.LimitUpdater
import uz.oybek.ozamiz.data.profile.api.ProfileManagerRepo
import uz.oybek.ozamiz.data.profile.api.ProfileRepo
import uz.oybek.ozamiz.ui.utils.getArgs
import javax.inject.Inject

data class AiChatUiState(
    val messages: List<ChatMessage> = emptyList(),
    val remainingMessages: Int = 0,
    val isLoading: Boolean = false,
    val isPremium: Boolean = false
)

sealed interface AiChatUiEffect {
    data class ShowToast(@param:StringRes val messageRes: Int) : AiChatUiEffect
    data object NavigateToPremium : AiChatUiEffect
    data class ShowLimitReachedDialog(val isPremium: Boolean) : AiChatUiEffect
}

@HiltViewModel
class AiChatViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val dietologRepo: DietologRepository,
    private val profileRepo: ProfileRepo,
    private val profileManagerRepo: ProfileManagerRepo,
    private val limitManagerRepo: LimitManagerRepo,
    private val limitUpdater: LimitUpdater
) : ViewModel() {

    private val args = savedStateHandle.getArgs<AiChatArgs>()

    private val _isLoading = MutableStateFlow(false)
    private val _remainingMessages = MutableStateFlow(0)
    private val _uiEffect = Channel<AiChatUiEffect>(Channel.BUFFERED)
    val effects = _uiEffect.receiveAsFlow()

    val uiState: StateFlow<AiChatUiState> = combine(
        dietologRepo.getAllMessagesFlow(),
        _remainingMessages,
        _isLoading,
        profileRepo.user
    ) { entities, remaining, loading, user ->
        val chatMessages = entities.map { it.toChatMessage() }
        val displayedMessages = if (loading) {
            chatMessages + ChatMessage(
                id = "thinking_placeholder",
                isUser = false,
                isThinking = true,
                timestamp = System.currentTimeMillis()
            )
        } else {
            chatMessages
        }
        AiChatUiState(
            messages = displayedMessages,
            remainingMessages = remaining,
            isLoading = loading,
            isPremium = user.isPremiumActive
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = AiChatUiState(
            remainingMessages = getRemainingMessages(),
            isPremium = profileRepo.user.value.isPremiumActive
        )
    )

    private var sendJob: Job? = null

    init {
        viewModelScope.launch {
            profileManagerRepo.updateUser { it.apply { unreadMessages = 0 } }
        }
        refreshLimitState()
    }

    fun getRemainingMessages(): Int = limitManagerRepo.getLimits().remainingChatLimit

    fun canSendMessage(): Boolean = limitManagerRepo.getLimits().canSendMessages

    fun refreshLimitState() {
        _remainingMessages.value = getRemainingMessages()
    }

    fun onSendClicked(text: String) {
        val trimmed = text.trim()
        if (trimmed.isBlank()) return
        if (_isLoading.value) return

        if (!canSendMessage()) {
            val isPremium = profileRepo.user.value.isPremiumActive
            if (isPremium) {
                _uiEffect.trySend(AiChatUiEffect.ShowToast(R.string.ai_chat_limit_premium_finished))
            } else {
                _uiEffect.trySend(AiChatUiEffect.NavigateToPremium)
            }
            return
        }

        sendJob?.cancel()
        sendJob = viewModelScope.launch {
            _isLoading.value = true
            try {
                when (dietologRepo.sendUserMessage(trimmed)) {
                    is uz.oybek.ozamiz.common.result.BaseResult.Success -> {
                        limitUpdater.incrementChatMessageCount()
                        refreshLimitState()
                    }
                    is uz.oybek.ozamiz.common.result.BaseResult.Failure -> {
                        _uiEffect.trySend(AiChatUiEffect.ShowToast(R.string.common_error_server_unavailable))
                    }
                }
            } catch (_: Exception) {
                _uiEffect.trySend(AiChatUiEffect.ShowToast(R.string.common_error_server_unavailable))
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun onFeedbackClicked(isLiked: Boolean) {
        // Feedback handling
    }

    fun confirmFoodIntake(message: ChatMessage, payload: AnalyzedFoodPayload) {
        viewModelScope.launch {
            dietologRepo.confirmAnalyzedFood(message.id, payload)
        }
    }

    fun startNewChat(clearOld: Boolean = true) {
        if (clearOld) {
            viewModelScope.launch {
                dietologRepo.clearHistory()
            }
        }
    }

    private fun uz.oybek.ozamiz.data.dietolog.local.entity.DietologMessageEntity.toChatMessage(): ChatMessage {
        return ChatMessage(
            id = id,
            text = text,
            isUser = senderType == uz.oybek.ozamiz.data.dietolog.model.DietologSenderType.USER,
            timestamp = timestamp,
            analyzedFood = analyzedFood,
            isAddedToTracker = isAddedToTracker,
            actions = actions
        )
    }
}
