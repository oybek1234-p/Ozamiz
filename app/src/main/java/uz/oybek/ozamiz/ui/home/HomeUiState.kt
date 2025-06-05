package uz.oybek.ozamiz.ui.home

import uz.oybek.ozamiz.data.User
import uz.oybek.ozamiz.data.info.DailyLogData
import uz.oybek.ozamiz.data.info.ExerciseLog
import uz.oybek.ozamiz.data.model.StreakInfo
import uz.oybek.ozamiz.ui.getinfo.ReminderItem

data class HomeUiState(
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
    val user: User? = null,
    val dailyLogData: DailyLogData? = null,
    val consumedCalories: Int = 0,
    val consumedWaterMl: Int = 0,
    val burnedCalories: Int = 0,
    val targetCalories: Int = 0,
    val targetWaterMl: Int = 0,
    val targetCaloriesBurned: Int = 0,
    val mealCalorieDistribution: Map<String, Int> = emptyMap(),
    val mealConsumedCalories: Map<String, Int> = emptyMap(),
    val todayExerciseLogs: List<ExerciseLog> = emptyList(),
    val enabledExerciseReminders: List<ReminderItem> = emptyList(),
    val mealReminderTimes: Map<String, String> = emptyMap(),
    val streakInfo: StreakInfo? = null,

    // Yangi maydonlar vazn maqsadi uchun
    val showWeightGoalSection: Boolean = false,
    val currentWeightDisplay: Double = 0.0,
    val initialWeightForGoalDisplay: Double = 0.0, // Progress bar uchun boshlang'ich (current weight in this case)
    val targetWeightDisplay: Double = 0.0,
    val weightLostDisplay: Double = 0.0,         // "Progress" uchun (0 for initial state as per image)
    val weightRemainingDisplay: Double = 0.0,    // "Qoldi" uchun
    val goalProgressPercent: Int = 0,           // Progress bar uchun foiz (0 for initial state as per image)
    val goalStartDateDisplay: Long = 0L,        // Maqsad boshlangan sana (registerDate)
    val estimatedTimelineStrings: List<String> = emptyList(), // Taxminiy vaqtlar uchun
    val activeDays: Int = 0 // StreakInfo.activeDays
)