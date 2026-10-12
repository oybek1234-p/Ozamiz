package uz.oybek.ozamiz.ui.feature.log.food

import androidx.annotation.Keep
import androidx.annotation.StringRes
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import uz.oybek.ozamiz.ui.modelUtils.MealTime
import uz.oybek.ozamiz.data.limit.api.LimitManagerRepo
import uz.oybek.ozamiz.data.profile.api.ProfileRepo
import uz.oybek.ozamiz.data.tracker.api.FoodTrackerRepo
import uz.oybek.ozamiz.data.tracker.model.FoodModel
import uz.oybek.ozamiz.data.tracker.session.SelectedDateManager
import uz.oybek.ozamiz.common.util.currentTime
import uz.oybek.ozamiz.ui.utils.getArgs
import java.util.Date
import javax.inject.Inject

@Keep
data class LogFoodUiState(
    val date: Date = currentTime(),
    val mealType: String = MealTime.Breakfast.id,
    val logs: List<FoodModel> = emptyList(),
    val isPremium: Boolean = false,
    val isLoaded: Boolean = false
)

sealed interface LogFoodUiEffect {
    data class ShowToastRes(@param:StringRes val messageResId: Int) : LogFoodUiEffect
    data class NavigateToCamera(val mealType: String) : LogFoodUiEffect
    data class ShowLimitSheet(
        val remainingLimit: Int,
        val targetLimit: Int,
        val mealType: String
    ) : LogFoodUiEffect
}

@HiltViewModel
class LogFoodViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val selectedDateManager: SelectedDateManager,
    private val foodTrackerRepo: FoodTrackerRepo,
    private val limitManagerRepo: LimitManagerRepo,
    private val profileRepo: ProfileRepo
) : ViewModel() {

    private val args = savedStateHandle.getArgs<LogFoodArgs>()

    private val _uiState = MutableStateFlow(createInitialState())
    val uiState: StateFlow<LogFoodUiState> = _uiState.asStateFlow()

    private val _effects = Channel<LogFoodUiEffect>(Channel.BUFFERED)
    val effects = _effects.receiveAsFlow()

    private var logsJob: Job? = null

    init {
        val date = selectedDateManager.getSelectedDate()
        val meal = _uiState.value.mealType
        observeLogs(date, meal)

        viewModelScope.launch {
            profileRepo.user.collect { user ->
                _uiState.update {
                    it.copy(isPremium = user.isPremiumActive)
                }
            }
        }
    }

    private fun createInitialState(): LogFoodUiState {
        val selectedDate = selectedDateManager.getSelectedDate()
        val rawMealType = args?.mealType
        val validMeal = rawMealType ?: MealTime.getMealByTime().id
        return LogFoodUiState(
            date = selectedDate,
            mealType = validMeal
        )
    }

    private fun observeLogs(date: Date, meal: String) {
        logsJob?.cancel()
        logsJob = viewModelScope.launch {
            foodTrackerRepo.observeFoodLogs(date, meal)
                .collect { logs ->
                    _uiState.update { current ->
                        current.copy(
                            logs = logs.sortedByDescending { it.timestamp },
                            isLoaded = true
                        )
                    }
                }
        }
    }

    fun deleteFoodLog(foodLog: FoodModel) {
        viewModelScope.launch {
            foodTrackerRepo.deleteFoodLog(foodLog)
        }
    }

    fun onAddFoodClicked() {
        val limits = limitManagerRepo.getLimits()
        val state = _uiState.value

        viewModelScope.launch {
            if (limits.canLogFood) {
                _effects.send(
                    LogFoodUiEffect.NavigateToCamera(
                        mealType = state.mealType
                    )
                )
            } else {
                _effects.send(
                    LogFoodUiEffect.ShowLimitSheet(
                        remainingLimit = limits.remainingFoodLimit,
                        targetLimit = limits.foodLimit,
                        mealType = state.mealType
                    )
                )
            }
        }
    }
}
