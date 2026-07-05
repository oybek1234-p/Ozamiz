package uz.oybek.ozamiz.ui.feature.log.food

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import uz.oybek.ozamiz.data.ai.analyzer.FoodAnalysis
import uz.oybek.ozamiz.data.ai.analyzer.FoodAnalyzer
import uz.oybek.ozamiz.data.config.AppPreference
import uz.oybek.ozamiz.data.food.SavedFoodRepository
import uz.oybek.ozamiz.data.food.model.MealTime
import uz.oybek.ozamiz.data.logs.FoodLogRepository
import uz.oybek.ozamiz.data.logs.model.FoodLog
import uz.oybek.ozamiz.data.streak.StreakRepository
import uz.oybek.ozamiz.data.user.local.user
import uz.oybek.ozamiz.data.user.model.daysFromRegister
import uz.oybek.ozamiz.ui.feature.premium.isPremium
import java.util.Date

data class LogFoodState(
    val isAnalyzing: Boolean = false,
    val description: String = "",
    val image: Uri? = null,
    val meal: String = MealTime.Breakfast.id,
    val result: FoodAnalysis? = null,
    val editing: FoodLog? = null,
    val uploadUrl: String? = null,
    val date: Date = Date(),
    val logs: List<FoodLog> = emptyList(),
    val savedFoodIds: Set<String> = emptySet()
)

sealed interface LogEvent {
    sealed class Error : LogEvent {
        object EmptyInput : Error()
        object AnalyzeError : Error()
    }

    data class Analyzed(
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
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    private var analyzeJob: Job? = null
    private var logsJob: Job? = null

    fun setDateAndMeal(date: Date, meal: String) {
        _state.update { it.copy(date = date, meal = meal) }
        observeLogsAndFavorites(date)
    }

    private fun observeLogsAndFavorites(date: Date) {
        logsJob?.cancel()
        logsJob = viewModelScope.launch {
            launch {
                combine(
                    FoodLogRepository.getAllLogs(date),
                    _state.map { it.meal }.distinctUntilChanged()
                ) { allLogs, mealId ->
                    allLogs.filter { it.mealType == mealId }
                }.collect { filteredLogs ->
                    _state.update { it.copy(logs = filteredLogs) }
                }
            }

            launch {
                SavedFoodRepository.getSavedFoods()
                    .map { it.map { food -> food.description.lowercase().trim() }.toSet() }
                    .collect { descriptions ->
                        _state.update { it.copy(savedFoodIds = descriptions) }
                    }
            }
        }
    }

    fun updateMeal(meal: String) {
        _state.update { it.copy(meal = meal) }
    }

    fun deleteFoodLog(foodLog: FoodLog, date: Date) {
        viewModelScope.launch {
            FoodLogRepository.delete(foodLog.logId, date)
        }
    }

    fun removeFavorite(foodLog: FoodLog) {
        viewModelScope.launch {
            SavedFoodRepository.removeSavedFood(foodLog.logId)
        }
    }

    fun relogFood(foodLog: FoodLog) {
        viewModelScope.launch {
            _loadingFoodIds.update { it + foodLog.logId }
            val log = foodLog.copy(
                logId = "",
                timestamp = System.currentTimeMillis(),
                mealType = state.value.meal
            )
            FoodLogRepository.add(null, log, state.value.date)
            StreakRepository.updateStreak()
            _loadingFoodIds.update { it - foodLog.logId }
        }
    }

    fun updateDescription(text: String?) {
        _state.update { it.copy(description = text.toString()) }
    }

    fun analyzePhoto(image: Uri) {
        _state.update { it.copy(image = image) }
        analyze()
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
            val result = FoodAnalyzer.analyze(
                description = state.value.description,
                meal = state.value.meal,
                image = state.value.image
            )
            if (user.premium.not()) {
                launch {
                    AppPreference.incrementFoodLogLimitCount()
                }
            }
            result?.photoUrl = state.value.uploadUrl
            result?.mealType = state.value.meal

            _state.update { it.copy(result = result, isAnalyzing = false) }

            if (result != null) {
                _event.send(LogEvent.Analyzed(result))
            } else {
                _event.send(LogEvent.Error.AnalyzeError)
            }
        }
    }

    fun analyzeAudio(audioUri: Uri) {
        analyzeJob?.cancel()
        analyzeJob = viewModelScope.launch {
            _state.update { it.copy(isAnalyzing = true) }

            val result = FoodAnalyzer.analyzeAudio(
                audioUri,
                state.value.meal
            )
            if (user.premium.not()) {
                launch {
                    AppPreference.incrementFoodLogLimitCount()
                }
            }
            _state.update { it.copy(result = result, isAnalyzing = false) }

            if (result != null) {
                _event.send(LogEvent.Analyzed(result))
            } else {
                _event.send(LogEvent.Error.AnalyzeError)
            }
        }
    }

    fun cancelAnalyze() {
        analyzeJob?.cancel()
        _state.update { it.copy(isAnalyzing = false) }
    }

    fun toggleFavorite(foodLog: FoodLog) {
        viewModelScope.launch {
            val isSaved = SavedFoodRepository.isFoodSaved(foodLog.description)
            if (isSaved) {
                SavedFoodRepository.removeSavedFood(foodLog.description)
            } else {
                val toSave = foodLog.copy(logId = "")
                SavedFoodRepository.saveFood(toSave)
            }
        }
    }

    fun canAddLog(): Boolean {
        if (user.isPremium()) return true
        if (user.daysFromRegister() <= 3 && AppPreference.getFoodLogLimitCount() <= 3)
            return true
        return false
    }
}
