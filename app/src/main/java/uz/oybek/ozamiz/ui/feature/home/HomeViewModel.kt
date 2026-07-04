package uz.oybek.ozamiz.ui.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
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
import uz.oybek.ozamiz.data.ai.AiMenuRepository
import uz.oybek.ozamiz.data.ai.Dietolog
import uz.oybek.ozamiz.data.config.LocalPreferencesRepository
import uz.oybek.ozamiz.data.food.DietController
import uz.oybek.ozamiz.data.logs.DailyLogRepository
import uz.oybek.ozamiz.data.logs.DailySummaryRepository
import uz.oybek.ozamiz.data.logs.ExerciseLogRepository
import uz.oybek.ozamiz.data.logs.FoodLogRepository
import uz.oybek.ozamiz.data.logs.WaterLogRepository
import uz.oybek.ozamiz.data.logs.WeightLogRepository
import uz.oybek.ozamiz.data.logs.model.DailyLogs
import uz.oybek.ozamiz.data.logs.model.DailySummary
import uz.oybek.ozamiz.data.logs.model.ExerciseLog
import uz.oybek.ozamiz.data.logs.model.FoodLog
import uz.oybek.ozamiz.data.logs.model.UserExercise
import uz.oybek.ozamiz.data.logs.model.WaterLog
import uz.oybek.ozamiz.data.streak.StreakInfo
import uz.oybek.ozamiz.data.streak.StreakRepository
import uz.oybek.ozamiz.data.user.UserRepository
import uz.oybek.ozamiz.data.user.local.user
import uz.oybek.ozamiz.data.user.model.User
import uz.oybek.ozamiz.data.utils.DataWrapper
import uz.oybek.ozamiz.data.utils.DateUtils
import uz.oybek.ozamiz.data.utils.dateApiFormat
import uz.oybek.ozamiz.ui.feature.premium.isPremiumOrTrial
import uz.oybek.ozamiz.ui.utils.DailyTipUtil
import java.util.Date
import kotlin.math.roundToInt

class HomeViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(
        HomeUiState(isInitialLoading = false)
    )
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private val _showRateAppEvent = kotlinx.coroutines.flow.MutableSharedFlow<Unit>()
    val showRateAppEvent = _showRateAppEvent.asSharedFlow()

    private val _showStreakSheetEvent = kotlinx.coroutines.flow.MutableSharedFlow<Pair<StreakInfo, Boolean>>()
    val showStreakSheetEvent = _showStreakSheetEvent.asSharedFlow()

    private val _showWaterLimitEvent = kotlinx.coroutines.flow.MutableSharedFlow<Unit>()
    val showWaterLimitEvent = _showWaterLimitEvent.asSharedFlow()

    private val _showWeightLogEvent = kotlinx.coroutines.flow.MutableSharedFlow<Unit>()
    val showWeightLogEvent = _showWeightLogEvent.asSharedFlow()

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
    private var lastRecGateKey: String? = null
    private var cachedRecommendedExercises: List<UserExercise> = emptyList()

    private var summaryJob: Job? = null

    init {
        observeAiMenu()
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

            val pTarget =
                if (initialUser.targetProtein > 0) initialUser.targetProtein else macros.protein
            val fTarget = if (initialUser.targetFat > 0) initialUser.targetFat else macros.fat
            val cTarget = if (initialUser.targetCarbs > 0) initialUser.targetCarbs else macros.carbs

            cachedRecommendedExercises = UserExercise.getExercisesList(initialUser.gender).take(4)

            val todayStr = DateUtils.getFormattedDateString(initialSelectedDate, "yyyy-MM-dd")
            val cache = LocalPreferencesRepository.getTodayCache(todayStr)

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
                    caloriesRemaining = if (cache != null) targetCals - cache.calories else targetCals,
                    caloriesConsumed = cache?.calories ?: 0,
                    waterConsumed = cache?.water ?: 0,
                    exerciseBurnt = cache?.exercise ?: 0,
                    proteinConsumed = cache?.protein ?: 0,
                    fatConsumed = cache?.fat ?: 0,
                    carbsConsumed = cache?.carbs ?: 0,
                    userPoints = initialUser.scorePoints,
                    streakInfo = initialUser.streakInfo,
                    paymentChecking = initialUser.paymentChecking
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

                cachedRecommendedExercises = UserExercise.getExercisesList(user.gender).take(4)

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
                        paymentChecking = user.paymentChecking,
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
        loadAiMealRecommendation(_uiState.value.selectedDate)
        checkWeightLog()
    }

    private fun checkWeightLog() {
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

    private fun loadAiMealRecommendation(date: Date) {
        if (!DateUtils.isSameDay(date, Date())) {
            _uiState.update {
                it.copy(
                    aiMealRecommendation = null,
                    nextMealTypeName = null
                )
            }
            return
        }

        val hour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)
        val mealTimeId = when {
            hour < 10 -> "breakfast"
            hour < 14 -> "lunch"
            else -> "dinner"
        }
        val targetCals = _uiState.value.caloriesTarget
        val netCalories = _uiState.value.caloriesConsumed
        val isExceeded = targetCals > 0 && netCalories >= targetCals
        val todayStr = DateUtils.getFormattedDateString(Date(), "yyyy-MM-dd")
        val gateKey = "${todayStr}_${mealTimeId}_exceeded_${isExceeded}"

        val dismissedKey = LocalPreferencesRepository.getDismissedMealRecommendation()
        val isDismissed = dismissedKey == "${todayStr}_${mealTimeId}"

        if (isDismissed) {
            _uiState.update {
                it.copy(
                    isAiMealDismissed = true,
                    isAiMealLoading = false
                )
            }
        }

        if (lastRecGateKey == gateKey && _uiState.value.aiMealRecommendation != null) {
            return
        }
        lastRecGateKey = gateKey

        val isFirstLoad = _uiState.value.aiMealRecommendation == null
        if (isFirstLoad && !isDismissed) {
            _uiState.update { it.copy(isAiMealLoading = true) }
        }
        viewModelScope.launch {
            val result = Dietolog.getMealRecommendations()
            if (result != null) {
                val recommendedMealName = result.first
                val recommendation = result.second

                val currentRec = _uiState.value.aiMealRecommendation
                val currentTitle = _uiState.value.nextMealTypeName

                if ((isDismissed || _uiState.value.isAiMealDismissed) && currentTitle == recommendedMealName) {
                    _uiState.update { it.copy(isAiMealLoading = false) }
                    return@launch
                }

                if (currentRec != null && currentTitle == recommendedMealName) {
                    _uiState.update { it.copy(isAiMealLoading = false) }
                    return@launch
                }

                _uiState.update {
                    it.copy(
                        aiMealRecommendation = recommendation,
                        nextMealTypeName = recommendedMealName,
                        isAiMealLoading = false,
                        isAiMealDismissed = if (isDismissed) true else (if (currentTitle != recommendedMealName) false else it.isAiMealDismissed)
                    )
                }
            } else {
                _uiState.update { it.copy(isAiMealLoading = false) }
            }
        }
    }

    fun onDismissAiRecommendations() {
        val todayStr = DateUtils.getFormattedDateString(Date(), "yyyy-MM-dd")
        val hour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)
        val mealTimeId = when {
            hour < 10 -> "breakfast"
            hour < 14 -> "lunch"
            else -> "dinner"
        }
        LocalPreferencesRepository.setDismissedMealRecommendation("${todayStr}_${mealTimeId}")
        _uiState.update { it.copy(isAiMealDismissed = true) }
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
            try {
                val summaryFlow = DailyLogRepository.observeDailySummary(date)
                val exerciseFlow = ExerciseLogRepository.getLogs(date)
                val foodFlow = FoodLogRepository.getAllLogs(date)

                // Load initial data for all streams at once in parallel
                val summaryDeferred = async { summaryFlow.first() }
                val exerciseDeferred = async { exerciseFlow.first() }
                val foodDeferred = async { foodFlow.first() }

                val summaryWrapper = summaryDeferred.await()
                val exercises = exerciseDeferred.await()
                val foods = foodDeferred.await()

                timeoutJob.cancel()
                processStateUpdate(date, user, summaryWrapper, exercises, foods)

                // Subscribe to future database updates, skipping the duplicate first emission
                var skipCount = 0
                combine(
                    summaryFlow,
                    exerciseFlow,
                    foodFlow
                ) { s, e, f -> Triple(s, e, f) }
                    .collect { (s, e, f) ->
                        if (skipCount < 1) {
                            skipCount++
                        } else {
                            processStateUpdate(date, user, s, e, f)
                        }
                    }
            } catch (e: Exception) {
                e.printStackTrace()
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
                val targetCals =
                    if (user.targetCalories > 0) user.targetCalories else DietController.calculateTargetCalories(
                        user
                    )
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
                        recommendedExercises = if (exerciseLogs.isEmpty()) cachedRecommendedExercises else emptyList(),
                        isProteinExceeded = summary.consumedProtein > macros.protein + 50,
                        isCarbsExceeded = summary.consumedCarbs > macros.carbs + 70,
                        isFatExceeded = summary.consumedFat > macros.fat + 30,
                        isCalorieOptimal = netCalories >= targetCals - 50 && netCalories <= targetCals + 100,
                        isCalorieExceeded = netCalories > targetCals + 100,
                        lastFoodTimestamp = foodLogs.maxOfOrNull {
                            (it.timestamp as? Number)?.toLong() ?: 0L
                        },
                        waterStatusMessage = waterStatus,
                        exerciseStatusMessage = exerciseStatus,
                        isDietologAdviceDismissed = if (foodJustAddedEvent) false else it.isDietologAdviceDismissed,
                        isAiMealDismissed = if (foodJustAddedEvent) false else it.isAiMealDismissed
                    )
                }
                if (DateUtils.isSameDay(date, Date())) {
                    val todayStr = DateUtils.getFormattedDateString(Date(), "yyyy-MM-dd")
                    LocalPreferencesRepository.saveTodayCache(
                        dateStr = todayStr,
                        calories = currentFoodCalories,
                        water = currentWater,
                        exercise = currentExerciseBurnt,
                        protein = summary.consumedProtein.roundToInt(),
                        fat = summary.consumedFat.roundToInt(),
                        carbs = summary.consumedCarbs.roundToInt()
                    )
                }

                if (isWaterAdditionPending && waterJustAddedEvent) {
                    isWaterAdditionPending = false
                }

                if (isExerciseAdditionPending && exerciseJustAddedEvent) {
                    isExerciseAdditionPending = false
                }

                previousWaterAmount = currentWater
                previousFoodCalories = currentFoodCalories
                previousExerciseBurnt = currentExerciseBurnt
                isFirstLoadCompleted = true

                loadAiMealRecommendation(date)
            }

            is DataWrapper.Error -> {
                _uiState.update { it.copy(isInitialLoading = false, isRefreshing = false) }
            }
        }
    }


    fun onFoodAdded() {
        isFoodAdditionPending = true
        LocalPreferencesRepository.setFirstLogAdded(true)
        triggerStreakUpdateIfNeeded()
        refresh(showIndicator = false)
    }

    private fun triggerStreakUpdateIfNeeded() {
        StreakRepository.updateStreak { streakInfo, returnedAfterManyDays ->
            viewModelScope.launch {
                _showStreakSheetEvent.emit(Pair(streakInfo, returnedAfterManyDays))
            }
        }
    }

    suspend fun getTodayWaterLogsCountSync(): Int {
        val dailyLogsWrapper = DailyLogRepository.observeDailyLogs(_uiState.value.selectedDate).first()
        val dailyLogs = (dailyLogsWrapper as? DataWrapper.Success)?.data ?: DailyLogs()
        return dailyLogs.waterLogs?.size ?: 0
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
        _uiState.update { it.copy(selectedDate = date, isInitialLoading = false) }
        startLoadingData(date)
        loadAiMealRecommendation(date)
    }

    fun refresh(showIndicator: Boolean = true) {
        if (showIndicator) {
            _uiState.update { it.copy(isRefreshing = true) }
        }
        startLoadingData(_uiState.value.selectedDate)
    }

    fun addWater(amount: Int) {
        val currentWater = _uiState.value.waterConsumed
        if (currentWater + amount > 4000) {
            return
        }

        viewModelScope.launch {
            if (!isUserPremium && getTodayWaterLogsCountSync() >= 5) {
                _showWaterLimitEvent.emit(Unit)
                return@launch
            }

            val pointsToAdd = if (amount >= 250) (amount * 5) / 250 else 5

            _uiState.update { 
                it.copy(
                    waterConsumed = currentWater + amount,
                    isWaterAdding = true
                ) 
            }
            if (DateUtils.isSameDay(uiState.value.selectedDate, Date())) {
                val todayStr = DateUtils.getFormattedDateString(Date(), "yyyy-MM-dd")
                val oldCache = LocalPreferencesRepository.getTodayCache(todayStr)
                if (oldCache != null) {
                    LocalPreferencesRepository.saveTodayCache(
                        dateStr = todayStr,
                        calories = oldCache.calories,
                        water = currentWater + amount,
                        exercise = oldCache.exercise,
                        protein = oldCache.protein,
                        fat = oldCache.fat,
                        carbs = oldCache.carbs
                    )
                }
            }

            delay(1500)

            val date = uiState.value.selectedDate
            WaterLogRepository.add(WaterLog(amountMl = amount), date)
            triggerStreakUpdateIfNeeded()
            if (pointsToAdd > 0) {
                UserRepository.addPoints(pointsToAdd)
            }
            if (pointsToAdd > 0) {
                val currentPoints = _uiState.value.userPoints
                _uiState.update {
                    it.copy(
                        userPoints = currentPoints + pointsToAdd,
                        pointsJustAdded = pointsToAdd
                    )
                }
            }
            refresh(showIndicator = false)
            _uiState.update { it.copy(isWaterAdding = false) }
        }
    }

    fun onExerciseAdded() {
        isExerciseAdditionPending = true
        triggerStreakUpdateIfNeeded()
        refresh(showIndicator = false)
    }

    // Local Preferences Abstraction
    fun isHomeTourSeen() = LocalPreferencesRepository.isHomeTourSeen()
    fun setHomeTourSeen(seen: Boolean) = LocalPreferencesRepository.setHomeTourSeen(seen)
    fun getPersistentDate(key: String) = LocalPreferencesRepository.getPersistentDateStr(key)
    fun savePersistentDate(key: String, date: String) =
        LocalPreferencesRepository.savePersistentDateStr(key, date)

    val isUserPremium: Boolean
        get() = UserRepository.user.isPremiumOrTrial()

    fun clearPointsJustAdded() = _uiState.update { it.copy(pointsJustAdded = 0) }
    fun clearWaterJustAdded() = _uiState.update { it.copy(waterJustAdded = false) }
    fun clearExerciseJustAdded() = _uiState.update { it.copy(exerciseJustAddedType = null) }

    private fun initDailyTip() {
        if (isTipInitialized) return
        viewModelScope.launch {
            val lastDismissedDate = LocalPreferencesRepository.getLastDismissedDate()
            val isDismissedToday = lastDismissedDate == Date().dateApiFormat()
            var currentIndex = LocalPreferencesRepository.getCurrentTipIndex()

            if (currentIndex == -1) {
                currentIndex = (0 until DailyTipUtil.tips.size).random()
                LocalPreferencesRepository.setCurrentTipIndex(currentIndex)
            }

            val tip = DailyTipUtil.getTip(currentIndex)
            _uiState.update { it.copy(dailyTip = tip, isTipVisible = !isDismissedToday) }
            isTipInitialized = true
        }
    }

    fun onHomeEntered() {
        viewModelScope.launch {
            Dietolog.checkAppOpenedAdvice()
            loadAiMealRecommendation(_uiState.value.selectedDate)
        }
    }

    private fun refreshTip() {
        val currentIndex = LocalPreferencesRepository.getCurrentTipIndex()
        val nextIndex = DailyTipUtil.getNewTipIndex(currentIndex)
        LocalPreferencesRepository.setCurrentTipIndex(nextIndex)
        val tip = DailyTipUtil.getTip(nextIndex)
        _uiState.update { it.copy(dailyTip = tip) }
    }

    fun onDismissDietologAdvice() {
        _uiState.update { it.copy(isDietologAdviceDismissed = true) }
    }

    private fun observeAiDietitianAdvice() {
        Dietolog.latestMessage
            .onEach { advice ->
                _uiState.update {
                    it.copy(
                        dailyAiDietitianAdvice = advice,
                        isDietologAdviceDismissed = false
                    )
                }
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
