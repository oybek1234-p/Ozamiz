package uz.oybek.ozamiz.ui.feature.log.exercise

import android.app.Application
import android.os.Bundle
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import uz.oybek.ozamiz.data.level.PointsEvent
import uz.oybek.ozamiz.data.level.PointsRepository
import uz.oybek.ozamiz.data.logs.ExerciseLogRepository
import uz.oybek.ozamiz.data.logs.model.ExerciseLog
import uz.oybek.ozamiz.data.logs.model.UserExercise
import uz.oybek.ozamiz.data.utils.DateUtils
import uz.oybek.ozamiz.ui.feature.home.LogFragment
import java.util.Date

sealed class AddExerciseUiState {
    object Loading : AddExerciseUiState()
    data class Input(val selectedExercise: UserExercise, val caloriesBurned: Int) :
        AddExerciseUiState()

    object Saving : AddExerciseUiState()
    data class Success(val exerciseId: String, val caloriesBurned: Int) : AddExerciseUiState()
    data class Error(val message: String) : AddExerciseUiState()
}

class AddExerciseLogViewModel(application: Application) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow<AddExerciseUiState>(AddExerciseUiState.Loading)
    val uiState: StateFlow<AddExerciseUiState> = _uiState.asStateFlow()

    private var selectedDate: Date = Date()
    private var selectedExercise: UserExercise? = null

    fun loadArguments(args: Bundle?) {
        args ?: return
        val exerciseId = args.getString("exerciseId")
        selectedDate = LogFragment.currentDate

        try {
            selectedExercise = UserExercise.valueOf(exerciseId!!)
            _uiState.value = AddExerciseUiState.Input(selectedExercise!!, 0)
        } catch (_: Exception) {
        }
    }

    fun onDurationChanged(durationStr: String) {
        val exercise = selectedExercise ?: return
        val duration = durationStr.toIntOrNull() ?: 0
        val calories = exercise.getCalories(duration)
        _uiState.update { (it as AddExerciseUiState.Input).copy(caloriesBurned = calories) }
    }

    fun saveExerciseLog(durationStr: String) {
        val duration = durationStr.toIntOrNull()
        val exercise = selectedExercise

        if (exercise == null) {
            return
        }
        if (duration == null || duration <= 0) {
            return
        }
        _uiState.value = AddExerciseUiState.Saving
        viewModelScope.launch {
            val caloriesBurned = exercise.getCalories(duration)
            val exerciseLog = ExerciseLog(
                exerciseType = exercise.name,
                durationMin = duration,
                caloriesBurned = caloriesBurned
            )
            val success = ExerciseLogRepository.add(exerciseLog, selectedDate)
            if (success) {
                PointsRepository.onEvent(PointsEvent.Exercise(caloriesBurned))
                _uiState.value = AddExerciseUiState.Success(exercise.name, caloriesBurned)
            } else {
                _uiState.value = AddExerciseUiState.Error("Saqlashda xatolik yuz berdi")
            }
        }
    }
}