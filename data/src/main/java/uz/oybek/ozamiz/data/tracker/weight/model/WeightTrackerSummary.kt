package uz.oybek.ozamiz.data.tracker.weight.model

import androidx.annotation.Keep

@Keep
data class BodyMeasurementMetric(
    val currentValue: Double? = null,
    val startValue: Double? = null,
    val changeValue: Double? = null,
    val unit: WeightUnit = WeightUnit.CM,
    val targetValue: Double? = null,
    val remainingValue: Double? = null
)

@Keep
data class WeightTrackerSummary(
    val totalWeightChangeKg: Double = 0.0,
    val totalMeasurementChangeCm: Double = 0.0,
    val progressPercent: Int = 0,
    val prediction: WeightPrediction = WeightPrediction(),
    val photos: WeightPhotoSummary = WeightPhotoSummary(),
    val idealWeightMin: Int = 0,
    val idealWeightMax: Int = 0,
    val userHeightCm: Int = 0,
    val weight: BodyMeasurementMetric = BodyMeasurementMetric(unit = WeightUnit.KG),
    val belly: BodyMeasurementMetric = BodyMeasurementMetric(unit = WeightUnit.CM),
    val hips: BodyMeasurementMetric = BodyMeasurementMetric(unit = WeightUnit.CM),
    val arm: BodyMeasurementMetric = BodyMeasurementMetric(unit = WeightUnit.CM)
)
