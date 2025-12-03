package uz.oybek.ozamiz.ui.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.asFlow
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import uz.oybek.ozamiz.appContext
import uz.oybek.ozamiz.data.logs.LogRepository
import uz.oybek.ozamiz.data.logs.model.DailyLogs
import uz.oybek.ozamiz.data.logs.model.ExerciseLog
import uz.oybek.ozamiz.data.logs.model.FoodLog
import uz.oybek.ozamiz.data.logs.model.WaterLog
import uz.oybek.ozamiz.data.streak.StreakInfo
import uz.oybek.ozamiz.data.user.local.LocalUser
import uz.oybek.ozamiz.data.user.model.User
import uz.oybek.ozamiz.data.utils.DataWrapper
import uz.oybek.ozamiz.notification.reminders.ReminderScheduler
import java.util.Calendar
import java.util.Date

class HomeViewModel : ViewModel() {

    private val dailyLogRepository = LogRepository()

    private val _selectedDate = MutableStateFlow(getMidnight(Date()))

    private val _uiState = MutableStateFlow(HomeUiState(isLoading = true))
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch(Dispatchers.Default) {
            val initialUser = LocalUser.user
            _uiState.update {
                it.copy(
                    user = initialUser.takeIf { u -> u.valid },
                    datesForScroller = generateDatesForScroller(),
                    selectedDate = _selectedDate.value
                )
            }
        }
        observeUserUpdates()
        observeDateAndLogs()
    }

    private fun observeUserUpdates() {
        viewModelScope.launch {
            LocalUser.userLive.asFlow().distinctUntilChanged().collectLatest { user ->
                val previousState = _uiState.value
                if (user != null && user.valid) {
                    val processedState = withContext(Dispatchers.Default) {
                        processUserAndLogs(user, previousState.dailyLogData)
                    }
                    _uiState.update { processedState }

                    if (previousState.user?.reminderSettings != user.reminderSettings) {
                        ReminderScheduler.scheduleAllActiveReminders(appContext)
                    }
                } else if (previousState.user != null) {
                    _uiState.value = HomeUiState(
                        isLoading = false,
                        errorMessage = "Foydalanuvchi tizimdan chiqdi"
                    )
                }
            }
        }
    }

    private fun observeDateAndLogs() {
        viewModelScope.launch {
            _selectedDate.collectLatest { date ->
                _uiState.update { it.copy(isLoading = true) }
                dailyLogRepository.observeLogsForDate(date).collectLatest { result ->
                    when (result) {
                        is DataWrapper.Success -> {
                            val logs = result.data
                            _uiState.update { currentState ->
                                val processed = withContext(Dispatchers.Default) {
                                    processUserAndLogs(currentState.user, logs)
                                }
                                processed.copy(
                                    isLoading = false,
                                    errorMessage = null,
                                    selectedDate = date
                                )
                            }
                        }

                        is DataWrapper.Error -> _uiState.update {
                            it.copy(
                                isLoading = false,
                                errorMessage = result.error,
                                dailyLogData = DailyLogs()
                            )
                        }
                    }
                }
            }
        }
    }

    private fun processUserAndLogs(user: User?, logs: DailyLogs?): HomeUiState {
        if (user == null || !user.valid) {
            return _uiState.value.copy(
                isLoading = false,
                errorMessage = "Foydalanuvchi sozlanmagan"
            )
        }

        val foodLogs = logs?.foodLogs?.values?.toList() ?: emptyList()
        val waterLogs = logs?.waterLogs?.values?.toList() ?: emptyList()
        val exerciseLogsList =
            logs?.exerciseLogs?.values?.sortedByDescending { it.timestamp as? Long ?: 0L }?.toList()
                ?: emptyList()

        val consumedCalories = foodLogs.sumOf { it.calories }
        val consumedWaterMl = waterLogs.sumOf { it.amountMl }
        val burnedCalories = exerciseLogsList.sumOf { it.caloriesBurned }
        val consumedFat = foodLogs.sumOf { it.fat ?: 0.0 }
        val consumedCarbs = foodLogs.sumOf { it.carbs ?: 0.0 }
        val consumedProtein = foodLogs.sumOf { it.protein ?: 0.0 }

        return _uiState.value.copy(
            user = user,
            dailyLogData = logs,
            consumedCalories = consumedCalories,
            consumedWaterMl = consumedWaterMl,
            burnedCalories = burnedCalories,
            targetCalories = user.targetCalories,
            targetWaterMl = user.targetWaterMl,
            targetCaloriesBurned = user.targetCalories,
            streakInfo = user.streakInfo ?: StreakInfo(),
            fat = consumedFat,
            carbs = consumedCarbs,
            protein = consumedProtein
        )
    }

    fun onDateSelected(date: Date) {
        _selectedDate.value = getMidnight(date)
    }

    fun refreshData() {
        val currentDate = _selectedDate.value
        _selectedDate.value = Date(0)
        _selectedDate.value = currentDate
    }

    private fun generateDatesForScroller(): List<Date> {
        val calendar = Calendar.getInstance()
        val dates = mutableListOf<Date>()
        for (i in 7 downTo 1) {
            calendar.time = Date()
            calendar.add(Calendar.DAY_OF_YEAR, -i)
            dates.add(getMidnight(calendar.time))
        }
        dates.add(getMidnight(Date()))
        for (i in 1..7) {
            calendar.time = Date()
            calendar.add(Calendar.DAY_OF_YEAR, i)
            dates.add(getMidnight(calendar.time))
        }
        return dates
    }

    fun addWaterLog(amountMl: Int) {
        viewModelScope.launch {
            if (amountMl <= 0) {
                _uiState.update { it.copy(errorMessage = "Miqdor 0 dan katta bo'lishi kerak") }
                return@launch
            }
            val waterLog = WaterLog(amountMl = amountMl)
            val result = dailyLogRepository.addWaterLog(waterLog, _selectedDate.value)
            if (result is DataWrapper.Error) {
                _uiState.update { it.copy(errorMessage = "Suv logini saqlashda xato: ${result.error}") }
            }
        }
    }

    fun deleteFoodLog(log: FoodLog) {
        viewModelScope.launch {
            val result = dailyLogRepository.deleteFoodLog(log.logId, _selectedDate.value)
            if (result is DataWrapper.Error) {
                _uiState.update { it.copy(errorMessage = "Ovqat logini o'chirishda xato: ${result.error}") }
            }
        }
    }

    fun deleteWaterLog(log: WaterLog) {
        viewModelScope.launch {
            val result = dailyLogRepository.deleteWaterLog(log.logId, _selectedDate.value)
            if (result is DataWrapper.Error) {
                _uiState.update { it.copy(errorMessage = "Suv logini o'chirishda xato: ${result.error}") }
            }
        }
    }

    fun deleteExerciseLog(log: ExerciseLog) {
        viewModelScope.launch {
            val result = dailyLogRepository.deleteExerciseLog(log.logId, _selectedDate.value)
            if (result is DataWrapper.Error) {
                _uiState.update { it.copy(errorMessage = "Mashq logini o'chirishda xato: ${result.error}") }
            }
        }
    }

    fun clearErrorMessage() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    private fun getMidnight(date: Date): Date {
        return Calendar.getInstance().apply {
            time = date
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.time
    }
}