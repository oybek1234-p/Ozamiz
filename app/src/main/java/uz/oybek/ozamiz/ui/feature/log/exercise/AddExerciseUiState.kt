package uz.oybek.ozamiz.ui.feature.log.exercise

import androidx.annotation.Keep
import uz.oybek.ozamiz.data.logs.model.UserExercise
import uz.oybek.ozamiz.common.util.currentTime
import java.util.Date

@Keep
data class AddExerciseUiState(
    val selectedExercise: UserExercise? = null,
    val durationMinutes: Int = 0,
    val caloriesBurned: Int = 0,
    val fatBurnedGrams: Double = 0.0,
    val isSaving: Boolean = false,
    val isSaveButtonVisible: Boolean = false,
    val date: Date = currentTime()
)