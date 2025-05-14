package uz.oybek.ozamiz.ui.log

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.database.ServerValue
import com.google.gson.Gson
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import uz.oybek.ozamiz.R
import uz.oybek.ozamiz.data.DataWrapper
import uz.oybek.ozamiz.data.LocalUser
import uz.oybek.ozamiz.data.info.ExerciseLog
import uz.oybek.ozamiz.data.logs.DailyLogRepository
import uz.oybek.ozamiz.ui.getinfo.ExerciseType
import kotlin.math.roundToInt

sealed class LogExerciseUiState {
    object Loading : LogExerciseUiState()
    data class Input(val availableTypes: List<ExerciseType>, val preselectedTypeId: String? = null) : LogExerciseUiState()
     data class Editing(val log: ExerciseLog, val availableTypes: List<ExerciseType>) : LogExerciseUiState()
    object Saving : LogExerciseUiState()
    object Success : LogExerciseUiState()
    data class Error(val message: String) : LogExerciseUiState()
}

class LogExerciseViewModel(application: Application) : AndroidViewModel(application) {

    private val dailyLogRepository = DailyLogRepository()
    private val allExerciseTypes = ExerciseType.getDefaultExerciseTypes()

    private val _uiState = MutableStateFlow<LogExerciseUiState>(LogExerciseUiState.Loading)
    val uiState: StateFlow<LogExerciseUiState> = _uiState.asStateFlow()

    private var selectedExercise: ExerciseType? = null
     private var logToEdit: ExerciseLog? = null

    fun loadInitialData(logJson: String?, preselectedId: String?) {
         if (logJson != null) {
             try {
                 logToEdit = Gson().fromJson(logJson, ExerciseLog::class.java)
                 _uiState.value = LogExerciseUiState.Editing(logToEdit!!, allExerciseTypes)
             } catch (e: Exception) {
                 _uiState.value = LogExerciseUiState.Error("Log ma'lumotini o'qishda xatolik")
             }
         } else {
             logToEdit = null
             _uiState.value = LogExerciseUiState.Input(allExerciseTypes, preselectedId)
         }
        selectedExercise = allExerciseTypes.find { it.id == preselectedId || it.id == logToEdit?.activityType }
    }

    fun setSelectedExerciseType(type: ExerciseType) {
        selectedExercise = type
    }

    fun saveExerciseLog(durationStr: String) {
        val duration = durationStr.toIntOrNull()
        val currentSelectedType = selectedExercise

        if (currentSelectedType == null) {
            _uiState.value = LogExerciseUiState.Error(getApplication<Application>().getString(R.string.error_select_exercise_type))
            return
        }
        if (duration == null || duration <= 0) {
            _uiState.value = LogExerciseUiState.Error(getApplication<Application>().getString(R.string.error_enter_valid_duration))
            return
        }

        _uiState.value = LogExerciseUiState.Saving

        viewModelScope.launch {
            val caloriesBurned = calculateCaloriesBurned(currentSelectedType.id, duration)

             val exerciseLog = (logToEdit ?: ExerciseLog()).copy(
                 activityType = currentSelectedType.id, // Save ID or Name based on preference
                 durationMin = duration,
                 caloriesBurned = caloriesBurned,
                 timestamp = logToEdit?.timestamp ?: ServerValue.TIMESTAMP // Keep original timestamp if editing
             )


            val result: DataWrapper<Unit>
             if (logToEdit != null) {
                 // Update existing log (Need update function in repository)
                 // result = dailyLogRepository.updateExerciseLog(exerciseLog) // Assuming update function exists
                 // For now, let's delete and add as a workaround if update isn't implemented
                 dailyLogRepository.deleteExerciseLog(logToEdit!!.logId) // Delete old one first
                 result = dailyLogRepository.addExerciseLog(exerciseLog.copy(logId = "")) // Add as new
             } else {
                 result = dailyLogRepository.addExerciseLog(exerciseLog)
             }


            when (result) {
                is DataWrapper.Success -> {
                    _uiState.value = LogExerciseUiState.Success
                }
                is DataWrapper.Error -> {
                    _uiState.value = LogExerciseUiState.Error(getApplication<Application>().getString(R.string.error_saving_log) + ": " + result.error)
                }
                else -> {
                    // Optional: Handle Loading state from repository if needed
                }
            }
        }
    }

    // Simple calorie estimation based on activity type and duration
    private fun calculateCaloriesBurned(activityTypeId: String, durationMin: Int): Int {
         // MET values are approximate and vary greatly. User weight should be factored in for better accuracy.
         val weightKg = LocalUser.user.weight.takeIf { it > 0 } ?: 70.0 // Default weight if unknown
         val metValue = when (activityTypeId) {
             "walking" -> 3.5
             "yoga" -> 2.5
             "dance" -> 5.0
             "strength" -> 4.0 // General strength training
             else -> 3.0 // Default MET
         }
         // Formula: Calories Burned = MET * Weight (kg) * Duration (hours)
         // Or simplified: MET * 3.5 * Weight (kg) / 200 * Duration (minutes) -> BMR based approx
         val calories = (metValue * 3.5 * weightKg / 200.0) * durationMin
         return calories.roundToInt()
    }
}