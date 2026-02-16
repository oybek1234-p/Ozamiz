package uz.oybek.ozamiz.ui.feature.home

import uz.oybek.ozamiz.data.streak.StreakInfo
import uz.oybek.ozamiz.data.logs.model.ExerciseLog
import uz.oybek.ozamiz.data.logs.model.UserExercise
import java.util.Date

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
    val carbsConsumed: Int = 0,
    val carbsTarget: Int = 0,
    val proteinConsumed: Int = 0,
    val proteinTarget: Int = 0,
    val fatConsumed: Int = 0,
    val fatTarget: Int = 0,
    val exerciseLogs: List<ExerciseLog> = emptyList(),
    val recommendedExercises: List<UserExercise> = emptyList()
)