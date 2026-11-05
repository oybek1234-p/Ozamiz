package uz.oybek.ozamiz.data.tracker.weight.model

import androidx.annotation.Keep

@Keep
data class WeightPrediction(
    val weeklyLossKg: Double = 0.0,
    val monthlyLossKg: Double = 0.0,
    val targetTimestamp: Long? = null,
    val daysToTarget: Int? = null,
    val isBasedOnActualFoodLogs: Boolean = false,
    val reasonText: String = "",
    val tdeeCalories: Int = 0,
    val targetCalories: Int = 0,
    val plannedDeficitCalories: Int = 0,
    val avgConsumedCalories: Int = 0,
    val weeklyTotalDeficitCalories: Int = 0
)
