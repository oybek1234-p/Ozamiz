package uz.oybek.ozamiz.data.logs.model

import androidx.annotation.Keep

@Keep
data class DailySummary(
    val consumedCalories: Int = 0,
    val consumedWaterMl: Int = 0,
    val burnedCalories: Int = 0,
    val consumedProtein: Double = 0.0,
    val consumedCarbs: Double = 0.0,
    val consumedFat: Double = 0.0,
    val mealsCount: Int = 0,
    val timestamp: Long = 0L
) {
    constructor() : this(0, 0, 0, 0.0, 0.0, 0.0, 0, 0L)
}
