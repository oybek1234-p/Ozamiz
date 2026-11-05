package uz.oybek.ozamiz.ui.feature.natija

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import uz.oybek.ozamiz.R

enum class WeightMetricInfo(
    @get:StringRes val titleRes: Int,
    @get:DrawableRes val iconRes: Int,
    @get:StringRes val insightRes: Int,
    @get:StringRes val instructionRes: Int,
) {
    WEIGHT(
        titleRes = R.string.metric_title_weight,
        iconRes = R.drawable.weight_ic,
        insightRes = R.string.metric_insight_weight,
        instructionRes = R.string.metric_instruction_weight,
    ),
    BELLY(
        titleRes = R.string.metric_title_belly,
        iconRes = R.drawable.belly_ic,
        insightRes = R.string.metric_insight_belly,
        instructionRes = R.string.metric_instruction_belly,
    ),
    HIPS(
        titleRes = R.string.metric_title_hips,
        iconRes = R.drawable.hips_ic,
        insightRes = R.string.metric_insight_hips,
        instructionRes = R.string.metric_instruction_hips,
    ),
    ARM(
        titleRes = R.string.metric_title_arm,
        iconRes = R.drawable.arm_ic,
        insightRes = R.string.metric_insight_arm,
        instructionRes = R.string.metric_instruction_arm,
    ),
}
