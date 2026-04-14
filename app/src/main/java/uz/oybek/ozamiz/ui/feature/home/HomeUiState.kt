package uz.oybek.ozamiz.ui.feature.home

import uz.oybek.ozamiz.data.streak.StreakInfo
import uz.oybek.ozamiz.data.logs.model.ExerciseLog
import uz.oybek.ozamiz.data.logs.model.UserExercise
import java.util.Date
import uz.oybek.ozamiz.data.course.model.CourseLevel
import uz.oybek.ozamiz.data.course.model.CourseLesson

data class HomeUiState(
    val isInitialLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val selectedDate: Date = Date(),
    val datesForScroller: List<Date> = emptyList(),
    val dailySummaries: Map<String, uz.oybek.ozamiz.data.logs.model.DailySummary> = emptyMap(),
    val streakInfo: StreakInfo? = null,
    val caloriesRemaining: Int = 0,
    val caloriesConsumed: Int = 0,
    val caloriesTarget: Int = 0,
    val waterConsumed: Int = 0,
    val waterTarget: Int = 0,
    val waterGlasses: List<WaterGlassState> = emptyList(),
    val exerciseBurnt: Int = 0,
    val exerciseTarget: Int = 0,
    val dailyFoodLogs: List<uz.oybek.ozamiz.data.logs.model.FoodLog> = emptyList(),
    val carbsConsumed: Int = 0,
    val carbsTarget: Int = 0,
    val proteinConsumed: Int = 0,
    val proteinTarget: Int = 0,
    val fatConsumed: Int = 0,
    val fatTarget: Int = 0,
    val exerciseLogs: List<ExerciseLog> = emptyList(),
    val recommendedExercises: List<UserExercise> = emptyList(),
    val isProteinExceeded: Boolean = false,
    val isCarbsExceeded: Boolean = false,
    val isFatExceeded: Boolean = false,
    val isCalorieOptimal: Boolean = false,
    val isCalorieExceeded: Boolean = false,
    val courseLevels: List<CourseLevel> = emptyList(),
    val activeLesson: CourseLesson? = null,
    val userPoints: Int = 0,
    val pointsJustAdded: Int = 0,
    val waterJustAdded: Boolean = false,
    val exerciseJustAddedType: String? = null,
    val recommendedFoods: List<uz.oybek.ozamiz.data.menu.model.Menu> = emptyList(),
    val recommendingForMeal: String? = null,
    val dailyTip: uz.oybek.ozamiz.ui.utils.DailyTipUtil.Tip? = null,
    val isTipVisible: Boolean = true,
    val showWeightLogCard: Boolean = false,
    val motivationalMessage: String? = null
)