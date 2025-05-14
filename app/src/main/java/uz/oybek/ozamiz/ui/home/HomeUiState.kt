package uz.oybek.ozamiz.ui.home

import uz.oybek.ozamiz.data.User
import uz.oybek.ozamiz.data.info.DailyLogData
import uz.oybek.ozamiz.data.info.ExerciseLog
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
    val mealReminderTimes: Map<String, String> = emptyMap()
)