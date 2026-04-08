package uz.oybek.ozamiz.ui.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import uz.oybek.ozamiz.AppConstants
import uz.oybek.ozamiz.appContext
import uz.oybek.ozamiz.data.food.DietController
import uz.oybek.ozamiz.data.food.model.MealTime
import uz.oybek.ozamiz.data.logs.DailyLogRepository
import uz.oybek.ozamiz.data.logs.DailySummaryRepository
import uz.oybek.ozamiz.data.logs.WaterLogRepository
import uz.oybek.ozamiz.data.logs.model.DailySummary
import uz.oybek.ozamiz.data.logs.model.UserExercise
import uz.oybek.ozamiz.data.logs.model.WaterLog
import uz.oybek.ozamiz.data.menu.MenuRepository
import uz.oybek.ozamiz.data.streak.StreakRepository
import uz.oybek.ozamiz.data.user.UserRepository
import uz.oybek.ozamiz.data.user.local.user
import uz.oybek.ozamiz.data.user.model.User
import uz.oybek.ozamiz.data.utils.DataWrapper
import uz.oybek.ozamiz.data.utils.dateApiFormat
import androidx.core.content.edit
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

    private val _showPremiumEvent = kotlinx.coroutines.flow.MutableSharedFlow<Unit>()
    val showPremiumEvent = _showPremiumEvent.asSharedFlow()

    private val glassVolume = AppConstants.Water.GLASS_VOLUME_ML
    private var previousWaterAmount = 0
    private var previousFoodCalories = 0
    private var previousPoints = -1
    private var isFirstLoadCompleted = false
    private var recommendationJob: Job? = null
    private var isTipInitialized = false

    init {
        _uiState.update {
            it.copy(
                datesForScroller = DateUtils.generateDatesForScroller(),
                selectedDate = Date()
            )
        }
        initDailyTip()
        viewModelScope.launch {
            try {
                StreakRepository.updateStreak()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        UserRepository.observeCurrentUser { user ->
            if (user != null && user.valid) {
                val newPoints = user.scorePoints
                val added =
                    if (previousPoints >= 0 && newPoints != previousPoints) newPoints - previousPoints else 0
                previousPoints = newPoints
                _uiState.update {
                    it.copy(
                        streakInfo = user.streakInfo,
                        userPoints = newPoints,
                        pointsJustAdded = added
                    )
                }
                val regDate = user.registerDate
                if (regDate != null) {
                    val daysPassed =
                        (System.currentTimeMillis() - regDate.time) / (1000 * 60 * 60 * 24)
                    val prefs = appContext.getSharedPreferences(
                        "app_prefs",
                        android.content.Context.MODE_PRIVATE
                    )

                    if (daysPassed >= 1 && !user.premium && !user.paymentChecking) {
                        val hasShownPremium = prefs.getBoolean("has_shown_premium_sheet", false)
                        if (!hasShownPremium) {
                            viewModelScope.launch {
                                _showPremiumEvent.emit(Unit)
                                prefs.edit().putBoolean("has_shown_premium_sheet", true).apply()
                            }
                        }
                    }

                    if (daysPassed >= 3) {
                        val hasShownRate = prefs.getBoolean("has_shown_rate_sheet", false)
                        if (!hasShownRate) {
                            viewModelScope.launch {
                                _showRateAppEvent.emit(Unit)
                            }
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

        uz.oybek.ozamiz.data.course.LocalCourseRepository.levelsFlow.onEach { levels ->
            _uiState.update { it.copy(courseLevels = levels) }
        }.launchIn(viewModelScope)

        uz.oybek.ozamiz.data.course.LocalCourseRepository.activeLessonFlow.onEach { lesson ->
            _uiState.update { it.copy(activeLesson = lesson) }
        }.launchIn(viewModelScope)
    }

    private fun loadData(date: Date) {
        val summaryFlow = DailyLogRepository.observeDailySummary(date)
        val exerciseFlow = uz.oybek.ozamiz.data.logs.ExerciseLogRepository.getLogs(date)
        val foodFlow = uz.oybek.ozamiz.data.logs.FoodLogRepository.getAllLogs(date)

        kotlinx.coroutines.flow.combine(
            summaryFlow,
            exerciseFlow,
            foodFlow
        ) { summaryWrapper, exercises, foods ->
            Triple(summaryWrapper, exercises, foods)
        }.map { (summaryWrapper, exercises, foods) ->
            processStateUpdate(
                date = date,
                user = user,
                summaryWrapper = summaryWrapper,
                exerciseLogs = exercises,
                foodLogs = foods
            )
        }.launchIn(viewModelScope)
    }

    private fun processStateUpdate(
        date: Date,
        user: User?,
        summaryWrapper: DataWrapper<DailySummary>,
        exerciseLogs: List<uz.oybek.ozamiz.data.logs.model.ExerciseLog>,
        foodLogs: List<uz.oybek.ozamiz.data.logs.model.FoodLog> = emptyList()
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
                val waterJustAddedEvent =
                    if (isFirstLoadCompleted && currentWater > previousWaterAmount) true else false

                val currentFoodCalories = summary.consumedCalories
                val foodJustAddedEvent =
                    if (isFirstLoadCompleted && currentFoodCalories > previousFoodCalories) true else false

                val sortedExerciseLogs = exerciseLogs
                    .sortedByDescending { it.timestamp as? Long ?: 0L }

                val currentExerciseBurnt = summary.burnedCalories
                val exerciseJustAddedEvent =
                    if (isFirstLoadCompleted && currentExerciseBurnt > _uiState.value.exerciseBurnt) {
                        sortedExerciseLogs.firstOrNull()?.exerciseType
                    } else null

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
                        waterJustAdded = it.waterJustAdded || waterJustAddedEvent,
                        foodJustAdded = it.foodJustAdded || foodJustAddedEvent,
                        exerciseJustAddedType = it.exerciseJustAddedType ?: exerciseJustAddedEvent,
                        exerciseBurnt = summary.burnedCalories,
                        exerciseTarget = AppConstants.Exercise.DEFAULT_TARGET_CALORIES,
                        carbsConsumed = summary.consumedCarbs.roundToInt(),
                        carbsTarget = macros.carbs,
                        proteinConsumed = summary.consumedProtein.roundToInt(),
                        proteinTarget = macros.protein,
                        fatConsumed = summary.consumedFat.roundToInt(),
                        fatTarget = macros.fat,
                        exerciseLogs = sortedExerciseLogs,
                        dailyFoodLogs = foodLogs,
                        recommendedExercises =
                            if (sortedExerciseLogs.isEmpty()) {
                                UserExercise.getExercisesList(user.gender)
                                    .take(4)
                            } else {
                                emptyList()
                            },
                        isProteinExceeded = summary.consumedProtein > macros.protein + 20,
                        isCarbsExceeded = summary.consumedCarbs > macros.carbs + 20,
                        isFatExceeded = summary.consumedFat > macros.fat + 20,
                        isCalorieOptimal = summary.consumedCalories >= user.targetCalories && summary.consumedCalories <= user.targetCalories + 200,
                        isCalorieExceeded = summary.consumedCalories > user.targetCalories + 200
                    )
                }
                previousWaterAmount = currentWater
                previousFoodCalories = currentFoodCalories
                isFirstLoadCompleted = true
                updateFoodRecommendations(foodLogs, date)
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
        isFirstLoadCompleted = false
        loadData(date)
    }

    fun refresh() {
        _uiState.update { it.copy(isRefreshing = true) }
        loadData(uiState.value.selectedDate)
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

        viewModelScope.launch {
            WaterLogRepository.add(WaterLog(amountMl = amount), date)
            uz.oybek.ozamiz.data.streak.StreakRepository.updateStreak()
            if (pointsToAdd > 0) {
                UserRepository.addPoints(pointsToAdd)
            }
        }
    }

    fun clearPointsJustAdded() {
        _uiState.update { it.copy(pointsJustAdded = 0) }
    }

    fun clearWaterJustAdded() {
        _uiState.update { it.copy(waterJustAdded = false) }
    }

    fun clearFoodJustAdded() {
        _uiState.update { it.copy(foodJustAdded = false) }
    }

    fun clearExerciseJustAdded() {
        _uiState.update { it.copy(exerciseJustAddedType = null) }
    }

    private fun updateFoodRecommendations(
        foodLogs: List<uz.oybek.ozamiz.data.logs.model.FoodLog>,
        date: Date
    ) {
        if (!DateUtils.isSameDay(date, Date())) {
            _uiState.update { it.copy(recommendedFoods = emptyList(), recommendingForMeal = null) }
            return
        }

        if (_uiState.value.isCalorieOptimal || _uiState.value.isCalorieExceeded) {
            _uiState.update { it.copy(recommendedFoods = emptyList(), recommendingForMeal = null) }
            return
        }

        val breakfastLogged = foodLogs.any { it.mealType == MealTime.Breakfast.id }
        val lunchLogged = foodLogs.any { it.mealType == MealTime.Lunch.id }
        val dinnerLogged = foodLogs.any { it.mealType == MealTime.Dinner.id }

        val nextMealId = when {
            !breakfastLogged -> MealTime.Breakfast.id
            !lunchLogged -> MealTime.Lunch.id
            !dinnerLogged -> MealTime.Dinner.id
            else -> MealTime.Snack.id
        }

        val mealName = when (nextMealId) {
            MealTime.Breakfast.id -> "Nonushta"
            MealTime.Lunch.id -> "Tushlik"
            MealTime.Dinner.id -> "Kechki ovqat"
            else -> "Tamaddi"
        }

        if (uiState.value.recommendingForMeal == mealName && uiState.value.recommendedFoods.isNotEmpty()) return

        recommendationJob?.cancel()
        recommendationJob = viewModelScope.launch {
            val categories = MenuRepository.getCategories().firstOrNull() ?: emptyList()
            val category = categories.find { it.name.contains(mealName, ignoreCase = true) }
            val categoryId = category?.id

            if (categoryId != null) {
                val allMenus = MenuRepository.getMenus(categoryId).firstOrNull()?.sortedBy { it.id }
                    ?: emptyList()
                if (allMenus.isEmpty()) return@launch

                val prefs = appContext.getSharedPreferences(
                    "recommendation_prefs",
                    android.content.Context.MODE_PRIVATE
                )
                val prefKey = "last_index_$nextMealId"
                val lastIndex = prefs.getInt(prefKey, -1)

                val nextStartIndex = (lastIndex + 1) % allMenus.size

                val recommended = mutableListOf<uz.oybek.ozamiz.data.menu.model.Menu>()
                for (i in 0 until 3) {
                    val currentIndex = (nextStartIndex + i) % allMenus.size
                    recommended.add(allMenus[currentIndex])
                    if (allMenus.size < 3 && recommended.size == allMenus.size) break
                }

                val newLastIndex = (nextStartIndex + recommended.size - 1) % allMenus.size
                prefs.edit().putInt(prefKey, newLastIndex).apply()

                _uiState.update {
                    it.copy(
                        recommendedFoods = recommended,
                        recommendingForMeal = mealName
                    )
                }
            }
        }
    }

    private fun initDailyTip() {
        if (isTipInitialized) return
        viewModelScope.launch {
            val prefs =
                appContext.getSharedPreferences("daily_tip_prefs", android.content.Context.MODE_PRIVATE)

            val lastDismissedDate = prefs.getString("last_dismissed_date", "")
            val isDismissedToday = lastDismissedDate == Date().dateApiFormat()

            var currentIndex = prefs.getInt("current_tip_index", -1)

            if (currentIndex == -1) {
                currentIndex = (0 until uz.oybek.ozamiz.ui.utils.DailyTipUtil.tips.size).random()
                prefs.edit { putInt("current_tip_index", currentIndex) }
            }

            val tip = uz.oybek.ozamiz.ui.utils.DailyTipUtil.getTip(currentIndex)
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
        val nextIndex = uz.oybek.ozamiz.ui.utils.DailyTipUtil.getNewTipIndex(currentIndex)

        prefs.edit().putInt("current_tip_index", nextIndex).apply()
        val tip = uz.oybek.ozamiz.ui.utils.DailyTipUtil.getTip(nextIndex)
        _uiState.update { it.copy(dailyTip = tip) }
    }
}
