package uz.oybek.ozamiz.ui.feature.exercises

import uz.oybek.ozamiz.data.exercise.model.HomeExercise

data class WeightLossExerciseUiModel(
    val exercise: HomeExercise,
    val isLocked: Boolean
)
