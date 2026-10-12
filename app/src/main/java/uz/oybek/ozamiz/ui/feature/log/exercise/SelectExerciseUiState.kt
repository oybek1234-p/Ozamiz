package uz.oybek.ozamiz.ui.feature.log.exercise

import androidx.annotation.Keep
import uz.oybek.ozamiz.data.logs.model.UserExercise

@Keep
data class SelectExerciseUiState(
    val availableTypes: List<UserExercise> = emptyList(),
    val totalBurnedToday: Int = 0,
    val exerciseTarget: Int = 300,
    val isGoalReached: Boolean = false,
    val isLoading: Boolean = false
)