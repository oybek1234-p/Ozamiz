package uz.oybek.ozamiz.ui.feature.natija

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import uz.oybek.ozamiz.data.firebase.FirebasePaths
import uz.oybek.ozamiz.data.firebase.getValueSafe
import uz.oybek.ozamiz.data.logs.LogRepository
import uz.oybek.ozamiz.data.logs.WeightLogRepository
import uz.oybek.ozamiz.data.logs.model.DailyLogs
import uz.oybek.ozamiz.data.logs.model.WeightLog
import androidx.annotation.Keep
import uz.oybek.ozamiz.common.result.BaseResult
import uz.oybek.ozamiz.common.util.dateApiFormat
import uz.oybek.ozamiz.common.util.orZero
import uz.oybek.ozamiz.data.user.local.LocalUser
import java.util.Date

@Keep
data class NatijaUiState(
    val isLoading: Boolean = false,
    val logs: List<WeightLog> = emptyList(),
    val startWeight: Double? = null,
    val currentWeight: Double? = null,
    val goalWeight: Double? = null,
    val weightDiff: Double = 0.0,
    val weeklyRate: Double = 0.0,
    val photoBefore: String? = null,
    val photoAfter: String? = null,
    val photoBeforeDate: Long? = null,
    val photoAfterDate: Long? = null,
    val photoBeforeWeight: Double? = null,
    val photoAfterWeight: Double? = null,
    val showTopBanner: Boolean = false,
    val showBottomBanner: Boolean = false,

    val estimatedGoalDate: Long? = null,
    val error: String? = null,
    val bmi: Double? = null,
    val bmiCategory: BmiCategory = BmiCategory.NONE,

    val avgDailyDeficit: Int = 0,
    val dailyFatGrams: Int = 0,
    val weeklyPredictedLoss: String = "-0.5 kg",
    val monthlyPredictedLoss: String = "-2.0 kg",
    val goalDurationText: String = "",
    val goalDateFormatted: String = "",
    val isSurplus: Boolean = false,
    val isGainGoal: Boolean = false,
    val isPredictionLoading: Boolean = true,
    val idealWeightText: String = "",
    val userHeightCm: Int = 0,

    // Water & Exercise metrics
    val todayWaterMl: Int = 0,
    val targetWaterMl: Int = 2500,
    val waterActiveDays: Int = 0,
    val todayExerciseCalories: Int = 0,
    val totalExerciseCalories: Int = 0,
    val fatBurnedGrams: Int = 0,
    val exerciseActiveDays: Int = 0
)

@Keep
enum class BmiCategory {
    UNDERWEIGHT, HEALTHY, OVERWEIGHT, OBESE, NONE
}

class NatijaViewModel : ViewModel() {

    private fun getInitialIsGainGoal(): Boolean {
        val user = LocalUser.user
        if (!user.goal.isNullOrBlank()) {
            return user.goal == uz.oybek.ozamiz.data.profile.model.UserGoal.GAIN_WEIGHT.key
        }
        val targetW = user.targetWeight.orZero()
        val currentW = if (user.weight > 0) user.weight else 70.0
        return targetW > 0.0 && targetW > currentW
    }

    private val _uiState = MutableStateFlow(
        NatijaUiState(
            isGainGoal = getInitialIsGainGoal(),
            weeklyPredictedLoss = if (getInitialIsGainGoal()) "+0.5 kg" else "-0.5 kg",
            monthlyPredictedLoss = if (getInitialIsGainGoal()) "+2.0 kg" else "-2.0 kg"
        )
    )
    val uiState = _uiState.asStateFlow()

    fun loadData() {
        val user = LocalUser.user
        if (!user.valid) return

        _uiState.update { it.copy(isLoading = true, error = null) }
        loadWaterAndExerciseData(user.uid)

        viewModelScope.launch {
            WeightLogRepository.getLogsFlow(user.uid).collect { result ->
                when (result) {
                    is BaseResult.Success -> {
                        val logs = result.data.sortedBy { it.timestampLong }
                        Log.e("Kot", logs.toString())
                        val validLogs = logs.filter { it.weightKg > 0 }

                        if (validLogs.isNotEmpty()) {
                            val currentUser = LocalUser.user
                            if (currentUser.startWeight == null || (currentUser.startWeight.orZero()) <= 0.0
                            ) {
                                val firstForStart = validLogs.first().weightKg
                                LocalUser.update { startWeight = firstForStart }
                            }
                        }

                        processLogs(logs)
                    }

                    is BaseResult.Failure -> {
                        _uiState.update { it.copy(isLoading = false, error = result.error) }
                    }

                    else -> {}
                }
            }
        }
    }

    private fun processLogs(logs: List<WeightLog>) {
        val user = LocalUser.user
        val validLogs = logs.filter { it.weightKg > 0 }

        val currentW =
            validLogs.lastOrNull()?.weightKg ?: (if (user.weight > 0) user.weight else null)
        val startW = (if ((user.startWeight.orZero()) > 0
        ) user.startWeight else null) ?: validLogs.firstOrNull()?.weightKg

        // Calculate diff (comparison with previous log)
        val diff = if (validLogs.size >= 2) {
            val last = validLogs.last().weightKg
            val prev = validLogs[validLogs.size - 2].weightKg
            last - prev
        } else if (validLogs.size == 1 && startW != null && startW > 0 && startW != validLogs.last().weightKg) {
            validLogs.last().weightKg - startW
        } else {
            0.0
        }

        val targetWeight = user.targetWeight

        // BMI Calculation
        val bmiData = if (currentW != null && user.height > 0) {
            calculateBmi(currentW, user.height)
        } else {
            0.0 to BmiCategory.NONE
        }

        if (validLogs.isEmpty()) {
            _uiState.update {
                it.copy(
                    isLoading = false,
                    logs = emptyList(),
                    startWeight = startW,
                    currentWeight = currentW,
                    weightDiff = diff,
                    goalWeight = targetWeight,
                    showTopBanner = true,
                    showBottomBanner = false,
                    photoBefore = null,
                    photoAfter = null,
                    bmi = bmiData.first,
                    bmiCategory = bmiData.second
                )
            }
            calculateDeficitAndGoalForecast(currentW, targetWeight)
            return
        }

        val lastLog = validLogs.last()

        val photoLogs = logs.filter { it.photoUrl.isNotEmpty() && it.timestampLong > 0 }
            .sortedByDescending { it.timestampLong }

        // Group by day to get the latest photo from each day
        val dailyPhotos = photoLogs.groupBy {
            val cal = java.util.Calendar.getInstance()
            cal.timeInMillis = it.timestampLong
            cal.set(java.util.Calendar.HOUR_OF_DAY, 0)
            cal.set(java.util.Calendar.MINUTE, 0)
            cal.set(java.util.Calendar.SECOND, 0)
            cal.set(java.util.Calendar.MILLISECOND, 0)
            cal.timeInMillis
        }.values.map { it.first() }

        val lastPhotoLog = dailyPhotos.getOrNull(0)
        val prevPhotoLog = if (dailyPhotos.size >= 2) {
            dailyPhotos[1]
        } else if (photoLogs.size >= 2) {
            photoLogs[1]
        } else {
            null
        }

        val lastPhoto = lastPhotoLog?.photoUrl
        val firstPhoto = prevPhotoLog?.photoUrl
        val lastPhotoDate = lastPhotoLog?.timestampLong
        val firstPhotoDate = prevPhotoLog?.timestampLong
        val lastPhotoWeight = lastPhotoLog?.weightKg
        val prevPhotoWeight = prevPhotoLog?.weightKg

        // Weekly Rate and Prediction
        var weeklyRate = 0.0
        if (validLogs.size >= 2) {
            val oneWeekAgo = System.currentTimeMillis() - (7 * 24 * 60 * 60 * 1000)
            val logWeekAgo = validLogs.lastOrNull { it.timestampLong < oneWeekAgo }

            if (logWeekAgo != null) {
                weeklyRate = (currentW ?: lastLog.weightKg) - logWeekAgo.weightKg
            } else {
                weeklyRate = (currentW ?: lastLog.weightKg) - validLogs.first().weightKg
            }
        }

        // Prediction Calculation
        // Safe rate: 0.5 - 1 kg per week. Let's use 0.75 as average/optimistic strict plan.
        var predictionDate: Long? = null
        if (currentW != null && targetWeight != null && targetWeight < currentW) {
            val remain = currentW - targetWeight
            val ratePerWeek = 0.75
            val weeksNeeded = remain / ratePerWeek
            val millisNeeded = (weeksNeeded * 7 * 24 * 60 * 60 * 1000).toLong()
            predictionDate = System.currentTimeMillis() + millisNeeded
        }

        // Banner Logic
        val sevenDaysMs = 7L * 24 * 60 * 60 * 1000
        val lastLogTime = lastLog.timestampLong
        val timeSinceLastLog = System.currentTimeMillis() - lastLogTime
        val showTop = timeSinceLastLog > sevenDaysMs
        val showBottom = !showTop
//
//        val idealPair = if (user.height > 0) DietEngine.getIdealWeight(user.height.toDouble()) else null
//        val idealText = if (idealPair != null) "${idealPair.first} - ${idealPair.second} kg" else ""
//
//        _uiState.update {
//            it.copy(
//                isLoading = false,
//                logs = logs,
//                startWeight = startW,
//                currentWeight = currentW,
//                goalWeight = targetWeight,
//                weightDiff = diff,
//                weeklyRate = weeklyRate,
//                photoBefore = firstPhoto,
//                photoAfter = lastPhoto,
//                photoBeforeDate = firstPhotoDate,
//                photoAfterDate = lastPhotoDate,
//                photoBeforeWeight = prevPhotoWeight,
//                photoAfterWeight = lastPhotoWeight,
//                showTopBanner = showTop,
//                showBottomBanner = showBottom,
//                estimatedGoalDate = predictionDate,
//                error = null,
//                bmi = bmiData.first,
//                bmiCategory = bmiData.second,
//                idealWeightText = idealText,
//                userHeightCm = user.height
//            )
//        }

        calculateDeficitAndGoalForecast(currentW, targetWeight)
    }

    private fun bmiCategoryFromBmi(bmi: Double): BmiCategory {
        return when {
            bmi <= 0 -> BmiCategory.NONE
            bmi < 18.5 -> BmiCategory.UNDERWEIGHT
            bmi < 25.0 -> BmiCategory.HEALTHY
            bmi < 30.0 -> BmiCategory.OVERWEIGHT
            else -> BmiCategory.OBESE
        }
    }

    private fun calculateDeficitAndGoalForecast(currentW: Double?, targetW: Double?) {
        viewModelScope.launch {
            val currentUser = LocalUser.user
            if (!currentUser.valid) {
                _uiState.update { it.copy(isPredictionLoading = false) }
                return@launch
            }

            val tdee = 0.0
            val targetCalories =
                if (currentUser.targetCalories > 0) currentUser.targetCalories else tdee.toInt()
            val effectiveDailyDeficit = tdee - targetCalories
            val avgDeficitInt = effectiveDailyDeficit.toInt()
            val fatGrams =
                (kotlin.math.abs(effectiveDailyDeficit) / 7.7).let { kotlin.math.round(it).toInt() }
            val isSurplus = effectiveDailyDeficit < 0

            val current = currentW ?: (if (currentUser.weight > 0) currentUser.weight else 70.0)
            val target = targetW ?: currentUser.targetWeight.orZero()
            val isGainGoal = if (!currentUser.goal.isNullOrBlank()) {
                currentUser.isGainWeight
            } else {
                target > 0.0 && target > current
            }

            val (weeklyLossKg, monthlyLossKg, weeklyLossStr, monthlyLossStr) = if (isGainGoal) {
                val dailySurplus = -effectiveDailyDeficit
                val safeSurplus = if (dailySurplus > 0) dailySurplus else 500.0
                val rawWeeklyGain = (safeSurplus * 7.0) / 7700.0
                val clampedWeekly = minOf(2.0, maxOf(0.1, rawWeeklyGain))
                val clampedMonthly = clampedWeekly * 4.0
                val wStr = String.format(java.util.Locale.US, "+%.1f kg", clampedWeekly)
                val mStr = String.format(java.util.Locale.US, "+%.1f kg", clampedMonthly)
                listOf(clampedWeekly, clampedMonthly, wStr, mStr)
            } else {
                val safeDeficit = if (effectiveDailyDeficit > 0) effectiveDailyDeficit else 500.0
                val rawWeeklyKg = (safeDeficit * 7.0) / 7700.0
                val clampedWeekly = minOf(2.0, maxOf(0.1, rawWeeklyKg))
                val clampedMonthly = clampedWeekly * 4.0
                val wStr = String.format(java.util.Locale.US, "-%.1f kg", clampedWeekly)
                val mStr = String.format(java.util.Locale.US, "-%.1f kg", clampedMonthly)
                listOf(clampedWeekly, clampedMonthly, wStr, mStr)
            }

            val weeklyKg = weeklyLossKg as Double
            val remainingKg = if (isGainGoal) target - current else current - target

            val (remainingTimeStr, targetDateFormatted) = if (target <= 0.0 || remainingKg <= 0.05) {
                val remStr = if (target <= 0.0) "Maqsad ko'rsatilmagan" else "Maqsad bajarildi"
                remStr to ""
            } else {
                val safeWeeklyRate = if (weeklyKg > 0.01) weeklyKg else 0.5
                val weeksNeeded = remainingKg / safeWeeklyRate
                val totalDaysNeeded =
                    (weeksNeeded * 7.0).let { kotlin.math.round(it).toInt() }.coerceAtLeast(1)
                val months = totalDaysNeeded / 30
                val days = totalDaysNeeded % 30
                val remStr = when {
                    months > 0 && days > 0 -> "$months oy $days kun qoldi"
                    months > 0 -> "$months oy qoldi"
                    else -> "$days kun qoldi"
                }
                val targetCal = java.util.Calendar.getInstance()
                targetCal.add(java.util.Calendar.DAY_OF_YEAR, totalDaysNeeded)
                val sdf = java.text.SimpleDateFormat("dd.MM.yyyy", java.util.Locale.getDefault())
                remStr to sdf.format(targetCal.time)
            }

            _uiState.update {
                it.copy(
                    avgDailyDeficit = avgDeficitInt,
                    dailyFatGrams = fatGrams,
                    weeklyPredictedLoss = weeklyLossStr as String,
                    monthlyPredictedLoss = monthlyLossStr as String,
                    goalDurationText = remainingTimeStr,
                    goalDateFormatted = targetDateFormatted,
                    isSurplus = isSurplus,
                    isGainGoal = isGainGoal,
                    isPredictionLoading = false
                )
            }
        }
    }

    private fun calculateBmi(weight: Double, heightCm: Int): Pair<Double, BmiCategory> {
        if (weight <= 0 || heightCm <= 0) return 0.0 to BmiCategory.NONE

        val heightM = heightCm / 100.0
        val bmi = weight / (heightM * heightM)

        val category = when {
            bmi < 18.5 -> BmiCategory.UNDERWEIGHT
            bmi < 25.0 -> BmiCategory.HEALTHY
            bmi < 30.0 -> BmiCategory.OVERWEIGHT
            else -> BmiCategory.OBESE
        }

        return bmi to category
    }

    private fun loadWaterAndExerciseData(userId: String) {
        viewModelScope.launch {
            try {
                val user = LocalUser.user
                val targetWater = if (user.targetWaterMl > 0) user.targetWaterMl else 2500
                val formattedToday = Date().dateApiFormat()

                val logsSnapshot = try {
                    FirebasePaths.Database.logs(userId).get().await()
                } catch (e: Exception) {
                    null
                }

                var todayWater = 0
                var todayExerciseCals = 0
                var totalExerciseCals = 0
                var waterDaysCount = 0
                var exerciseDaysCount = 0

                if (logsSnapshot != null && logsSnapshot.exists()) {
                    for (dateSnap in logsSnapshot.children) {
                        val dateKey = dateSnap.key ?: ""
                        val dailyLogs = dateSnap.getValueSafe(DailyLogs::class.java) ?: continue
                        val summary = LogRepository.calculateSummary(dailyLogs)

                        val waterInDay =
                            if (summary.consumedWaterMl > 0) summary.consumedWaterMl else (dailyLogs.waterLogs?.values?.sumOf { it.amountMl }
                                .orZero())
                        val exerciseInDay =
                            if (summary.burnedCalories > 0) summary.burnedCalories else (dailyLogs.exerciseLogs?.values?.sumOf { it.caloriesBurned }
                                .orZero())

                        if (waterInDay > 0) {
                            waterDaysCount++
                        }
                        if (exerciseInDay > 0) {
                            exerciseDaysCount++
                            totalExerciseCals += exerciseInDay
                        }

                        if (dateKey == formattedToday) {
                            todayWater = waterInDay
                            todayExerciseCals = exerciseInDay
                        }
                    }
                }

                val fatGrams = (totalExerciseCals / 7.7).toInt().coerceAtLeast(0)

                _uiState.update {
                    it.copy(
                        todayWaterMl = todayWater,
                        targetWaterMl = targetWater,
                        waterActiveDays = waterDaysCount,
                        todayExerciseCalories = todayExerciseCals,
                        totalExerciseCalories = totalExerciseCals,
                        fatBurnedGrams = fatGrams,
                        exerciseActiveDays = exerciseDaysCount
                    )
                }
            } catch (e: Exception) {
                Log.e("NatijaViewModel", "Error loading water/exercise data", e)
            }
        }
    }
}

