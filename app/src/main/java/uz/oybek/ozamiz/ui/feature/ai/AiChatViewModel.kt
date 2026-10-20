package uz.oybek.ozamiz.ui.feature.ai

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import uz.oybek.ozamiz.data.ai.aiChat.ChatMessage
import uz.oybek.ozamiz.data.dietolog.api.DietologRepository
import uz.oybek.ozamiz.data.dietolog.engine.DietologEvaluationEngine
import uz.oybek.ozamiz.data.dietolog.model.AnalyzedFoodPayload
import uz.oybek.ozamiz.data.dietolog.model.DietologSenderType
import uz.oybek.ozamiz.data.limit.api.LimitManagerRepo
import uz.oybek.ozamiz.data.limit.api.LimitUpdater
import uz.oybek.ozamiz.data.user.UserProfileRepository
import uz.oybek.ozamiz.common.result.BaseResult
import javax.inject.Inject

@HiltViewModel
class AiChatViewModel @Inject constructor(
    private val dietologRepository: DietologRepository,
    private val evaluationEngine: DietologEvaluationEngine,
    private val limitManagerRepo: LimitManagerRepo,
    private val limitUpdater: LimitUpdater
) : ViewModel() {

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    /**
     * SSOT: UI yagona Flow<List<ChatMessage>> oqimiga obuna bo'ladi.
     * Xabar yuborilayotganda loading (isThinking = true) animatsiyasi ro'yxat oxirida ko'rsatiladi.
     */
    val messages: StateFlow<List<ChatMessage>> = kotlinx.coroutines.flow.combine(
        dietologRepository.getAllMessagesFlow(),
        _isLoading
    ) { entities, loading ->
        val list = entities.map { entity ->
            ChatMessage(
                id = entity.id,
                text = entity.text,
                isUser = entity.senderType == DietologSenderType.USER,
                timestamp = entity.timestamp,
                analyzedFood = entity.analyzedFood,
                isAddedToTracker = entity.isAddedToTracker,
                actions = entity.actions
            )
        }.toMutableList()

        if (loading) {
            list.add(
                ChatMessage(
                    id = "thinking_placeholder",
                    text = "",
                    isUser = false,
                    isThinking = true,
                    timestamp = System.currentTimeMillis()
                )
            )
        }
        list
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    private val _remainingMessages = MutableStateFlow(getRemainingMessages())
    val remainingMessages: StateFlow<Int> = _remainingMessages.asStateFlow()

    private var sendJob: Job? = null

    fun getRemainingMessages(): Int = limitManagerRepo.getLimits().remainingChatLimit
    fun canSendMessage(): Boolean = limitManagerRepo.getLimits().canSendMessages

    fun refreshLimitState() {
        _remainingMessages.value = getRemainingMessages()
    }

    init {
        UserProfileRepository.setUnreadNotificationsZero()
        refreshLimitState()
        // Ilovaga yoki chatga kirganda mahalliy eslatmalarni baholash
        viewModelScope.launch {
            evaluationEngine.evaluateAndTriggerReminders()
        }
    }

    fun startNewChat(clearHistory: Boolean = true) {
        sendJob?.cancel()
        _isLoading.value = false
        refreshLimitState()
        if (clearHistory) {
            viewModelScope.launch {
                dietologRepository.clearHistory()
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

        sendJob?.cancel()

        viewModelScope.launch {
            limitUpdater.incrementChatMessageCount()
            _remainingMessages.value = getRemainingMessages()
        }

        _isLoading.value = true

        sendJob = viewModelScope.launch {
            try {
                dietologRepository.sendUserMessage(trimmed).collect { result ->
                    if (result is BaseResult.Failure) {
                        android.util.Log.e("AiChatViewModel", "sendUserMessage error: ${result.error}")
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("AiChatViewModel", "Error while sending chat message", e)
            } finally {
                _isLoading.value = false
                done()
            }
        }
    }

    /**
     * Foydalanuvchi «Kunlik hisobga qo'shilsinmi?» tugmasini bosganda:
     * DailyTracker moduli ishga tushadi: tahlil qilingan kaloriya va makrolar hisobga qo'shiladi
     * va tugma «Qo'shildi» holatiga o'tadi.
     */
    fun confirmFoodIntake(message: ChatMessage, payload: AnalyzedFoodPayload) {
        viewModelScope.launch {
            val result = dietologRepository.confirmAnalyzedFood(message.id, payload)
            if (result is BaseResult.Success) {
                // Ovqat qo'shilgach, kunlik limitlar o'zgargani uchun yangi eslatmalar baholanadi
                evaluationEngine.evaluateAndTriggerReminders(bypassCooldown = true)
            }
        }
    }

    fun onFeedbackClick(message: ChatMessage, isLiked: Boolean, userQuestion: String) {
        // Shaxsiy fikr-mulohaza qayta ishlash
    }
}