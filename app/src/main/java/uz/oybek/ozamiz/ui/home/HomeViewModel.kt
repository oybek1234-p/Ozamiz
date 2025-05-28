package uz.oybek.ozamiz.ui.home

import androidx.lifecycle.ViewModel
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
import uz.oybek.ozamiz.data.utils.NutritionCalculator
import uz.oybek.ozamiz.reminders.ReminderScheduler
import uz.oybek.ozamiz.ui.getinfo.ExerciseType
import uz.oybek.ozamiz.ui.getinfo.ReminderItem
import uz.oybek.ozamiz.ui.getinfo.ReminderType
import java.util.Locale

class HomeViewModel : ViewModel() {

    private val dailyLogRepository = DailyLogRepository()

    private val _uiState = MutableStateFlow(HomeUiState(isLoading = false))
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        val initialUser = LocalUser.user
        if (initialUser.valid) {
            val initialTargetCalories = initialUser.targetCalories.takeIf { it > 0 }
                ?: NutritionCalculator.calculateTargetCalories(initialUser)
            val initialTargetWater = initialUser.targetWaterMl.takeIf { it > 0 }
                ?: NutritionCalculator.calculateTargetWaterMl(initialUser.weight)
            val initialTargetBurned = initialUser.targetCaloriesBurned.takeIf { it > 0 } ?: 300
            val initialDistribution = NutritionCalculator.distributeCaloriesForMeals(initialTargetCalories)
            val initialReminders = processReminders(initialUser)

            _uiState.update {
                it.copy(
                    user = initialUser,
                    targetCalories = initialTargetCalories,
                    targetWaterMl = initialTargetWater,
                    targetCaloriesBurned = initialTargetBurned,
                    mealCalorieDistribution = initialDistribution,
                    enabledExerciseReminders = initialReminders.first,
                    mealReminderTimes = initialReminders.second,
                    isLoading = false // Ensure loading is false
                )
            }
            recalculateTargetsIfNeeded(initialUser)
        } else {
            _uiState.update { it.copy(isLoading = false, errorMessage = "Foydalanuvchi sozlanmagan") }
        }

        observeUserUpdatesFromRepository()
        observeLogs()
    }

    private fun observeUserUpdatesFromRepository() {
        viewModelScope.launch {
            LocalUser.userLive.observeForever { user ->
                val previousState = _uiState.value
                if (user != null && user.valid) {
                    if (user != previousState.user) { // Process only if user object changed
                        val needsRecalc = previousState.user?.let { oldUser ->
                            user.gender != oldUser.gender || user.age != oldUser.age || user.height != oldUser.height ||
                                    user.weight != oldUser.weight || user.activityLevel != oldUser.activityLevel ||
                                     user.targetCalories <= 0 ||
                                    user.targetWaterMl <= 0 || user.targetCaloriesBurned <= 0
                        } ?: true

                        val settingsChanged = previousState.user?.reminderSettings != user.reminderSettings

                        var newState = previousState.copy(user = user)

                        if (needsRecalc) {
                            val newTargetCalories = NutritionCalculator.calculateTargetCalories(user)
                            val newTargetWater = NutritionCalculator.calculateTargetWaterMl(user.weight)
                            val newTargetBurned = user.targetCaloriesBurned.takeIf { it > 0 } ?: 300
                            val newDistribution = NutritionCalculator.distributeCaloriesForMeals(newTargetCalories)
                            newState = newState.copy(
                                targetCalories = newTargetCalories,
                                targetWaterMl = newTargetWater,
                                targetCaloriesBurned = newTargetBurned,
                                mealCalorieDistribution = newDistribution
                            )
                            if(user.targetCalories != newTargetCalories || user.targetWaterMl != newTargetWater || user.targetCaloriesBurned != newTargetBurned) {
                                viewModelScope.launch {
                                    UserRepository.updateUser { dbUser ->
                                        dbUser.targetCalories = newTargetCalories
                                        dbUser.targetWaterMl = newTargetWater
                                        dbUser.targetCaloriesBurned = newTargetBurned
                                    }
                                }
                            }
                        }

                        if (settingsChanged) {
                            val remindersResult = processReminders(user)
                            newState = newState.copy(
                                enabledExerciseReminders = remindersResult.first,
                                mealReminderTimes = remindersResult.second
                            )
                            // Trigger re-scheduling of alarms since reminder settings changed
                            ReminderScheduler.scheduleAllActiveReminders(uz.oybek.ozamiz.appContext)
                        }

                        newState = calculateProgress(user, newState.dailyLogData)
                        _uiState.value = newState
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
                .collectLatest { result ->
                    when (result) {
                        is DataWrapper.Loading -> { /* Ignore */ }
                        is DataWrapper.Success -> {
                            val logs = result.data
                            _uiState.update { currentState ->
                                calculateProgress(currentState.user, logs).copy(
                                    errorMessage = null,
                                    dailyLogData = logs,
                                    todayExerciseLogs = logs.exerciseLogs?.values
                                        ?.sortedByDescending { it.timestamp as? Long ?: 0L }
                                        ?.toList() ?: emptyList()
                                )
                            }
                        }
                        is DataWrapper.Error -> {
                            _uiState.update { it.copy(errorMessage = result.error) }
                        }
                    }
                }
        }
    }

    private fun recalculateTargetsIfNeeded(user: User?) {
        if (user == null || !user.valid) return

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
            newTargetBurned = 300
            needsDbUpdate = true
        }

        if (needsDbUpdate) {
            viewModelScope.launch {
                UserRepository.updateUser { dbUser ->
                    dbUser.targetCalories = newTargetCalories
                    dbUser.targetWaterMl = newTargetWater
                    dbUser.targetCaloriesBurned = newTargetBurned
                }
            }
        }

        val distribution = NutritionCalculator.distributeCaloriesForMeals(newTargetCalories)
        _uiState.update { currentState ->
            if (currentState.targetCalories != newTargetCalories ||
                currentState.targetWaterMl != newTargetWater ||
                currentState.targetCaloriesBurned != newTargetBurned ||
                currentState.mealCalorieDistribution != distribution) {
                currentState.copy(
                    targetCalories = newTargetCalories,
                    targetWaterMl = newTargetWater,
                    targetCaloriesBurned = newTargetBurned,
                    mealCalorieDistribution = distribution
                )
            } else {
                currentState
            }
        }
    }

    private fun processReminders(user: User?): Pair<List<ReminderItem>, Map<String, String>> {
        if (user?.reminderSettings == null) {
            return Pair(emptyList(), emptyMap())
        }
        val exerciseReminders = mutableListOf<ReminderItem>()
        val mealTimes = mutableMapOf<String, String>()

        user.reminderSettings?.forEach { (id, timeValue) ->
            val timeString = timeValue // Already String? in User model
            if (timeString != null) {
                val parts = timeString.split(":")
                if (parts.size == 2) {
                    val hour = parts[0].toIntOrNull()
                    val minute = parts[1].toIntOrNull()
                    if (hour != null && minute != null) {
                        val formattedTime = String.format(Locale.getDefault(), "%02d:%02d", hour, minute)
                        if (MealsType.mealTypes.any { it.id == id }) {
                            mealTimes[id] = formattedTime
                        } else { // Assume exercise if not meal
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

    private fun calculateProgress(user: User?, logs: DailyLogData?): HomeUiState {
        val foodLogs = logs?.foodLogs?.values?.toList() ?: emptyList()
        val waterLogs = logs?.waterLogs?.values?.toList() ?: emptyList()
        val exerciseLogsList = logs?.exerciseLogs?.values?.toList() ?: emptyList()

        val consumedCalories = foodLogs.sumOf { it.calories }
        val consumedWaterMl = waterLogs.sumOf { it.amountMl }
        val burnedCalories = exerciseLogsList.sumOf { it.caloriesBurned }

        val mealConsumed = mutableMapOf<String, Int>()
        foodLogs.forEach { log ->
            val current = mealConsumed.getOrDefault(log.mealType, 0)
            mealConsumed[log.mealType] = current + log.calories
        }

        return _uiState.value.copy(
            consumedCalories = consumedCalories,
            consumedWaterMl = consumedWaterMl,
            burnedCalories = burnedCalories,
            mealConsumedCalories = mealConsumed
        )
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