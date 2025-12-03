package uz.oybek.ozamiz.ui.feature.home

import uz.oybek.ozamiz.data.logs.model.DailyLogs
import uz.oybek.ozamiz.data.streak.StreakInfo
import uz.oybek.ozamiz.data.user.model.User
import java.util.Date

data class HomeUiState(
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
    val user: User? = null,
    val dailyLogData: DailyLogs? = null,
    val consumedCalories: Int = 0,
    val consumedWaterMl: Int = 0,
    val burnedCalories: Int = 0,
    val targetCalories: Int = 0,
    val targetWaterMl: Int = 0,
    val targetCaloriesBurned: Int = 0,
    val streakInfo: StreakInfo? = null,
    val fat: Double = 0.0,
    val carbs: Double = 0.0,
    val protein: Double = 0.0,
    val selectedDate: Date = Date(),
    val datesForScroller: List<Date> = emptyList()
)