package uz.oybek.ozamiz.ui.feature.log.exercise

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import androidx.annotation.Keep
import uz.oybek.ozamiz.data.logs.LogRepository
import uz.oybek.ozamiz.data.logs.model.UserExercise
import uz.oybek.ozamiz.data.user.local.LocalUser
import uz.oybek.ozamiz.data.utils.DataWrapper
import java.util.Date

@Keep
sealed class SelectExerciseUiState {
    object Loading : SelectExerciseUiState()
    data class Success(
        val availableTypes: List<UserExercise>,
        val totalBurnedToday: Int = 0,
        val exerciseTarget: Int = 300
    ) : SelectExerciseUiState()

    data class Error(val message: String) : SelectExerciseUiState()
}

class SelectExerciseViewModel(application: Application) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow<SelectExerciseUiState>(SelectExerciseUiState.Loading)
    val uiState: StateFlow<SelectExerciseUiState> = _uiState.asStateFlow()

    var selectedDate: Date = Date()
        private set

    fun loadInitialData(timestamp: Long) {
        selectedDate = if (timestamp > 0) Date(timestamp) else Date()
        val allExerciseTypes = UserExercise.getExercisesList(LocalUser.user.gender)
        _uiState.value = SelectExerciseUiState.Success(availableTypes = allExerciseTypes)
        observeDailySummary()
    }

    private fun observeDailySummary() {
        viewModelScope.launch {
            LogRepository.observeDailySummary(selectedDate).collect { wrapper ->
                if (wrapper is DataWrapper.Success) {
                    val summary = wrapper.data
                    _uiState.update { state ->
                        if (state is SelectExerciseUiState.Success) {
                            state.copy(totalBurnedToday = summary.burnedCalories)
                        } else state
                    }
                }
            }
        }
    }
}