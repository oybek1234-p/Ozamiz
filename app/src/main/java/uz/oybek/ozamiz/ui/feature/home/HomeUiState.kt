package uz.oybek.ozamiz.ui.feature.home

import androidx.annotation.Keep
import uz.oybek.ozamiz.data.profile.model.UserGoal
import uz.oybek.ozamiz.data.tracker.model.DailyCalorieMacroSummary
import uz.oybek.ozamiz.data.tracker.model.DailyExerciseSummary
import uz.oybek.ozamiz.data.tracker.model.DailyMealsSummary
import uz.oybek.ozamiz.data.tracker.model.DailySummary
import uz.oybek.ozamiz.data.tracker.model.DailyWaterSummary
import uz.oybek.ozamiz.common.util.currentTime
import java.util.Date

@Keep
data class HomeUiState(
    val calendar: CalendarUiState = CalendarUiState(),
    val calorieMacro: DailyCalorieMacroSummary = DailyCalorieMacroSummary(),
    val meals: DailyMealsSummary = DailyMealsSummary(),
    val water: DailyWaterSummary = DailyWaterSummary(),
    val exercise: DailyExerciseSummary = DailyExerciseSummary(),
    val profile: HomeProfileUiState = HomeProfileUiState(),
    val isLoading: Boolean = false
)

@Keep
data class DateScrollerItem(
    val date: Date,
    val dayOfMonth: String,
    val isToday: Boolean = false,
    val isSelected: Boolean = false,
    val progressPercent: Int = 0,
    val isExceeded: Boolean = false,
    val isCompleted: Boolean = false
)

@Keep
data class CalendarUiState(
    val selectedDate: Date = currentTime(),
    val items: List<DateScrollerItem> = emptyList(),
    val isRefreshing: Boolean = false
)

@Keep
data class HomeProfileUiState(
    val userGoal: UserGoal = UserGoal.LOSE_WEIGHT,
    val showWeightLogCard: Boolean = false,
    val paymentChecking: Boolean = false,
    val userPoints: Int = 0,
    val daysFromRegister: Int = 0,
    val isRated: Boolean = false,
    val unreadMessages: Int = 0
)
