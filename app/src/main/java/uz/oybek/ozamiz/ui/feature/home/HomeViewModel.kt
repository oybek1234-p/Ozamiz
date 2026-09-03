package uz.oybek.ozamiz.ui.feature.home

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asFlow
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.receiveAsFlow
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
import uz.oybek.ozamiz.data.user.UserProfileRepository
import uz.oybek.ozamiz.data.user.local.LocalUser
import uz.oybek.ozamiz.data.user.local.user
import uz.oybek.ozamiz.data.user.model.User
import uz.oybek.ozamiz.data.utils.DataWrapper
import uz.oybek.ozamiz.data.utils.DateUtils
import uz.oybek.ozamiz.data.utils.dateApiFormat
import uz.oybek.ozamiz.data.utils.isToday
import java.util.Date
import kotlin.math.roundToInt

import uz.oybek.ozamiz.data.user.model.daysFromRegister

class HomeViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(
        HomeUiState(isInitialLoading = false)
    )
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private val _showRateAppEvent = MutableSharedFlow<Unit>()
    val showRateAppEvent = _showRateAppEvent.asSharedFlow()

    private val _showStreakSheetEvent =
        MutableSharedFlow<Pair<StreakInfo, Boolean>>()
    val showStreakSheetEvent = _showStreakSheetEvent.asSharedFlow()

    private val _showWeightLogEvent = MutableSharedFlow<Unit>()
    val showWeightLogEvent = _showWeightLogEvent.asSharedFlow()

    var lastCaloriesEaten: Int? = null
    var lastCaloriesRemaining: Int? = null
    var lastCaloriesBurned: Int? = null
    var lastProteinConsumed: Int? = null
    var lastFatConsumed: Int? = null
    var lastCarbsConsumed: Int? = null
    private var lastDate: Date? = null

    fun checkDateReset(newDate: Date) {
        if (lastDate == null || !DateUtils.isSameDay(lastDate, newDate)) {
            lastDate = newDate
            lastCaloriesEaten = null
            lastCaloriesRemaining = null
            lastCaloriesBurned = null
            lastProteinConsumed = null
            lastFatConsumed = null
            lastCarbsConsumed = null
        }
    }

    private val _exerciseAddedEvent = MutableSharedFlow<Int>()
    val exerciseAddedEvent = _exerciseAddedEvent.asSharedFlow()

    private val _showStreakFreezeEvent = MutableSharedFlow<Unit>()
    val showStreakFreezeEvent = _showStreakFreezeEvent.asSharedFlow()

    private var hasCheckedFreezeThisSession = false
    
    var pendingCameraArgs: android.os.Bundle? = null
        private set

    fun setPendingCameraArgs(args: android.os.Bundle?) {
        pendingCameraArgs = args
    }

    private fun checkStreakFrozen(user: User) {
        if (hasCheckedFreezeThisSession) return
        if (uz.oybek.ozamiz.data.streak.StreakManager.isStreakFrozen(user.streakInfo)) {
            hasCheckedFreezeThisSession = true
            viewModelScope.launch {
                _showStreakFreezeEvent.emit(Unit)
            }
        }
    }

    private var dataCollectionJob: Job? = null

    init {
        val initialDates = DateUtils.generateDatesForScroller()
        val initialSelectedDate = DateUtils.getStartOfDay(Date())
        _uiState.update {
            it.copy(
                datesForScroller = initialDates,
                selectedDate = initialSelectedDate
            )
        }
        viewModelScope.launch {
            LocalUser.userLive.asFlow().collect { user ->
                if (user.valid) {
                    val daysFromRegister = user.daysFromRegister()
                    
                    _uiState.update { state ->
                        state.copy(
                            streakInfo = user.streakInfo,
                            userPoints = user.scorePoints,
                            daysFromRegister = daysFromRegister,
                            paymentChecking = user.paymentChecking
                        )
                    }
                    checkStreakFrozen(user)
                }
            }
        }
        startLoadingData(initialSelectedDate)
        observeScrollerSummaries()
        checkWeightLog()
    }

    private fun observeScrollerSummaries() {
        val dates = _uiState.value.datesForScroller
        if (dates.isEmpty()) return

        // Observe only selected date and adjacent dates to keep home screen smooth and fast
        val selectedDate = _uiState.value.selectedDate
        val datesToObserve = dates.filter {
            kotlin.math.abs(it.time - selectedDate.time) <= 3 * 86400000L
        }

        val flows = datesToObserve.map { date ->
            LogRepository.observeDailySummary(date).map { date to it }
        }

        viewModelScope.launch {
            combine(flows) { array ->
                array.filter { it.second is DataWrapper.Success }
                    .associate { it.first.dateApiFormat() to (it.second as DataWrapper.Success).data }
            }.collect { map ->
                _uiState.update { state ->
                    state.copy(dailySummaries = state.dailySummaries + map)
                }
            }
        }
    }

    private fun checkWeightLog() {
        WeightLogRepository.needShowWeightLog(user.uid).onEach { needShow ->
            _uiState.update { it.copy(showWeightLogCard = needShow) }
        }.launchIn(viewModelScope)
    }

    private data class FlowData(
        val summary: DataWrapper<DailySummary>,
        val exerciseLogs: List<ExerciseLog>,
        val foodLogs: List<FoodLog>,
        val waterLogs: List<WaterLog>
    )

    private fun startLoadingData(date: Date) {
        dataCollectionJob?.cancel()
        dataCollectionJob = viewModelScope.launch {
            try {
                combine(
                    LogRepository.observeDailySummary(date),
                    ExerciseLogRepository.getLogs(date),
                    FoodLogRepository.getAllLogs(date),
                    WaterLogRepository.getLogs(date)
                ) { s, e, f, w -> FlowData(s, e, f, w) }
                    .collect { data ->
                        processStateUpdate(date, data.summary, data.exerciseLogs, data.foodLogs, data.waterLogs)
                    }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private suspend fun processStateUpdate(
        date: Date,
        summaryWrapper: DataWrapper<DailySummary>,
        exerciseLogs: List<ExerciseLog>,
        foodLogs: List<FoodLog>,
        waterLogs: List<WaterLog>
    ) {
        if (!user.valid) {
            _uiState.update { it.copy(isInitialLoading = false, isRefreshing = false) }
            return
        }
        when (summaryWrapper) {
            is DataWrapper.Success -> {
                val summary = summaryWrapper.data
                val targetCals = user.targetCalories
                val daysFromRegister = user.daysFromRegister()

                val currentWater = summary.consumedWaterMl
                val currentExerciseBurnt = summary.burnedCalories
                val waterTarget = user.targetWaterMl

                val remaining = targetCals - summary.consumedCalories

                _uiState.update {
                    it.copy(
                        isInitialLoading = false,
                        isRefreshing = false,
                        selectedDate = date,
                        caloriesRemaining = remaining,
                        caloriesConsumed = summary.consumedCalories,
                        caloriesTarget = targetCals,
                        waterConsumed = currentWater,
                        waterTarget = waterTarget,
                        exerciseBurnt = currentExerciseBurnt,
                        exerciseTarget = 300,
                        carbsConsumed = summary.consumedCarbs.roundToInt(),
                        proteinConsumed = summary.consumedProtein.roundToInt(),
                        fatConsumed = summary.consumedFat.roundToInt(),
                        proteinTarget = user.targetProtein,
                        fatTarget = user.targetFat,
                        carbsTarget = user.targetCarbs,
                        exerciseLogs = exerciseLogs,
                        waterLogs = waterLogs,
                        dailyFoodLogs = foodLogs
                    )
                }
            }

            is DataWrapper.Error -> {
                _uiState.update { it.copy(isInitialLoading = false, isRefreshing = false) }
            }
        }
    }

    fun deleteExerciseLog(log: ExerciseLog) {
        viewModelScope.launch {
            val date = uiState.value.selectedDate
            ExerciseLogRepository.delete(log.logId, date)
        }
    }

    fun deleteWaterLog(log: WaterLog) {
        viewModelScope.launch {
            val date = uiState.value.selectedDate
            WaterLogRepository.delete(log.logId, date)
        }
    }

    fun onDateSelected(date: Date) {
        _uiState.update { it.copy(selectedDate = date, isInitialLoading = false) }
        startLoadingData(date)
    }

    fun refresh() {
        _uiState.update { it.copy(isRefreshing = true) }
        viewModelScope.launch {
            delay(600)
            _uiState.update { it.copy(isRefreshing = false) }
        }
        startLoadingData(_uiState.value.selectedDate)
    }

    fun addWater(amount: Int) {
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
            WaterLogRepository.add(
                WaterLog(
                    amountMl = amount,
                    timestamp = System.currentTimeMillis(),
                    timestampLong = System.currentTimeMillis()
                ),
                date
            )
        }
    }

    fun addExercise(exerciseType: String, durationMin: Int, caloriesBurned: Int) {
        viewModelScope.launch {
            val date = uiState.value.selectedDate
            val log = ExerciseLog(
                exerciseType = exerciseType,
                durationMin = durationMin,
                caloriesBurned = caloriesBurned,
                timestamp = System.currentTimeMillis(),
                timestampLong = System.currentTimeMillis()
            )
            val success = ExerciseLogRepository.add(log, date)
            if (success) {
                PointsRepository.onEvent(PointsEvent.Exercise(caloriesBurned))
                _exerciseAddedEvent.emit(caloriesBurned)
            }
        }
    }
}
