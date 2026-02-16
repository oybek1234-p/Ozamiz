package uz.oybek.ozamiz.ui.feature.log.exercise

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import uz.oybek.ozamiz.data.logs.model.UserExercise
import uz.oybek.ozamiz.data.user.local.LocalUser
import java.util.Date

sealed class SelectExerciseUiState {
    object Loading : SelectExerciseUiState()
    data class Success(val availableTypes: List<UserExercise>) : SelectExerciseUiState()
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
        _uiState.value = SelectExerciseUiState.Success(allExerciseTypes)
    }
}