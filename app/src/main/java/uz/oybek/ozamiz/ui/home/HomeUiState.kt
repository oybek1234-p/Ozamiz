package uz.oybek.ozamiz.ui.home

import uz.oybek.ozamiz.data.User
import uz.oybek.ozamiz.data.info.DailyLogData
import uz.oybek.ozamiz.data.model.StreakInfo
import java.util.Date

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
    val streakInfo: StreakInfo? = null,
    val fat: Double = 0.0,
    val carbs: Double = 0.0,
    val protein: Double = 0.0,
    // Yangi maydonlar
    val selectedDate: Date = Date(),
    val datesForScroller: List<Date> = emptyList()
)