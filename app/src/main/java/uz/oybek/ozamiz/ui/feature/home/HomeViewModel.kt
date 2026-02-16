package uz.oybek.ozamiz.ui.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import uz.oybek.ozamiz.AppConstants
import uz.oybek.ozamiz.appContext
import uz.oybek.ozamiz.data.food.DietController
import uz.oybek.ozamiz.data.logs.DailyLogRepository
import uz.oybek.ozamiz.data.logs.DailySummaryRepository
import uz.oybek.ozamiz.data.logs.WaterLogRepository
import uz.oybek.ozamiz.data.logs.model.DailySummary
import uz.oybek.ozamiz.data.logs.model.UserExercise
import uz.oybek.ozamiz.data.logs.model.WaterLog
import uz.oybek.ozamiz.data.streak.StreakRepository
import uz.oybek.ozamiz.data.user.UserRepository
import uz.oybek.ozamiz.data.user.local.user
import uz.oybek.ozamiz.data.user.model.User
import uz.oybek.ozamiz.data.utils.DataWrapper
import uz.oybek.ozamiz.data.utils.DateUtils
import java.util.Date
import kotlin.math.roundToInt

class HomeViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(
        HomeUiState(isInitialLoading = false)
    )
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private val _showRateAppEvent = kotlinx.coroutines.flow.MutableSharedFlow<Unit>()
    val showRateAppEvent = _showRateAppEvent.asSharedFlow()

    private val glassVolume = AppConstants.Water.GLASS_VOLUME_ML
    private var previousWaterAmount = 0

    init {
        _uiState.update {
            it.copy(
                datesForScroller = DateUtils.generateDatesForScroller(),
                selectedDate = Date()
            )
        }
        viewModelScope.launch {
            try {
                StreakRepository.updateStreak()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        UserRepository.observeCurrentUser { user ->
            if (user != null && user.valid) {
                _uiState.update {
                    it.copy(streakInfo = user.streakInfo)
                }
                val streak = user.streakInfo?.currentStreak ?: 0
                if (streak >= 3) {
                    val prefs = appContext.getSharedPreferences(
                        "app_prefs",
                        android.content.Context.MODE_PRIVATE
                    )
                    val hasShown = prefs.getBoolean("has_shown_rate_sheet", false)
                    if (!hasShown) {
                        viewModelScope.launch {
                            _showRateAppEvent.emit(Unit)
                        }
                    }
                }
            }
        }

        loadData(_uiState.value.selectedDate)

        val dates = _uiState.value.datesForScroller
        if (dates.isNotEmpty()) {
            val start = dates.first()
            val end = dates.last()
            DailySummaryRepository.observeSummaries(start, end).onEach { summaries ->
                _uiState.update { it.copy(dailySummaries = summaries) }
            }
                .launchIn(viewModelScope)
        }
    }

    private fun loadData(date: Date) {
        val summaryFlow = DailyLogRepository.observeDailySummary(date)
        val exerciseFlow = uz.oybek.ozamiz.data.logs.ExerciseLogRepository.getLogs(date)

        kotlinx.coroutines.flow.combine(summaryFlow, exerciseFlow) { summaryWrapper, exercises ->
            Pair(summaryWrapper, exercises)
        }.map { (summaryWrapper, exercises) ->
            processStateUpdate(
                date = date,
                user = user,
                summaryWrapper = summaryWrapper,
                exerciseLogs = exercises
            )
        }.launchIn(viewModelScope)
    }

    private fun processStateUpdate(
        date: Date,
        user: User?,
        summaryWrapper: DataWrapper<DailySummary>,
        exerciseLogs: List<uz.oybek.ozamiz.data.logs.model.ExerciseLog>
    ) {
        if (user == null || !user.valid) {
            _uiState.update {
                it.copy(
                    isInitialLoading = false,
                    isRefreshing = false
                )
            }
            return
        }

        when (summaryWrapper) {
            is DataWrapper.Success -> {
                val summary = summaryWrapper.data
                val macros = DietController.calculateMacrosByPercent(user.targetCalories)
                val calRemaining =
                    (user.targetCalories - summary.consumedCalories)
                val currentWater = summary.consumedWaterMl

                val sortedExerciseLogs = exerciseLogs
                    .sortedByDescending { it.timestamp as? Long ?: 0L }

                _uiState.update {
                    it.copy(
                        isInitialLoading = false,
                        isRefreshing = false,
                        selectedDate = date,
                        streakInfo = user.streakInfo,
                        caloriesRemaining = calRemaining,
                        caloriesConsumed = summary.consumedCalories,
                        caloriesTarget = user.targetCalories,
                        waterConsumed = currentWater,
                        waterTarget = user.targetWaterMl,
                        waterGlasses = calculateGlasses(currentWater, previousWaterAmount),
                        exerciseBurnt = summary.burnedCalories,
                        exerciseTarget = AppConstants.Exercise.DEFAULT_TARGET_CALORIES,
                        carbsConsumed = summary.consumedCarbs.roundToInt(),
                        carbsTarget = macros.carbs,
                        proteinConsumed = summary.consumedProtein.roundToInt(),
                        proteinTarget = macros.protein,
                        fatConsumed = summary.consumedFat.roundToInt(),
                        fatTarget = macros.fat,
                        exerciseLogs = sortedExerciseLogs,
                        recommendedExercises =
                            if (sortedExerciseLogs.isEmpty()) {
                                UserExercise.getExercisesList(user.gender)
                                    .take(4)
                            } else {
                                emptyList()
                            }
                    )
                }
                previousWaterAmount = currentWater
            }

            is DataWrapper.Error -> {
                _uiState.update {
                    it.copy(
                        isInitialLoading = false,
                        isRefreshing = false
                    )
                }
            }
        }
    }

    private fun calculateGlasses(current: Int, old: Int): List<WaterGlassState> {
        val totalGlassesNeeded =
            maxOf(AppConstants.Water.DEFAULT_GLASSES_COUNT, (current / glassVolume) + 2)
        val glasses = ArrayList<WaterGlassState>()

        for (i in 0 until totalGlassesNeeded) {
            val threshold = (i + 1) * glassVolume
            val isFilled = current >= threshold
            val wasFilled = old >= threshold
            val shouldAnimate = isFilled && !wasFilled
            val isNext = !isFilled && (current >= (i * glassVolume))

            glasses.add(
                WaterGlassState(
                    id = i,
                    isFilled = isFilled,
                    isNextToAdd = isNext,
                    shouldAnimateFill = false
                )
            )
        }
        return glasses
    }

    fun onDateSelected(date: Date) {
        previousWaterAmount = 0
        loadData(date)
    }

    fun refresh() {
        _uiState.update { it.copy(isRefreshing = true) }
        loadData(uiState.value.selectedDate)
    }

    fun addWater(amount: Int) {
        val date = uiState.value.selectedDate
        viewModelScope.launch {
            WaterLogRepository.add(WaterLog(amountMl = amount), date)
            uz.oybek.ozamiz.data.streak.StreakRepository.updateStreak()
        }
    }
}