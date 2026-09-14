package uz.oybek.ozamiz.ui.feature.exercises

import androidx.annotation.Keep
import uz.oybek.ozamiz.data.exercise.model.HomeExercise

@Keep
data class WeightLossExerciseUiModel(
    val exercise: HomeExercise,
    val isLocked: Boolean
)
