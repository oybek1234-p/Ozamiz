package uz.oybek.ozamiz.ui.feature.home

import androidx.annotation.Keep
import uz.oybek.ozamiz.data.logs.model.DailySummary
import uz.oybek.ozamiz.data.logs.model.FoodLog
import uz.oybek.ozamiz.data.streak.StreakInfo
import uz.oybek.ozamiz.data.logs.model.ExerciseLog
import uz.oybek.ozamiz.data.logs.model.WaterLog
import java.util.Date

@Keep
data class HomeUiState(
    val isInitialLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val selectedDate: Date = Date(),
    val datesForScroller: List<Date> = emptyList(),
    val dailySummaries: Map<String, DailySummary> = emptyMap(),
    val streakInfo: StreakInfo? = null,
    val caloriesRemaining: Int = 0,
    val caloriesConsumed: Int = 0,
    val caloriesTarget: Int = 0,
    val waterConsumed: Int = 0,
    val waterTarget: Int = 0,
    val exerciseBurnt: Int = 0,
    val exerciseTarget: Int = 0,
    val dailyFoodLogs: List<FoodLog> = emptyList(),
    val carbsConsumed: Int = 0,
    val carbsTarget: Int = 0,
    val proteinConsumed: Int = 0,
    val proteinTarget: Int = 0,
    val fatConsumed: Int = 0,
    val fatTarget: Int = 0,
    val exerciseLogs: List<ExerciseLog> = emptyList(),
    val waterLogs: List<WaterLog> = emptyList(),
    val showWeightLogCard: Boolean = false,
    val userPoints: Int = 0,
    val daysFromRegister: Int = 0,
    val paymentChecking: Boolean = false
)
