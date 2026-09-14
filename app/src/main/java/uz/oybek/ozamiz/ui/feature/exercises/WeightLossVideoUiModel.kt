package uz.oybek.ozamiz.ui.feature.exercises

import androidx.annotation.Keep
import uz.oybek.ozamiz.data.exercise.model.HomeVideo

@Keep
data class WeightLossVideoUiModel(
    val video: HomeVideo,
    val isLocked: Boolean
)
