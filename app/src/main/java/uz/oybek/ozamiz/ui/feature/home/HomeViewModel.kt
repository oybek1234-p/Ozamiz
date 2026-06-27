package uz.oybek.ozamiz.ui.feature.home

import androidx.core.content.edit
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import uz.oybek.ozamiz.AppConstants
import uz.oybek.ozamiz.appContext
import uz.oybek.ozamiz.data.ai.AiMenuRepository

import uz.oybek.ozamiz.data.food.DietController

import uz.oybek.ozamiz.data.logs.DailyLogRepository
import uz.oybek.ozamiz.data.logs.DailySummaryRepository
import uz.oybek.ozamiz.data.logs.ExerciseLogRepository
import uz.oybek.ozamiz.data.logs.FoodLogRepository
import uz.oybek.ozamiz.data.logs.WaterLogRepository
import uz.oybek.ozamiz.data.logs.WeightLogRepository
import uz.oybek.ozamiz.data.logs.model.DailySummary
import uz.oybek.ozamiz.data.logs.model.ExerciseLog
import uz.oybek.ozamiz.data.logs.model.FoodLog
import uz.oybek.ozamiz.data.logs.model.UserExercise
import uz.oybek.ozamiz.data.logs.model.WaterLog
import uz.oybek.ozamiz.data.streak.StreakRepository
import uz.oybek.ozamiz.data.user.UserRepository
import uz.oybek.ozamiz.data.user.local.user
import uz.oybek.ozamiz.data.user.model.User
import uz.oybek.ozamiz.data.utils.DataWrapper
import uz.oybek.ozamiz.data.utils.DateUtils
import uz.oybek.ozamiz.data.utils.dateApiFormat
import uz.oybek.ozamiz.ui.utils.DailyTipUtil
import java.util.Date
import kotlin.math.roundToInt

class HomeViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(
        HomeUiState(isInitialLoading = true)
    )
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private val _foodJustAddedEvent = kotlinx.coroutines.flow.MutableSharedFlow<Unit>(replay = 1)
    val foodJustAddedEvent = _foodJustAddedEvent.asSharedFlow()

    private val _showRateAppEvent = kotlinx.coroutines.flow.MutableSharedFlow<Unit>()
    val showRateAppEvent = _showRateAppEvent.asSharedFlow()

    private val _showAppOnboardingEvent =
        kotlinx.coroutines.flow.MutableSharedFlow<Unit>(replay = 1)
    val showAppOnboardingEvent = _showAppOnboardingEvent.asSharedFlow()





    private val _showStreakSheetEvent = kotlinx.coroutines.flow.MutableSharedFlow<uz.oybek.ozamiz.data.streak.StreakInfo>()
    val showStreakSheetEvent = _showStreakSheetEvent.asSharedFlow()

    private val glassVolume = AppConstants.Water.GLASS_VOLUME_ML
    private var previousWaterAmount = 0
    private var previousFoodCalories = 0
    private var previousExerciseBurnt = 0
    private var previousPoints = -1
    private var isFirstLoadCompleted = false
    private var isTipInitialized = false
    private var isFoodAdditionPending = false
    private var isWaterAdditionPending = false
    private var isExerciseAdditionPending = false
    private var dataCollectionJob: Job? = null

    private var summaryJob: Job? = null

    init {
        AiMenuRepository.init(appContext)
        observeAiMenu()
        viewModelScope.launch {
            uz.oybek.ozamiz.data.ai.DailyAiAnalysisRepository.checkAndGenerateAdvice()
        }
        observeAiDietitianAdvice()
        val initialUser = UserRepository.user
        val initialDates = DateUtils.generateDatesForScroller()
        val initialSelectedDate = DateUtils.getStartOfDay(Date())

        if (initialUser.valid) {
            val targetCals =
                if (initialUser.targetCalories > 0) initialUser.targetCalories else DietController.calculateTargetCalories(
                    initialUser
                )
            val targetWater =
                if (initialUser.targetWaterMl > 0) initialUser.targetWaterMl else DietController.calculateTargetWaterMl(
                    initialUser.weight
                )
            val macros = DietController.calculateMacrosByPercent(targetCals)

                val pTarget = if (initialUser.targetProtein > 0) initialUser.targetProtein else macros.protein
                val fTarget = if (initialUser.targetFat > 0) initialUser.targetFat else macros.fat
                val cTarget = if (initialUser.targetCarbs > 0) initialUser.targetCarbs else macros.carbs

                _uiState.update {
                    it.copy(
                        isInitialLoading = false,
                        datesForScroller = initialDates,
                        selectedDate = initialSelectedDate,
                        caloriesTarget = targetCals,
                        waterTarget = targetWater,
                        proteinTarget = pTarget,
                        fatTarget = fTarget,
                        carbsTarget = cTarget,
                        caloriesRemaining = targetCals,
                        userPoints = initialUser.scorePoints,
                        streakInfo = initialUser.streakInfo
                    )
                }
        } else {
            _uiState.update {
                it.copy(
                    datesForScroller = initialDates,
                    selectedDate = initialSelectedDate
                )
            }
        }
        initDailyTip()


        UserRepository.observeCurrentUser { user ->
            if (user != null && user.valid) {
                val newPoints = user.scorePoints
                val added =
                    if (previousPoints >= 0 && newPoints != previousPoints) newPoints - previousPoints else 0
                previousPoints = newPoints

                val targetCals =
                    if (user.targetCalories > 0) user.targetCalories else DietController.calculateTargetCalories(
                        user
                    )
                val targetWater =
                    if (user.targetWaterMl > 0) user.targetWaterMl else DietController.calculateTargetWaterMl(
                        user.weight
                    )
                val macros = DietController.calculateMacrosByPercent(targetCals)

                val pTarget = if (user.targetProtein > 0) user.targetProtein else macros.protein
                val fTarget = if (user.targetFat > 0) user.targetFat else macros.fat
                val cTarget = if (user.targetCarbs > 0) user.targetCarbs else macros.carbs

                _uiState.update {
                    it.copy(
                        streakInfo = user.streakInfo,
                        userPoints = newPoints,
                        pointsJustAdded = if (added > 0) added else 0,
                        caloriesTarget = targetCals,
                        waterTarget = targetWater,
                        proteinTarget = pTarget,
                        fatTarget = fTarget,
                        carbsTarget = cTarget,
                        isInitialLoading = false
                    )
                }

                checkUserAchievements(user)

                if (dataCollectionJob == null || !dataCollectionJob!!.isActive) {
                    startLoadingData(_uiState.value.selectedDate)
                }

                if (summaryJob == null || !summaryJob!!.isActive) {
                    val dates = _uiState.value.datesForScroller
                    if (dates.isNotEmpty()) {
                        val start = dates.first()
                        val end = dates.last()
                        summaryJob = DailySummaryRepository.observeSummaries(start, end)
                            .onEach { summaries ->
                                _uiState.update { it.copy(dailySummaries = summaries) }
                            }.launchIn(viewModelScope)
                    }
                }
            } else if (user == null || !user.valid) {
                _uiState.update { it.copy(isInitialLoading = false, dailySummaries = emptyMap()) }
                summaryJob?.cancel()
                summaryJob = null
                dataCollectionJob?.cancel()
                dataCollectionJob = null
                previousWaterAmount = 0
                previousFoodCalories = 0
                previousExerciseBurnt = 0
                isFirstLoadCompleted = false
            }
        }



        WeightLogRepository.getLogsFlow(user.uid).onEach { result ->
            if (result is DataWrapper.Success) {
                val logs = result.data.sortedBy { it.timestampLong }
                val showWeightProgress = if (logs.isNotEmpty()) {
                    val lastLog = logs.last()
                    val oneWeekMs = 7L * 24 * 60 * 60 * 1000
                    System.currentTimeMillis() - lastLog.timestampLong > oneWeekMs
                } else {
                    true
                }
                _uiState.update { it.copy(showWeightLogCard = showWeightProgress) }
            }
        }.launchIn(viewModelScope)
    }

    private fun checkUserAchievements(user: User) {
    }

    private fun startLoadingData(date: Date) {
        dataCollectionJob?.cancel()
        dataCollectionJob = viewModelScope.launch {
            val timeoutJob = launch {
                delay(5000)
                if (_uiState.value.isRefreshing || _uiState.value.isInitialLoading) {
                    _uiState.update { it.copy(isRefreshing = false, isInitialLoading = false) }
                }
            }
            val summaryFlow = DailyLogRepository.observeDailySummary(date)
            val exerciseFlow = ExerciseLogRepository.getLogs(date)
            val foodFlow = FoodLogRepository.getAllLogs(date)

            combine(
                summaryFlow,
                exerciseFlow,
                foodFlow
            ) { summaryWrapper, exercises, foods ->
                Triple(summaryWrapper, exercises, foods)
            }.collect { (summaryWrapper, exercises, foods) ->
                timeoutJob.cancel()
                processStateUpdate(date, user, summaryWrapper, exercises, foods)
            }
        }
    }

    private fun processStateUpdate(
        date: Date,
        user: User?,
        summaryWrapper: DataWrapper<DailySummary>,
        exerciseLogs: List<ExerciseLog>,
        foodLogs: List<FoodLog>
    ) {
        if (user == null || !user.valid) {
            _uiState.update { it.copy(isInitialLoading = false, isRefreshing = false) }
            return
        }
        when (summaryWrapper) {
            is DataWrapper.Success -> {
                val summary = summaryWrapper.data
                val targetCals = if (user.targetCalories > 0) user.targetCalories else DietController.calculateTargetCalories(user)
                val macros = DietController.calculateMacrosByPercent(targetCals)

                val currentWater = summary.consumedWaterMl
                val waterJustAddedEvent = isFirstLoadCompleted && currentWater > previousWaterAmount

                val currentFoodCalories = summary.consumedCalories
                val foodJustAddedEvent =
                    isFirstLoadCompleted && currentFoodCalories > previousFoodCalories

                val currentExerciseBurnt = summary.burnedCalories
                val exerciseJustAddedEvent =
                    isFirstLoadCompleted && currentExerciseBurnt > previousExerciseBurnt
                val lastExerciseType = if (exerciseJustAddedEvent) {
                    exerciseLogs.maxByOrNull { it.timestamp as? Long ?: 0L }?.exerciseType
                } else null


                val netCalories = currentFoodCalories
                val progress =
                    if (user.targetCalories > 0) netCalories.toFloat() / user.targetCalories else 0f

                val waterPercent =
                    if (user.targetWaterMl > 0) currentWater.toFloat() / user.targetWaterMl else 0f
                val waterStatus = when {
                    waterPercent >= 1f -> "G'alaba! 🎉"
                    waterPercent >= 0.7f -> "Deyarli tayyor ✨"
                    waterPercent >= 0.3f -> "Zo'r ketyapsiz 👍"
                    else -> "Suv ichamiz 💧"
                }

                val exercisePercent =
                    if (AppConstants.Exercise.DEFAULT_TARGET_CALORIES > 0) currentExerciseBurnt.toFloat() / AppConstants.Exercise.DEFAULT_TARGET_CALORIES else 0f
                val exerciseStatus = when {
                    exercisePercent >= 1f -> "Marra zabt etildi! 🏆"
                    exercisePercent >= 0.7f -> "Daxshatli shijoat! 💪"
                    exercisePercent >= 0.2f -> "Yog' yonyapti ⚡"
                    else -> "Harakatni boshlang 🏃"
                }

                _uiState.update {
                    it.copy(
                        isInitialLoading = false,
                        isRefreshing = false,
                        selectedDate = date,
                        caloriesRemaining = targetCals - netCalories,
                        caloriesConsumed = netCalories,
                        foodConsumed = currentFoodCalories,
                        caloriesTarget = targetCals,
                        waterConsumed = currentWater,
                        waterTarget = user.targetWaterMl,
                        waterGlasses = calculateGlasses(currentWater, previousWaterAmount),
                        waterJustAdded = waterJustAddedEvent,
                        exerciseJustAddedType = lastExerciseType,
                        exerciseBurnt = currentExerciseBurnt,
                        exerciseTarget = AppConstants.Exercise.DEFAULT_TARGET_CALORIES,
                        carbsConsumed = summary.consumedCarbs.roundToInt(),
                        proteinConsumed = summary.consumedProtein.roundToInt(),
                        fatConsumed = summary.consumedFat.roundToInt(),
                        proteinTarget = if (user.targetProtein > 0) user.targetProtein else macros.protein,
                        fatTarget = if (user.targetFat > 0) user.targetFat else macros.fat,
                        carbsTarget = if (user.targetCarbs > 0) user.targetCarbs else macros.carbs,
                        exerciseLogs = exerciseLogs.sortedByDescending { log ->
                            log.timestamp as? Long ?: 0L
                        },
                        dailyFoodLogs = foodLogs,
                        recommendedExercises = if (exerciseLogs.isEmpty()) UserExercise.getExercisesList(
                            user.gender
                        ).take(4) else emptyList(),
                        isProteinExceeded = summary.consumedProtein > macros.protein + 50,
                        isCarbsExceeded = summary.consumedCarbs > macros.carbs + 70,
                        isFatExceeded = summary.consumedFat > macros.fat + 30,
                        isCalorieOptimal = netCalories >= targetCals - 50 && netCalories <= targetCals + 100,
                        isCalorieExceeded = netCalories > targetCals + 100,
                        lastFoodTimestamp = foodLogs.maxOfOrNull {
                            (it.timestamp as? Number)?.toLong() ?: 0L
                        },
                        waterStatusMessage = waterStatus,
                        exerciseStatusMessage = exerciseStatus
                    )
                }

                if (isFoodAdditionPending && foodJustAddedEvent) {
                    viewModelScope.launch {
                        _foodJustAddedEvent.emit(Unit)
                    }
                    isFoodAdditionPending = false
                }

                if (isWaterAdditionPending && waterJustAddedEvent) {
                    val waterTarget = user.targetWaterMl
                    if (waterTarget in 1..currentWater) {
                        uz.oybek.ozamiz.data.community.AchievementPostHelper.onWaterGoalReached(
                            currentWater,
                            waterTarget
                        )
                    }
                    isWaterAdditionPending = false
                }

                if (isExerciseAdditionPending && exerciseJustAddedEvent) {
                    val exerciseTarget = AppConstants.Exercise.DEFAULT_TARGET_CALORIES
                    if (exerciseTarget > 0 && currentExerciseBurnt >= exerciseTarget) {
                        uz.oybek.ozamiz.data.community.AchievementPostHelper.onExerciseGoalReached(
                            currentExerciseBurnt,
                            exerciseTarget
                        )
                    }
                    isExerciseAdditionPending = false
                }

                previousWaterAmount = currentWater
                previousFoodCalories = currentFoodCalories
                previousExerciseBurnt = currentExerciseBurnt
                isFirstLoadCompleted = true
            }

            is DataWrapper.Error -> {
                _uiState.update { it.copy(isInitialLoading = false, isRefreshing = false) }
            }
        }
    }

    fun onFoodAdded() {
        isFoodAdditionPending = true
        StreakRepository.updateStreak { streakInfo ->
            viewModelScope.launch {
                _showStreakSheetEvent.emit(streakInfo)
            }
        }
        refresh()
    }

    fun consumeFoodAddedEvent() {
        _foodJustAddedEvent.resetReplayCache()
    }

    private fun calculateGlasses(current: Int, old: Int): List<WaterGlassState> {
        val totalGlassesNeeded =
            maxOf(AppConstants.Water.DEFAULT_GLASSES_COUNT, (current / glassVolume) + 1)
        return (0 until totalGlassesNeeded).map { i ->
            val threshold = (i + 1) * glassVolume
            val isFilled = current >= threshold
            val isNext = !isFilled && (current >= (i * glassVolume))
            WaterGlassState(
                id = i,
                isFilled = isFilled,
                isNextToAdd = isNext,
                shouldAnimateFill = false
            )
        }
    }

    fun onDateSelected(date: Date) {
        if (DateUtils.isSameDay(date, _uiState.value.selectedDate) && isFirstLoadCompleted) return
        previousWaterAmount = 0
        previousFoodCalories = 0
        isFirstLoadCompleted = false
        _uiState.update { it.copy(selectedDate = date, isInitialLoading = true) }
        startLoadingData(date)
    }

    fun refresh() {
        _uiState.update { it.copy(isRefreshing = true) }
        startLoadingData(_uiState.value.selectedDate)
    }

    fun addWater(amount: Int) {
        val date = uiState.value.selectedDate
        val pointsToAdd = if (amount >= 250) (amount * 5) / 250 else 5

        if (pointsToAdd > 0) {
            val currentPoints = _uiState.value.userPoints
            _uiState.update {
                it.copy(
                    userPoints = currentPoints + pointsToAdd,
                    pointsJustAdded = pointsToAdd
                )
            }
        }

        isWaterAdditionPending = true
        viewModelScope.launch {
            WaterLogRepository.add(WaterLog(amountMl = amount), date)
            StreakRepository.updateStreak { streakInfo ->
                viewModelScope.launch {
                    _showStreakSheetEvent.emit(streakInfo)
                }
            }
            if (pointsToAdd > 0) {
                UserRepository.addPoints(pointsToAdd)
            }
            refresh()
        }
    }

    fun onExerciseAdded() {
        isExerciseAdditionPending = true
        StreakRepository.updateStreak { streakInfo ->
            viewModelScope.launch {
                _showStreakSheetEvent.emit(streakInfo)
            }
        }
        refresh()
    }

    fun clearPointsJustAdded() = _uiState.update { it.copy(pointsJustAdded = 0) }
    fun clearWaterJustAdded() = _uiState.update { it.copy(waterJustAdded = false) }
    fun clearExerciseJustAdded() = _uiState.update { it.copy(exerciseJustAddedType = null) }

    private fun initDailyTip() {
        if (isTipInitialized) return
        viewModelScope.launch {
            val prefs = appContext.getSharedPreferences(
                "daily_tip_prefs",
                android.content.Context.MODE_PRIVATE
            )
            val lastDismissedDate = prefs.getString("last_dismissed_date", "")
            val isDismissedToday = lastDismissedDate == Date().dateApiFormat()
            var currentIndex = prefs.getInt("current_tip_index", -1)

            if (currentIndex == -1) {
                currentIndex = (0 until DailyTipUtil.tips.size).random()
                prefs.edit { putInt("current_tip_index", currentIndex) }
            }

            val tip = DailyTipUtil.getTip(currentIndex)
            _uiState.update { it.copy(dailyTip = tip, isTipVisible = !isDismissedToday) }
            isTipInitialized = true
        }
    }

    fun onHomeEntered() {
        val prefs =
            appContext.getSharedPreferences("daily_tip_prefs", android.content.Context.MODE_PRIVATE)
        val lastDismissedDate = prefs.getString("last_dismissed_date", "")
        val isDismissedToday = lastDismissedDate == Date().dateApiFormat()
        if (!isDismissedToday && !_uiState.value.isTipVisible) {
            _uiState.update { it.copy(isTipVisible = true) }
        }
        checkAppOnboarding()
    }

    private fun checkAppOnboarding() {
        val prefs =
            appContext.getSharedPreferences("app_prefs", android.content.Context.MODE_PRIVATE)
        val isFirst = prefs.getBoolean("is_first_registration", false)
        val step = prefs.getInt("app_onboarding_step", 0)
    }

    fun onAppOnboardingPresented() {
        val prefs =
            appContext.getSharedPreferences("app_prefs", android.content.Context.MODE_PRIVATE)
        prefs.edit().putInt("app_onboarding_step", 1).apply()
        _showAppOnboardingEvent.resetReplayCache()
    }

    fun onAppOnboardingComplete() {
        val prefs =
            appContext.getSharedPreferences("app_prefs", android.content.Context.MODE_PRIVATE)
        prefs.edit().putInt("app_onboarding_step", 2).apply()
    }

    fun onTipDismissed() {
        _uiState.update { it.copy(isTipVisible = false) }
        val prefs =
            appContext.getSharedPreferences("daily_tip_prefs", android.content.Context.MODE_PRIVATE)
        prefs.edit().putString("last_dismissed_date", Date().dateApiFormat()).apply()
        refreshTip()
    }

    fun onTipUnderstood() {
        refreshTip()
        _uiState.update { it.copy(isTipVisible = true) }
    }

    private fun refreshTip() {
        val prefs =
            appContext.getSharedPreferences("daily_tip_prefs", android.content.Context.MODE_PRIVATE)
        val currentIndex = prefs.getInt("current_tip_index", -1)
        val nextIndex = DailyTipUtil.getNewTipIndex(currentIndex)
        prefs.edit().putInt("current_tip_index", nextIndex).apply()
        val tip = DailyTipUtil.getTip(nextIndex)
        _uiState.update { it.copy(dailyTip = tip) }
    }

    private fun observeAiDietitianAdvice() {
        uz.oybek.ozamiz.data.ai.DailyAiAnalysisRepository.currentAdvice
            .onEach { advice ->
                _uiState.update { it.copy(dailyAiDietitianAdvice = advice) }
            }
            .launchIn(viewModelScope)
    }

    private fun observeAiMenu() {
        viewModelScope.launch {
            AiMenuRepository.currentMenu.collect { menu ->
                _uiState.update { it.copy(todayAiMenu = menu) }
            }
        }
    }


}
