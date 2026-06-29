package uz.oybek.ozamiz.ui.feature.log.food

import uz.oybek.ozamiz.data.config.LocalPreferencesRepository

import android.net.Uri
import androidx.core.net.toUri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.async
import uz.oybek.ozamiz.data.firebase.uploader.ImageCompressor
import uz.oybek.ozamiz.data.firebase.uploader.ImagePaths
import uz.oybek.ozamiz.data.firebase.uploader.ImageUploader
import uz.oybek.ozamiz.data.food.analyzer.FoodAnalyzer
import uz.oybek.ozamiz.data.food.analyzer.model.FoodAnalysis
import uz.oybek.ozamiz.data.food.model.MealTime
import uz.oybek.ozamiz.data.logs.model.FoodLog
import uz.oybek.ozamiz.data.food.SavedFoodRepository
import uz.oybek.ozamiz.data.logs.FoodLogRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.first
import uz.oybek.ozamiz.data.logs.DailyLogRepository
import uz.oybek.ozamiz.data.logs.model.DailyLogs
import uz.oybek.ozamiz.data.user.UserRepository
import uz.oybek.ozamiz.data.user.model.User
import uz.oybek.ozamiz.data.utils.DataWrapper
import uz.oybek.ozamiz.ui.feature.premium.isPremiumOrTrial
import java.util.Date

data class LogFoodState(
    val isAnalyzing: Boolean = false,
    val description: String = "",
    val image: Uri? = null,
    val meal: String = MealTime.Breakfast.id,
    val result: FoodAnalysis? = null,
    val editing: FoodLog? = null,
    val uploadUrl: String? = null,
    val date: Date = Date()
)

sealed interface LogEvent {
    sealed class Error : LogEvent {
        object EmptyInput : Error()
        object AnalyzeError : Error()
    }

    data class Analyzed(
        val data: FoodAnalysis
    ) : LogEvent

    data class ClarificationNeeded(
        val data: FoodAnalysis
    ) : LogEvent

    data class Message(val text: String) : LogEvent
}

class LogFoodViewModel() : ViewModel() {

    private val _state = MutableStateFlow(LogFoodState())
    val state = _state.asStateFlow()

    private val _event = Channel<LogEvent>()
    val event = _event.receiveAsFlow()

    private val _loadingFoodIds = MutableStateFlow<Set<String>>(emptySet())
    val loadingFoodIds = _loadingFoodIds.asStateFlow()

    val savedFoods = SavedFoodRepository.getSavedFoods()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val savedFoodIds = savedFoods.map { it.map { food -> food.logId }.toSet() }
        .stateIn(viewModelScope, SharingStarted.Lazily, emptySet())

    private var analyzeJob: Job? = null

    fun updateMeal(meal: String) {
        _state.update { it.copy(meal = meal) }
    }

    fun logFood(foodLog: FoodLog, date: Date) {
        viewModelScope.launch {
            FoodLogRepository.add(image = null, foodLog = foodLog, date = date)
        }
    }

    fun reLogFood(foodLog: FoodLog) {
        viewModelScope.launch {
            val duplicate = foodLog.copy(logId = "", timestamp = System.currentTimeMillis())
            FoodLogRepository.add(null, duplicate, state.value.date)
            _event.send(LogEvent.Message("Qayta qo'shildi!"))
        }
    }

    fun toggleFavorite(foodLog: FoodLog) {
        viewModelScope.launch {
            val isSaved = SavedFoodRepository.isFoodSaved(foodLog.logId)
            if (isSaved) {
                SavedFoodRepository.removeSavedFood(foodLog.logId)
                _event.send(LogEvent.Message("Sevimlilardan olib tashlandi"))
            } else {
                SavedFoodRepository.saveFood(foodLog)
                _event.send(LogEvent.Message("Sevimlilarga qo'shildi!"))
            }
        }
    }

    fun removeFavorite(foodLog: FoodLog) {
        viewModelScope.launch {
            SavedFoodRepository.removeSavedFood(foodLog.logId)
            _event.send(LogEvent.Message("Sevimlilardan olib tashlandi"))
        }
    }

    fun logSavedFood(foodLog: FoodLog) {
        viewModelScope.launch {
            _loadingFoodIds.update { it + foodLog.logId }
            val log = foodLog.copy(logId = "", timestamp = System.currentTimeMillis(), mealType = state.value.meal)
            FoodLogRepository.add(null, log, state.value.date)
            _loadingFoodIds.update { it - foodLog.logId }
            _event.send(LogEvent.Message("Qo'shildi!"))
        }
    }

    fun updateDescription(text: String?) {
        _state.update { it.copy(description = text.toString()) }
    }

    fun analyzePhoto(image: Uri) {
        _state.update { it.copy(image = image) }
        analyze()
    }

    private suspend fun getDailyStats(date: Date): Triple<Int, Int, Int> {
        val dailyLogsWrapper = DailyLogRepository.observeDailyLogs(date).first()
        val dailyLogs = (dailyLogsWrapper as? DataWrapper.Success)?.data ?: DailyLogs()
        val consumed = dailyLogs.foodLogs?.values?.sumOf { it.calories } ?: 0
        val water = dailyLogs.waterLogs?.values?.sumOf { it.amountMl } ?: 0
        val burned = dailyLogs.exerciseLogs?.values?.sumOf { it.caloriesBurned } ?: 0
        return Triple(consumed, water, burned)
    }

    fun submitAnswer(answer: String) {
        val currentAnalysis = state.value.result ?: return
        viewModelScope.launch {
            _state.update { it.copy(isAnalyzing = true) }
            val history = FoodAnalyzer.getHistory(currentAnalysis.id)
            if (history.isEmpty()) {
                _state.update { it.copy(isAnalyzing = false) }
                _event.send(LogEvent.Error.AnalyzeError)
                return@launch
            }

            val answerContent = com.google.firebase.ai.type.content(role = "user") { text(answer) }
            val result = FoodAnalyzer.analyze(history + answerContent)
            
            _state.update { it.copy(result = result, isAnalyzing = false) }

            if (result != null) {
                result.photoUrl = currentAnalysis.photoUrl
                result.mealType = currentAnalysis.mealType
                FoodAnalyzer.updateCachedResult(result.id, currentAnalysis.photoUrl, currentAnalysis.mealType)

                if (result.result?.isAmbiguous == true) _event.send(LogEvent.ClarificationNeeded(result))
                else _event.send(LogEvent.Analyzed(result))
            } else {
                _event.send(LogEvent.Error.AnalyzeError)
            }
        }
    }

    fun analyze() {
        analyzeJob?.cancel()
        analyzeJob = viewModelScope.launch {
            val currentState = state.value
            if (currentState.description.isBlank() && currentState.image == null) {
                _event.send(LogEvent.Error.EmptyInput)
                return@launch
            }

            _state.update { it.copy(isAnalyzing = true) }
            val compressed = currentState.image?.let { ImageCompressor.compressImageFile(it) }
            val (consumed, water, burned) = getDailyStats(currentState.date)
            val target = UserRepository.user.targetCalories

            val initialContent = FoodAnalyzer.createInitialContent(
                compressed?.toUri(), currentState.description, currentState.meal,
                consumed, water, burned, target
            )

            val result = FoodAnalyzer.analyze(listOf(initialContent))
            _state.update { it.copy(result = result, isAnalyzing = false) }

            if (result != null) {
                result.photoUrl = currentState.image?.toString()
                result.mealType = currentState.meal
                FoodAnalyzer.updateCachedResult(result.id, currentState.image?.toString(), currentState.meal)
                
                if (result.result?.isAmbiguous == true) _event.send(LogEvent.ClarificationNeeded(result))
                else _event.send(LogEvent.Analyzed(result))
            } else {
                _event.send(LogEvent.Error.AnalyzeError)
            }
        }
    }

    fun analyzeAudio(audioUri: Uri) {
        analyzeJob?.cancel()
        analyzeJob = viewModelScope.launch {
            _state.update { it.copy(isAnalyzing = true) }
            val (consumed, water, burned) = getDailyStats(state.value.date)
            val target = UserRepository.user.targetCalories

            val result = FoodAnalyzer.analyzeAudio(audioUri, state.value.meal, consumed, water, burned, target)
            _state.update { it.copy(result = result, isAnalyzing = false) }
            
            if (result != null) {
                result.mealType = state.value.meal
                FoodAnalyzer.updateCachedResult(result.id, null, state.value.meal)
                
                if (result.result?.isAmbiguous == true) _event.send(LogEvent.ClarificationNeeded(result))
                else _event.send(LogEvent.Analyzed(result))
            } else {
                _event.send(LogEvent.Error.AnalyzeError)
            }
        }
    }

    fun cancelAnalyze() {
        analyzeJob?.cancel()
        _state.update { it.copy(isAnalyzing = false) }
    }

    val isUserPremium: Boolean
        get() = UserRepository.user.premium

    val isUserPremiumOrTrial: Boolean
        get() = UserRepository.user.premium || UserRepository.user.isPremiumOrTrial()

    fun isFoodTourSeen(): Boolean {
        return LocalPreferencesRepository.isFoodTourSeen()
    }

    fun setFoodTourSeen(seen: Boolean) {
        LocalPreferencesRepository.setFoodTourSeen(seen)
    }

    fun getLatestLogs(date: Date, limit: Int = 2) = FoodLogRepository.getLatestLogs(date, limit)
}
