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
import uz.oybek.ozamiz.data.logs.DailyLogRepository // DailyLogCacheManager endi kerak emas
import uz.oybek.ozamiz.data.utils.NutritionCalculator
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
            // Trigger background check/update for targets in DB if needed
            recalculateTargetsIfNeeded(initialUser)
        } else {
            // Handle case where no valid user is cached locally
            _uiState.update { it.copy(isLoading = false, errorMessage = "Foydalanuvchi sozlanmagan") }
        }

        // 2. Start background observers for RTDB updates (User and Logs)
        // These will first return cached data (if available) then network updates
        observeUserUpdatesFromRepository()
        observeLogs()
    }

    // Observes updates pushed to LocalUser by UserRepository's RTDB listener
    private fun observeUserUpdatesFromRepository() {
        viewModelScope.launch {
            LocalUser.userLive.observeForever { user ->
                val previousState = _uiState.value
                if (user != null && user.valid) {
                    if (user != previousState.user) { // Process only if user object changed
                        val needsRecalc = previousState.user?.let { oldUser ->
                            user.gender != oldUser.gender || user.age != oldUser.age || user.height != oldUser.height ||
                                    user.weight != oldUser.weight || user.activityLevel != oldUser.activityLevel ||
                                    user.weightGoal != oldUser.weightGoal || user.targetCalories <= 0 ||
                                    user.targetWaterMl <= 0 || user.targetCaloriesBurned <= 0
                        } ?: true

                        val settingsChanged = previousState.user?.reminderSettings != user.reminderSettings

                        var newState = previousState.copy(user = user) // Start with updated user

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
                            // Trigger background DB update if targets were recalculated
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
                        }

                        // Recalculate progress using the latest logs from the state
                        newState = calculateProgress(user, newState.dailyLogData) // Use logs from current state

                        _uiState.value = newState // Update the state flow
                    }
                } else if (previousState.user != null) { // Handle logout
                    _uiState.value = HomeUiState(isLoading = false, errorMessage = "Foydalanuvchi tizimdan chiqdi")
                }
            }
        }
    }

    // Observes log updates from RTDB (listener provides cache first, then network)
    private fun observeLogs() {
        viewModelScope.launch {
            dailyLogRepository.observeTodayLogs()
                .collectLatest { result ->
                    // Listener gives updates from RTDB cache/network
                    when (result) {
                        is DataWrapper.Loading -> { /* Ignore loading state here */ }
                        is DataWrapper.Success -> {
                            val logs = result.data
                            // Update state with new logs and recalculated progress
                            _uiState.update { currentState ->
                                // Crucially, calculate progress using the *user* currently in the state
                                calculateProgress(currentState.user, logs).copy(
                                    errorMessage = null, // Clear error on success
                                    dailyLogData = logs,
                                    todayExerciseLogs = logs.exerciseLogs?.values
                                        ?.sortedByDescending { it.timestamp as? Long ?: 0L }
                                        ?.toList() ?: emptyList()
                                )
                            }
                        }
                        is DataWrapper.Error -> {
                            // Show error, keep potentially stale logs in UI?
                            _uiState.update { it.copy(errorMessage = result.error) }
                            // Option: Clear logs visually on error
                            // _uiState.update { calculateProgress(it.user, DailyLogData()) }
                        }
                    }
                }
        }
    }

    // --- Helper methods (recalculateTargetsIfNeeded, processReminders, calculateProgress) ---
    // These remain mostly the same, ensuring they read from/update the _uiState correctly

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
            newTargetBurned = 300 // Example default
            needsDbUpdate = true
        }

        // Only trigger DB update if values actually changed
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
        // Update UI state only if targets or distribution differ from current state
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
                    // Ensure user object in state reflects these potentially new targets IF needsDbUpdate was true?
                    // Or rely on observeUserUpdatesFromRepository to update the user object later.
                    // Let's keep it simple: update targets/distribution here. The user object will update via its observer.
                )
            } else {
                currentState // No change needed
            }
        }
    }

    private fun fetchAndSetReminders(user: User?) {
        val result = processReminders(user)
        if (_uiState.value.enabledExerciseReminders != result.first || _uiState.value.mealReminderTimes != result.second) {
            _uiState.update { it.copy(enabledExerciseReminders = result.first, mealReminderTimes = result.second) }
        }
    }

    private fun processReminders(user: User?): Pair<List<ReminderItem>, Map<String, String>> {
        if (user?.reminderSettings == null) {
            return Pair(emptyList(), emptyMap())
        }
        val exerciseReminders = mutableListOf<ReminderItem>()
        val mealTimes = mutableMapOf<String, String>()
        user.reminderSettings?.forEach { (id, timeValue) ->
            val timeString = timeValue as? String
            if (timeString != null) {
                val parts = timeString.split(":")
                if (parts.size == 2) {
                    val hour = parts[0].toIntOrNull()
                    val minute = parts[1].toIntOrNull()
                    if (hour != null && minute != null) {
                        val formattedTime = String.format(Locale.getDefault(), "%02d:%02d", hour, minute)
                        if (MealsType.mealTypes.any { it.id == id }) {
                            mealTimes[id] = formattedTime
                        } else if (ExerciseType.getDefaultExerciseTypes().any { it.id == id }) {
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

    // Calculates progress based on provided user and logs, returns a *new* state object
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

        // Return a copy of the *current* state with updated progress fields
        return _uiState.value.copy(
            consumedCalories = consumedCalories,
            consumedWaterMl = consumedWaterMl,
            burnedCalories = burnedCalories,
            mealConsumedCalories = mealConsumed
        )
    }

    // --- Action Functions ---

    suspend fun addWaterLog(amountMl: Int): DataWrapper<Unit> {
        if (amountMl <= 0) return DataWrapper.Error("Miqdor 0 dan katta bo'lishi kerak")
        val waterLog = WaterLog(amountMl = amountMl)
        return dailyLogRepository.addWaterLog(waterLog) // Triggers observeLogs
    }

    fun deleteFoodLog(log: FoodLog) {
        viewModelScope.launch {
            val result = dailyLogRepository.deleteFoodLog(log.logId)
            if (result is DataWrapper.Error) {
                _uiState.update { it.copy(errorMessage = "Ovqat logini o'chirishda xato: ${result.error}") }
            }
            // UI updates via observeLogs
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
            // UI updates via observeLogs
        }
    }

    fun clearErrorMessage() {
        _uiState.update { it.copy(errorMessage = null) }
    }
}