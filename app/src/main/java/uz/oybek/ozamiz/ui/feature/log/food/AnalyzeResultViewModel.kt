package uz.oybek.ozamiz.ui.feature.log.food

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import uz.oybek.ozamiz.applicationScope
import uz.oybek.ozamiz.data.ai.aiChat.AiChatRepository
import uz.oybek.ozamiz.data.ai.analyzer.FoodAnalysis
import uz.oybek.ozamiz.data.ai.analyzer.FoodAnalysisResult
import uz.oybek.ozamiz.data.ai.analyzer.FoodAnalyzer
import uz.oybek.ozamiz.data.community.CommunityPostRepository
import uz.oybek.ozamiz.data.config.AppPreference
import uz.oybek.ozamiz.data.firebase.uploader.ImageCompressor
import uz.oybek.ozamiz.data.food.SavedFoodRepository
import uz.oybek.ozamiz.data.level.PointsEvent
import uz.oybek.ozamiz.data.level.PointsRepository
import uz.oybek.ozamiz.data.logs.FoodLogRepository
import uz.oybek.ozamiz.data.logs.LogRepository
import uz.oybek.ozamiz.data.streak.StreakRepository
import uz.oybek.ozamiz.data.user.UserProfileRepository
import uz.oybek.ozamiz.data.user.local.user
import uz.oybek.ozamiz.ui.feature.home.LogFragment
import uz.oybek.ozamiz.ui.feature.premium.isPremium
import java.util.Date

data class AnalyzeResultState(
    val result: FoodAnalysis? = null,
    val isLoading: Boolean = false,
    val isAnalyzing: Boolean = false,
    val isError: Boolean = false,
    val currentStreak: Int = 0,
    val isSaved: Boolean = false,
    val isRecallMode: Boolean = false,
    val pendingImageUri: Uri? = null,
    val pendingDescription: String? = null
)

sealed interface AnalyzeResultEvent {
    object Saved : AnalyzeResultEvent
    data class Failure(val message: String) : AnalyzeResultEvent
    data class LimitExceeded(val exceededTypes: List<String>, val overages: Map<String, Int>) : AnalyzeResultEvent
    data class PromptShare(
        val name: String,
        val photoUrl: String,
        val calories: Int,
        val exceededTypes: List<String>? = null
    ) : AnalyzeResultEvent
}

class AnalyzeResultViewModel : ViewModel() {

    private val _state = MutableStateFlow(AnalyzeResultState())
    val state: StateFlow<AnalyzeResultState> = _state

    val resultLive: StateFlow<AnalyzeResultState?> = _state

    private val _event = Channel<AnalyzeResultEvent>()
    val event = _event.receiveAsFlow()

    private var loadedLogDate: java.util.Date? = null
    private var loadedLogId: String? = null
    private var loadedUserId: String? = null
    private var loadedCommunityPostId: String? = null

    fun startAnalyze(imageUri: Uri?, description: String, meal: String, date: Date) {
        loadedLogDate = date
        _state.update {
            it.copy(
                isAnalyzing = true,
                pendingImageUri = imageUri,
                pendingDescription = description
            )
        }
        viewModelScope.launch(Dispatchers.IO) {
            val uri = imageUri?.let {
                ImageCompressor.compressImageToUri(
                    it, 512, 512
                )
            }
            val result = FoodAnalyzer.analyze(
                description = description, meal = meal, image = uri
            )
            if (result != null) {
                result.photoUrl = uri?.toString()
                result.mealType = meal
                if (user.isPremium().not()) {
                    AppPreference.incrementFoodLogLimitCount()
                }
            }

            val streak = UserProfileRepository.user.streakInfo?.currentStreak ?: 0
            _state.update {
                it.copy(
                    result = result,
                    isAnalyzing = false,
                    isError = result == null,
                    currentStreak = streak
                )
            }
            if (result != null) checkIfSaved()
        }
    }

    fun analyzeAudio(audioUri: Uri, meal: String, date: Date) {
        loadedLogDate = date
        _state.update {
            it.copy(
                isAnalyzing = true,
                pendingDescription = "pending_description_voice"
            )
        }
        viewModelScope.launch(Dispatchers.IO) {
            val result = FoodAnalyzer.analyzeAudio(
                audioUri, meal
            )
            if (result != null) {
                result.mealType = meal
                if (user.isPremium().not()) {
                    AppPreference.incrementFoodLogLimitCount()
                }
            }
            val streak = UserProfileRepository.user.streakInfo?.currentStreak ?: 0
            _state.update {
                it.copy(
                    result = result,
                    isAnalyzing = false,
                    isError = result == null,
                    currentStreak = streak
                )
            }
            if (result != null) checkIfSaved()
        }
    }

    fun setResult(result: FoodAnalysis?) {
        val streak = UserProfileRepository.user.streakInfo?.currentStreak ?: 0
        _state.update {
            it.copy(
                result = result, currentStreak = streak
            )
        }
        checkIfSaved()
    }

    fun setRecallResult(result: FoodAnalysis) {
        _state.update {
            it.copy(
                result = result, isRecallMode = true
            )
        }
        checkIfSaved()
    }

    fun setLogDate(date: java.util.Date) {
        loadedLogDate = date
    }

    fun loadLog(
        logId: String, date: Date, userId: String? = null
    ) {
        loadedLogDate = date
        loadedLogId = logId
        loadedUserId = userId
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            val streak = user.streakInfo?.currentStreak ?: 0
            val log = FoodLogRepository.getFoodLog(logId, date, userId)
            if (log != null) {
                val analysis = FoodAnalysis(
                    id = log.logId.hashCode(),
                    photoUrl = log.photoUrl?.takeIf { it.isNotBlank() },
                    result = FoodAnalysisResult(
                        name = log.description,
                        calories = log.calories,
                        protein = log.protein,
                        fat = log.fat,
                        carbs = log.carbs,
                        ingredients = emptyList(),
                        healthScore = log.healthLevel,
                        timestamp = (log.timestamp as? Number)?.toLong()
                            ?: System.currentTimeMillis()
                    ),
                    mealType = log.mealType
                )
                _state.update {
                    it.copy(
                        result = analysis,
                        isLoading = false,
                        currentStreak = streak,
                        isRecallMode = true
                    )
                }
            } else {
                _state.update {
                    it.copy(
                        isError = true, isLoading = false, isRecallMode = false
                    )
                }
            }

            checkIfSaved()
        }
    }

    private fun checkIfSaved() {
        val currentResult = _state.value.result ?: return
        viewModelScope.launch {
            val isSaved = SavedFoodRepository.isFoodSavedByDescription(
                currentResult.result?.name ?: ""
            )
            _state.update { it.copy(isSaved = isSaved) }
        }
    }

    fun toggleFavorite() {
        if (_state.value.isLoading) return
        val currentResult = _state.value.result ?: return
        val foodLog = currentResult.toFoodLog()
        viewModelScope.launch {
            val isSaved = SavedFoodRepository.isFoodSavedByDescription(foodLog.description)
            if (isSaved) {
                SavedFoodRepository.removeSavedFoodByDescription(foodLog.description)
            } else {
                SavedFoodRepository.saveFood(foodLog)
            }
            checkIfSaved()
        }
    }

    fun reLog() {
        if (_state.value.isLoading) return
        val currentResult = _state.value.result ?: return
        val log = currentResult.toFoodLog().copy(
            logId = "", timestamp = System.currentTimeMillis()
        )
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            val targetDate = loadedLogDate ?: Date()
            FoodLogRepository.add(
                currentResult.photoUrl, log,
                targetDate,
                false
            )
            _state.update { it.copy(isLoading = false) }
            _event.send(AnalyzeResultEvent.Saved)
        }
    }

    fun saveFood() {
        if (_state.value.isLoading) return
        val result = _state.value.result ?: return
        _state.update { it.copy(isLoading = true) }
        val shouldShare = (result.result?.healthScore ?: 0) > 7 || result.photoUrl != null

        applicationScope.launch {
            val log = result.toFoodLog()
            val targetDate = loadedLogDate ?: LogFragment.currentDate

            FoodLogRepository.add(result.photoUrl, log, targetDate, shouldShare)
            launch {
                StreakRepository.updateStreak()
                AiChatRepository.onFoodLogAdded(log)
                PointsRepository.onEvent(PointsEvent.AddFood(result.result?.healthScore ?: 0))
            }
            _state.update { it.copy(isLoading = false) }

            _event.send(AnalyzeResultEvent.Saved)
        }
    }

    fun deleteLog(onComplete: (Boolean) -> Unit) {
        if (_state.value.isLoading) return
        val logId = loadedLogId ?: return
        val date = loadedLogDate ?: return
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            val success = FoodLogRepository.delete(logId, date, loadedUserId)
            _state.update { it.copy(isLoading = false) }
            onComplete(success)
        }
    }

    fun updateLog(newDescription: String) {
        if (_state.value.isLoading) return
        val currentResult = _state.value.result ?: return
        val logId = loadedLogId
        val date = loadedLogDate

        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            val prompt = "$newDescription. (Do not ask questions, provide best possible estimate)"
            val analysis = FoodAnalyzer.analyze(
                description = prompt, meal = currentResult.mealType, image = null
            )
            if (analysis != null) {
                analysis.photoUrl = currentResult.photoUrl
                analysis.mealType = currentResult.mealType
                FoodAnalyzer.updateCachedResult(
                    analysis.id, currentResult.photoUrl, currentResult.mealType
                )

                _state.update { it.copy(result = analysis, isLoading = false) }
                if (logId != null && date != null) {
                    val updatedLog = analysis.toFoodLog()
                    updatedLog.logId = logId
                    FoodLogRepository.update(logId, updatedLog, date, loadedUserId)
                }
            } else {
                _state.update { it.copy(isLoading = false, isError = true) }
            }
        }
    }

    fun onSaveClick() {
        if (_state.value.isLoading) return
        saveFood()
    }

    private fun syncLogUpdate() {
        val currentResult = _state.value.result ?: return
        val logId = loadedLogId ?: return
        val date = loadedLogDate ?: return
        viewModelScope.launch {
            val updatedLog = currentResult.toFoodLog()
            updatedLog.logId = logId
            FoodLogRepository.update(logId, updatedLog, date, loadedUserId)
        }
    }

    fun updateCalories(calories: Double) {
        _state.update { state ->
            val currentResult = state.result ?: return@update state
            val analysis = currentResult.result ?: return@update state
            state.copy(
                result = currentResult.copy(
                    result = analysis.copy(calories = calories.toInt())
                )
            )
        }
        syncLogUpdate()
    }

    fun updateProtein(protein: Double) {
        _state.update { state ->
            val currentResult = state.result ?: return@update state
            val analysis = currentResult.result ?: return@update state
            state.copy(
                result = currentResult.copy(
                    result = analysis.copy(protein = protein)
                )
            )
        }
        syncLogUpdate()
    }

    fun updateFat(fat: Double) {
        _state.update { state ->
            val currentResult = state.result ?: return@update state
            val analysis = currentResult.result ?: return@update state
            state.copy(
                result = currentResult.copy(
                    result = analysis.copy(fat = fat)
                )
            )
        }
        syncLogUpdate()
    }

    fun updateCarbs(carbs: Double) {
        _state.update { state ->
            val currentResult = state.result ?: return@update state
            val analysis = currentResult.result ?: return@update state
            state.copy(
                result = currentResult.copy(
                    result = analysis.copy(carbs = carbs)
                )
            )
        }
        syncLogUpdate()
    }
}