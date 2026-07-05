package uz.oybek.ozamiz.ui.feature.home

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import uz.oybek.ozamiz.data.config.AppPreference
import uz.oybek.ozamiz.data.level.PointsEvent
import uz.oybek.ozamiz.data.level.PointsRepository
import uz.oybek.ozamiz.data.logs.ExerciseLogRepository
import uz.oybek.ozamiz.data.logs.FoodLogRepository
import uz.oybek.ozamiz.data.logs.LogRepository
import uz.oybek.ozamiz.data.logs.WaterLogRepository
import uz.oybek.ozamiz.data.logs.WeightLogRepository
import uz.oybek.ozamiz.data.logs.model.DailySummary
import uz.oybek.ozamiz.data.logs.model.ExerciseLog
import uz.oybek.ozamiz.data.logs.model.FoodLog
import uz.oybek.ozamiz.data.logs.model.WaterLog
import uz.oybek.ozamiz.data.streak.StreakInfo
import uz.oybek.ozamiz.data.user.UserRepository
import uz.oybek.ozamiz.data.user.local.user
import uz.oybek.ozamiz.data.utils.DataWrapper
import uz.oybek.ozamiz.data.utils.DateUtils
import uz.oybek.ozamiz.data.utils.dateApiFormat
import java.util.Date
import kotlin.math.roundToInt

class HomeViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(
        HomeUiState(isInitialLoading = false)
    )
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private val _showRateAppEvent = kotlinx.coroutines.flow.MutableSharedFlow<Unit>()
    val showRateAppEvent = _showRateAppEvent.asSharedFlow()

    private val _showStreakSheetEvent =
        kotlinx.coroutines.flow.MutableSharedFlow<Pair<StreakInfo, Boolean>>()
    val showStreakSheetEvent = _showStreakSheetEvent.asSharedFlow()

    private val _showWeightLogEvent = kotlinx.coroutines.flow.MutableSharedFlow<Unit>()
    val showWeightLogEvent = _showWeightLogEvent.asSharedFlow()

    private var dataCollectionJob: Job? = null

    init {
        val startTime = System.currentTimeMillis()
        val initialDates = DateUtils.generateDatesForScroller()
        val initialSelectedDate = DateUtils.getStartOfDay(Date())
        _uiState.update {
            it.copy(
                datesForScroller = initialDates,
                selectedDate = initialSelectedDate
            )
        }
        UserRepository.observeCurrentUser { user ->
            if (user != null && user.valid) {
                _uiState.update { state ->
                    state.copy(
                        streakInfo = user.streakInfo,
                        userPoints = user.scorePoints
                    )
                }
            }
        }
        startLoadingData(initialSelectedDate)
        observeScrollerSummaries()
        checkWeightLog()
    }

    private fun observeScrollerSummaries() {
        val startTime = System.currentTimeMillis()
        val dates = _uiState.value.datesForScroller
        if (dates.isEmpty()) return

        val flows = dates.map { date ->
            LogRepository.observeDailySummary(date).map { date to it }
        }

        viewModelScope.launch {
            combine(flows) { array ->
                array.filter { it.second is DataWrapper.Success }
                    .associate { it.first.dateApiFormat() to (it.second as DataWrapper.Success).data }
            }.collect { map ->
                _uiState.update { state ->
                    state.copy(dailySummaries = map)
                }
            }
        }
        Log.d("HomePerf", "HomeViewModel.observeScrollerSummaries execution time: ${System.currentTimeMillis() - startTime} ms")
    }

    private fun checkWeightLog() {
        val startTime = System.currentTimeMillis()
        WeightLogRepository.needShowWeightLog(user.uid).onEach { needShow ->
            _uiState.update { it.copy(showWeightLogCard = needShow) }
        }.launchIn(viewModelScope)
        Log.d("HomePerf", "HomeViewModel.checkWeightLog execution time: ${System.currentTimeMillis() - startTime} ms")
    }

    private fun startLoadingData(date: Date) {
        val startTime = System.currentTimeMillis()
        dataCollectionJob?.cancel()
        dataCollectionJob = viewModelScope.launch {
            try {
                combine(
                    LogRepository.observeDailySummary(date),
                    ExerciseLogRepository.getLogs(date),
                    FoodLogRepository.getAllLogs(date)
                ) { s, e, f -> Triple(s, e, f) }
                    .collect { (s, e, f) ->
                        processStateUpdate(date, s, e, f)
                    }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        Log.d("HomePerf", "HomeViewModel.startLoadingData execution time: ${System.currentTimeMillis() - startTime} ms")
    }

    private fun processStateUpdate(
        date: Date,
        summaryWrapper: DataWrapper<DailySummary>,
        exerciseLogs: List<ExerciseLog>,
        foodLogs: List<FoodLog>
    ) {
        val startTime = System.currentTimeMillis()
        if (!user.valid) {
            _uiState.update { it.copy(isInitialLoading = false, isRefreshing = false) }
            Log.d("HomePerf", "HomeViewModel.processStateUpdate (invalid user) execution time: ${System.currentTimeMillis() - startTime} ms")
            return
        }
        when (summaryWrapper) {
            is DataWrapper.Success -> {
                val summary = summaryWrapper.data
                val targetCals = user.targetCalories

                val currentWater = summary.consumedWaterMl
                val currentExerciseBurnt = summary.burnedCalories

                _uiState.update {
                    it.copy(
                        isInitialLoading = false,
                        isRefreshing = false,
                        selectedDate = date,
                        caloriesRemaining = targetCals - summary.consumedCalories,
                        caloriesConsumed = summary.consumedCalories,
                        caloriesTarget = targetCals,
                        waterConsumed = currentWater,
                        waterTarget = user.targetWaterMl,
                        exerciseBurnt = currentExerciseBurnt,
                        exerciseTarget = 300,
                        carbsConsumed = summary.consumedCarbs.roundToInt(),
                        proteinConsumed = summary.consumedProtein.roundToInt(),
                        fatConsumed = summary.consumedFat.roundToInt(),
                        proteinTarget = user.targetProtein,
                        fatTarget = user.targetFat,
                        carbsTarget = user.targetCarbs,
                        exerciseLogs = exerciseLogs,
                        dailyFoodLogs = foodLogs
                    )
                }
            }

            is DataWrapper.Error -> {
                _uiState.update { it.copy(isInitialLoading = false, isRefreshing = false) }
            }
        }
        Log.d("HomePerf", "HomeViewModel.processStateUpdate execution time: ${System.currentTimeMillis() - startTime} ms")
    }

    fun onDateSelected(date: Date) {
        _uiState.update { it.copy(selectedDate = date, isInitialLoading = false) }
        startLoadingData(date)
    }

    fun refresh() {
        _uiState.update { it.copy(isRefreshing = true) }
        startLoadingData(_uiState.value.selectedDate)
    }

    fun addWater(amount: Int) {
        val startTime = System.currentTimeMillis()
        val currentWater = _uiState.value.waterConsumed
        if (currentWater + amount > 4000) {
            return
        }
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    waterConsumed = currentWater + amount
                )
            }
            PointsRepository.onEvent(PointsEvent.AddWater(currentWater + amount))
            val date = uiState.value.selectedDate
            WaterLogRepository.add(WaterLog(amountMl = amount), date)
        }
    }

    fun isFirstLogAdded(): Boolean {
        return AppPreference.isFirstLogAdded
    }

    fun setFirstLogAdded(added: Boolean) {
        AppPreference.isFirstLogAdded = added
    }

    fun isSecondOnboardingSheetSeen(): Boolean {
        return AppPreference.isSecondOnboardingSheetSeen
    }

    fun setSecondOnboardingSheetSeen(seen: Boolean) {
        AppPreference.isSecondOnboardingSheetSeen = seen
    }
}
