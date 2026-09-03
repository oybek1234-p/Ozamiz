package uz.oybek.ozamiz.data.prediction

import uz.oybek.ozamiz.data.food.DietController
import uz.oybek.ozamiz.data.user.model.User
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt

data class WeightPredictionResult(
    val avgDailyDeficit: Int = 0,
    val dailyFatGrams: Int = 0,
    val weeklyLossKg: Double = 0.0,
    val monthlyLossKg: Double = 0.0,
    val weeklyLossStr: String = "0.0 kg",
    val monthlyLossStr: String = "0.0 kg",
    val isSurplus: Boolean = false,
    val remainingKg: Double = 0.0,
    val targetDateFormatted: String = "",
    val remainingTimeStr: String = "",
    val isTargetReached: Boolean = false,
    val validDaysCount: Int = 0,
    val isGainGoal: Boolean = false
)

object WeightPredictionRepository {

    suspend fun calculatePrediction(
        user: User,
        currentWeightOverride: Double? = null,
        targetWeightOverride: Double? = null
    ): WeightPredictionResult {
        if (!user.valid) return WeightPredictionResult()

        val tdee = DietController.calculateTDEE(user)
        val targetCalories = if (user.targetCalories > 0) {
            user.targetCalories
        } else {
            DietController.calculateTargetCalories(user)
        }

        val baselineDeficit = (tdee - targetCalories).toDouble()
        val effectiveDailyDeficit = baselineDeficit

        val avgDeficitInt = effectiveDailyDeficit.toInt()
        val fatGrams = (abs(effectiveDailyDeficit) / 7.7).roundToInt()
        val isSurplus = effectiveDailyDeficit < 0

        val currentW = currentWeightOverride ?: (if (user.weight > 0) user.weight else 70.0)
        val targetW = targetWeightOverride ?: user.targetWeight ?: 0.0

        val isGainGoal = if (!user.goal.isNullOrBlank()) {
            user.goal == "gain_weight"
        } else {
            targetW > 0.0 && targetW > currentW
        }

        // 1-Week & 1-Month Predictions (7700 kcal = 1 kg fat/mass)
        val (weeklyLossKg, monthlyLossKg, weeklyLossStr, monthlyLossStr, isWarningState) = if (isGainGoal) {
            // For Weight Gain: Surplus (negative effectiveDailyDeficit) means gaining weight
            val dailySurplus = -effectiveDailyDeficit
            val safeSurplus = if (dailySurplus > 0) dailySurplus else 500.0
            val rawWeeklyGain = (safeSurplus * 7.0) / 7700.0
            val clampedWeekly = minOf(2.0, maxOf(0.1, rawWeeklyGain))
            val clampedMonthly = clampedWeekly * 4.0
            val wStr = String.format(Locale.US, "+%.1f kg", clampedWeekly)
            val mStr = String.format(Locale.US, "+%.1f kg", clampedMonthly)
            FiveTuple(clampedWeekly, clampedMonthly, wStr, mStr, false)
        } else {
            // For Weight Loss: Deficit (positive effectiveDailyDeficit) means losing weight
            val safeDeficit = if (effectiveDailyDeficit > 0) effectiveDailyDeficit else 500.0
            val rawWeeklyKg = (safeDeficit * 7.0) / 7700.0
            val clampedWeekly = minOf(2.0, maxOf(0.1, rawWeeklyKg))
            val clampedMonthly = clampedWeekly * 4.0
            val wStr = String.format(Locale.US, "-%.1f kg", clampedWeekly)
            val mStr = String.format(Locale.US, "-%.1f kg", clampedMonthly)
            FiveTuple(clampedWeekly, clampedMonthly, wStr, mStr, false)
        }

        if (targetW <= 0.0) {
            return WeightPredictionResult(
                avgDailyDeficit = avgDeficitInt,
                dailyFatGrams = fatGrams,
                weeklyLossKg = weeklyLossKg,
                monthlyLossKg = monthlyLossKg,
                weeklyLossStr = weeklyLossStr,
                monthlyLossStr = monthlyLossStr,
                isSurplus = isSurplus,
                remainingKg = 0.0,
                targetDateFormatted = "",
                remainingTimeStr = "Maqsad ko'rsatilmagan",
                isTargetReached = false,
                validDaysCount = 7,
                isGainGoal = isGainGoal
            )
        }

        val remainingKg = if (isGainGoal) targetW - currentW else currentW - targetW

        if (remainingKg <= 0.05) {
            return WeightPredictionResult(
                avgDailyDeficit = avgDeficitInt,
                dailyFatGrams = fatGrams,
                weeklyLossKg = weeklyLossKg,
                monthlyLossKg = monthlyLossKg,
                weeklyLossStr = weeklyLossStr,
                monthlyLossStr = monthlyLossStr,
                isSurplus = isSurplus,
                remainingKg = 0.0,
                targetDateFormatted = "",
                remainingTimeStr = "Maqsad bajarildi",
                isTargetReached = true,
                validDaysCount = 7,
                isGainGoal = isGainGoal
            )
        }

        // Calculate estimated weeks and remaining time in Months & Days
        val safeWeeklyRate = if (weeklyLossKg > 0.01) weeklyLossKg else 0.5
        val weeksNeeded = remainingKg / safeWeeklyRate
        val totalDaysNeeded = (weeksNeeded * 7.0).roundToInt().coerceAtLeast(1)

        val months = totalDaysNeeded / 30
        val days = totalDaysNeeded % 30

        val remainingTimeStr = when {
            months > 0 && days > 0 -> "$months oy $days kun qoldi"
            months > 0 -> "$months oy qoldi"
            else -> "$days kun qoldi"
        }

        val targetCal = Calendar.getInstance()
        targetCal.add(Calendar.DAY_OF_YEAR, totalDaysNeeded)
        val sdf = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())
        val targetDateFormatted = sdf.format(targetCal.time)

        return WeightPredictionResult(
            avgDailyDeficit = avgDeficitInt,
            dailyFatGrams = fatGrams,
            weeklyLossKg = weeklyLossKg,
            monthlyLossKg = monthlyLossKg,
            weeklyLossStr = weeklyLossStr,
            monthlyLossStr = monthlyLossStr,
            isSurplus = isSurplus,
            remainingKg = remainingKg,
            targetDateFormatted = targetDateFormatted,
            remainingTimeStr = remainingTimeStr,
            isTargetReached = false,
            validDaysCount = 7,
            isGainGoal = isGainGoal
        )
    }

    private data class FiveTuple<A, B, C, D, E>(val first: A, val second: B, val third: C, val fourth: D, val fifth: E)
}
