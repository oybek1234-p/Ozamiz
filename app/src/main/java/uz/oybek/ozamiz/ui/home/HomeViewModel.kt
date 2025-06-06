package uz.oybek.ozamiz.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.asFlow
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import uz.oybek.ozamiz.data.DataWrapper
import uz.oybek.ozamiz.data.LocalUser
import uz.oybek.ozamiz.data.User
import uz.oybek.ozamiz.data.UserRepository
import uz.oybek.ozamiz.data.info.DailyLogData
import uz.oybek.ozamiz.data.info.ExerciseLog
import uz.oybek.ozamiz.data.info.FoodLog
import uz.oybek.ozamiz.data.info.MealsType
import uz.oybek.ozamiz.data.info.WaterLog
import uz.oybek.ozamiz.data.logs.DailyLogRepository
import uz.oybek.ozamiz.data.model.StreakInfo
import uz.oybek.ozamiz.data.utils.NutritionCalculator
import uz.oybek.ozamiz.reminders.ReminderScheduler
import uz.oybek.ozamiz.ui.getinfo.ExerciseType
import uz.oybek.ozamiz.ui.getinfo.ReminderItem
import uz.oybek.ozamiz.ui.getinfo.ReminderType
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.abs
import kotlin.math.min
import kotlin.math.roundToInt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class HomeViewModel : ViewModel() {

    private val dailyLogRepository = DailyLogRepository()

    private val _uiState = MutableStateFlow(HomeUiState(isLoading = true))
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch(Dispatchers.Default) {
            val initialUser = LocalUser.user
            if (initialUser.valid) {
                val processedState = processUserAndLogs(initialUser, _uiState.value.dailyLogData)
                _uiState.emit(processedState.copy(isLoading = false))
            } else {
                _uiState.emit(HomeUiState(isLoading = false, errorMessage = "Foydalanuvchi sozlanmagan"))
            }
        }
        observeUserUpdatesFromRepository()
        observeLogs()
    }

    private fun observeUserUpdatesFromRepository() {
        viewModelScope.launch {
            LocalUser.userLive.asFlow()
                .distinctUntilChanged()
                .collectLatest { user ->
                    val previousState = _uiState.value
                    if (user != null && user.valid) {
                        val processedState = withContext(Dispatchers.Default) {
                            processUserAndLogs(user, previousState.dailyLogData)
                        }
                        _uiState.value = processedState

                        if (previousState.user?.reminderSettings != user.reminderSettings) {
                            ReminderScheduler.scheduleAllActiveReminders(uz.oybek.ozamiz.appContext)
                        }
                    } else if (previousState.user != null) {
                        _uiState.value = HomeUiState(isLoading = false, errorMessage = "Foydalanuvchi tizimdan chiqdi")
                    }
                }
        }
    }


    private fun observeLogs() {
        viewModelScope.launch {
            dailyLogRepository.observeTodayLogs()
                .distinctUntilChanged()
                .collectLatest { result ->
                    when (result) {
                        is DataWrapper.Loading -> { /* Potentially set a specific loading flag if needed */ }
                        is DataWrapper.Success -> {
                            val logs = result.data
                            _uiState.update { currentState ->
                                val updatedState = withContext(Dispatchers.Default) {
                                    processUserAndLogs(currentState.user, logs)
                                }
                                updatedState.copy(
                                    errorMessage = null,
                                    isLoading = false
                                )
                            }
                        }
                        is DataWrapper.Error -> {
                            _uiState.update { it.copy(isLoading = false, errorMessage = result.error) }
                        }
                    }
                }
        }
    }

    private fun processUserAndLogs(user: User?, logs: DailyLogData?): HomeUiState {
        if (user == null || !user.valid) {
            return HomeUiState(isLoading = false, errorMessage = "Foydalanuvchi sozlanmagan")
        }

        var needsDbUpdate = false
        var newTargetCalories = user.targetCalories
        var newTargetWater = user.targetWaterMl
        var newTargetBurned = user.targetCaloriesBurned

        if (newTargetCalories <= 0) {
            newTargetCalories = NutritionCalculator.calculateTargetCalories(user)
            needsDbUpdate = true
        }
        if (newTargetWater <= 0) {
            newTargetWater = NutritionCalculator.calculateTargetWaterMl(user.weight)
            needsDbUpdate = true
        }
        if (newTargetBurned <= 0) {
            newTargetBurned = 300 // Default
            needsDbUpdate = true
        }

        // Ensure startWeight is set if a targetWeight exists but startWeight is null
        // This typically happens when a user sets a target for the first time
        // or if an old user logs in after the startWeight feature was added.
        var effectiveStartWeight = user.startWeight
        if (user.targetWeight != null && user.targetWeight!! < user.weight && user.startWeight == null) {
            effectiveStartWeight = user.weight // Set current weight as starting point for the goal
            needsDbUpdate = true
        }


        if (needsDbUpdate) {
            viewModelScope.launch { // Launch in viewModelScope for safety
                UserRepository.updateUser { dbUser ->
                    dbUser.targetCalories = newTargetCalories
                    dbUser.targetWaterMl = newTargetWater
                    dbUser.targetCaloriesBurned = newTargetBurned
                    if (dbUser.startWeight == null && effectiveStartWeight != null) {
                        dbUser.startWeight = effectiveStartWeight
                    }
                }
            }
        }

        val distribution = NutritionCalculator.distributeCaloriesForMeals(newTargetCalories)
        val remindersResult = processReminders(user)
        val streakInfo = user.streakInfo ?: StreakInfo()

        val foodLogs = logs?.foodLogs?.values?.toList() ?: emptyList()
        val waterLogs = logs?.waterLogs?.values?.toList() ?: emptyList()
        val exerciseLogsList = logs?.exerciseLogs?.values?.sortedByDescending { it.timestamp as? Long ?: 0L }?.toList() ?: emptyList()

        val consumedCalories = foodLogs.sumOf { it.calories }
        val consumedWaterMl = waterLogs.sumOf { it.amountMl }
        val burnedCalories = exerciseLogsList.sumOf { it.caloriesBurned }

        val mealConsumed = mutableMapOf<String, Int>()
        foodLogs.forEach { log ->
            mealConsumed[log.mealType] = mealConsumed.getOrDefault(log.mealType, 0) + log.calories
        }

        // Weight Goal Section Logic
        var showGoalSection = false
        val currentWeightDisp = user.weight
        var initialWeightGoalDisp = effectiveStartWeight ?: user.weight // Use effectiveStartWeight
        var targetWeightDisp = 0.0
        var weightLostDisp = 0.0
        var weightRemainingDisp = 0.0
        var goalProgressPerc = 0
        var goalStartDateDisp = user.registerDate?.time ?: System.currentTimeMillis()
        val estimatedTimelineStr = mutableListOf<String>()
        val activeDaysFromStreak = streakInfo.activeDays

        if (user.targetWeight != null && user.targetWeight!! < currentWeightDisp) { // Goal is to lose weight
            showGoalSection = true
            targetWeightDisp = user.targetWeight!!

            if (effectiveStartWeight != null && effectiveStartWeight > targetWeightDisp) {
                initialWeightGoalDisp = effectiveStartWeight // Actual start of the goal
                weightLostDisp = (effectiveStartWeight - currentWeightDisp).coerceAtLeast(0.0)
                val totalToLose = (effectiveStartWeight - targetWeightDisp).coerceAtLeast(0.01) // Avoid division by zero
                goalProgressPerc = ((weightLostDisp / totalToLose) * 100).roundToInt().coerceIn(0, 100)
            } else {
                // If startWeight isn't properly set for a loss goal, default to 0 progress from current
                initialWeightGoalDisp = currentWeightDisp
                weightLostDisp = 0.0
                goalProgressPerc = 0
            }
            weightRemainingDisp = (currentWeightDisp - targetWeightDisp).coerceAtLeast(0.0)


            if (weightRemainingDisp > 0) {
                val weeklyLossRate = 0.75 // kg per week (configurable)
                val w1 = min(weeklyLossRate, weightRemainingDisp)
                val w2 = min(weeklyLossRate * 2, weightRemainingDisp)
                val m1 = min(weeklyLossRate * 4, weightRemainingDisp)

                estimatedTimelineStr.add("1-hafta: ~${String.format(Locale.US, "%.1f", w1)} kg")
                estimatedTimelineStr.add("2-hafta: ~${String.format(Locale.US, "%.1f", w2)} kg gacha")
                estimatedTimelineStr.add("1 oyda: ~${String.format(Locale.US, "%.1f", m1)}-${String.format(Locale.US, "%.1f", min(weeklyLossRate * 5, weightRemainingDisp) )} kg gacha")
            } else {
                estimatedTimelineStr.add("Maqsadga erishdingiz!")
            }
        } else {
            // If no target, or target is not for losing, or startWeight is not logical
            showGoalSection = user.targetWeight != null // Show if target is set, even if not a loss goal for now
            initialWeightGoalDisp = currentWeightDisp
            targetWeightDisp = user.targetWeight ?: currentWeightDisp
            weightLostDisp = 0.0
            weightRemainingDisp = abs(currentWeightDisp - targetWeightDisp)
            goalProgressPerc = 0
            if (user.targetWeight != null && user.targetWeight!! >= currentWeightDisp) {
                estimatedTimelineStr.add("Maqsad vazn hozirgidan yuqori.")
            } else if (user.targetWeight == null) {
                estimatedTimelineStr.add("Maqsad vazn belgilanmagan.")
            }
        }


        return HomeUiState(
            isLoading = false, // Ensure isLoading is false after processing
            user = user,
            dailyLogData = logs,
            consumedCalories = consumedCalories,
            consumedWaterMl = consumedWaterMl,
            burnedCalories = burnedCalories,
            targetCalories = newTargetCalories,
            targetWaterMl = newTargetWater,
            targetCaloriesBurned = newTargetBurned,
            mealCalorieDistribution = distribution,
            mealConsumedCalories = mealConsumed,
            todayExerciseLogs = exerciseLogsList,
            enabledExerciseReminders = remindersResult.first,
            mealReminderTimes = remindersResult.second,
            streakInfo = streakInfo,
            // Weight Goal
            showWeightGoalSection = showGoalSection,
            currentWeightDisplay = currentWeightDisp,
            initialWeightForGoalDisplay = initialWeightGoalDisp,
            targetWeightDisplay = targetWeightDisp,
            weightLostDisplay = weightLostDisp,
            weightRemainingDisplay = weightRemainingDisp,
            goalProgressPercent = goalProgressPerc,
            goalStartDateDisplay = goalStartDateDisp,
            estimatedTimelineStrings = estimatedTimelineStr,
            activeDays = activeDaysFromStreak
        )
    }

    private fun processReminders(user: User?): Pair<List<ReminderItem>, Map<String, String>> {
        if (user?.reminderSettings == null) {
            return Pair(emptyList(), emptyMap())
        }
        val exerciseReminders = mutableListOf<ReminderItem>()
        val mealTimes = mutableMapOf<String, String>()

        user.reminderSettings?.forEach { (id, timeValue) ->
            val timeString = timeValue
            if (timeString != null) {
                val parts = timeString.split(":")
                if (parts.size == 2) {
                    val hour = parts[0].toIntOrNull()
                    val minute = parts[1].toIntOrNull()
                    if (hour != null && minute != null) {
                        val formattedTime = String.format(Locale.getDefault(), "%02d:%02d", hour, minute)
                        if (MealsType.mealTypes.any { it.id == id }) {
                            mealTimes[id] = formattedTime
                        } else {
                            val exerciseType = ExerciseType.getExerciseTypeById(id)
                            exerciseReminders.add(
                                ReminderItem(
                                    id = exerciseType.id, name = exerciseType.name, iconRes = exerciseType.iconRes,
                                    type = ReminderType.EXERCISE, description = exerciseType.description,
                                    imageUrl = exerciseType.defaultImageUrl, hour = hour, minute = minute
                                )
                            )
                        }
                    }
                }
            }
        }
        exerciseReminders.sortBy { (it.hour ?: 24) * 60 + (it.minute ?: 60) }
        return Pair(exerciseReminders, mealTimes)
    }

    suspend fun addWaterLog(amountMl: Int): DataWrapper<Unit> {
        if (amountMl <= 0) return DataWrapper.Error("Miqdor 0 dan katta bo'lishi kerak")
        val waterLog = WaterLog(amountMl = amountMl)
        return dailyLogRepository.addWaterLog(waterLog)
    }

    fun deleteFoodLog(log: FoodLog) {
        viewModelScope.launch {
            val result = dailyLogRepository.deleteFoodLog(log.logId)
            if (result is DataWrapper.Error) {
                _uiState.update { it.copy(errorMessage = "Ovqat logini o'chirishda xato: ${result.error}") }
            }
        }
    }

    fun deleteWaterLog(log: WaterLog) {
        viewModelScope.launch {
            val result = dailyLogRepository.deleteWaterLog(log.logId)
            if (result is DataWrapper.Error) {
                _uiState.update { it.copy(errorMessage = "Suv logini o'chirishda xato: ${result.error}") }
            }
        }
    }

    fun deleteExerciseLog(log: ExerciseLog) {
        viewModelScope.launch {
            val result = dailyLogRepository.deleteExerciseLog(log.logId)
            if (result is DataWrapper.Error) {
                _uiState.update { it.copy(errorMessage = "Mashq logini o'chirishda xato: ${result.error}") }
            }
        }
    }

    fun clearErrorMessage() {
        _uiState.update { it.copy(errorMessage = null) }
    }
}