package uz.oybek.ozamiz.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.asFlow
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import uz.oybek.ozamiz.data.DataWrapper
import uz.oybek.ozamiz.data.LocalUser
import uz.oybek.ozamiz.data.User
import uz.oybek.ozamiz.data.UserRepository
import uz.oybek.ozamiz.data.info.DailyLogData
import uz.oybek.ozamiz.data.info.ExerciseLog
import uz.oybek.ozamiz.data.info.FoodLog
import uz.oybek.ozamiz.data.info.WaterLog
import uz.oybek.ozamiz.data.logs.DailyLogRepository
import uz.oybek.ozamiz.data.model.StreakInfo
import uz.oybek.ozamiz.data.utils.NutritionCalculator
import uz.oybek.ozamiz.reminders.ReminderScheduler
import java.util.*
import java.util.concurrent.TimeUnit

class HomeViewModel : ViewModel() {

    private val dailyLogRepository = DailyLogRepository()

    // Holds the currently selected date, defaults to today.
    private val _selectedDate = MutableStateFlow(getMidnight(Date()))

    private val _uiState = MutableStateFlow(HomeUiState(isLoading = true))
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        // Initial setup
        viewModelScope.launch(Dispatchers.Default) {
            val initialUser = LocalUser.user
            _uiState.update {
                it.copy(
                    user = initialUser.takeIf { u -> u.valid },
                    // Sanalar ro'yxati faqat bir marta shu yerda yaratiladi
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
                        ReminderScheduler.scheduleAllActiveReminders(uz.oybek.ozamiz.appContext)
                    }
                } else if (previousState.user != null) {
                    _uiState.value = HomeUiState(isLoading = false, errorMessage = "Foydalanuvchi tizimdan chiqdi")
                }
            }
        }
    }

    private fun observeDateAndLogs() {
        viewModelScope.launch {
            _selectedDate.collectLatest { date ->
                _uiState.update { it.copy(isLoading = true) } // Sana o'zgarganda yuklanishni ko'rsatish
                dailyLogRepository.observeLogsForDate(date).collectLatest { result ->
                    when (result) {
                        is DataWrapper.Loading -> { /* isLoading allaqachon true */ }
                        is DataWrapper.Success -> {
                            val logs = result.data
                            _uiState.update { currentState ->
                                val processed = withContext(Dispatchers.Default) {
                                    processUserAndLogs(currentState.user, logs)
                                }
                                processed.copy(isLoading = false, errorMessage = null, selectedDate = date)
                            }
                        }
                        is DataWrapper.Error -> _uiState.update { it.copy(isLoading = false, errorMessage = result.error, dailyLogData = DailyLogData()) }
                    }
                }
            }
        }
    }

    private fun processUserAndLogs(user: User?, logs: DailyLogData?): HomeUiState {
        if (user == null || !user.valid) {
            return _uiState.value.copy(isLoading = false, errorMessage = "Foydalanuvchi sozlanmagan")
        }

        val foodLogs = logs?.foodLogs?.values?.toList() ?: emptyList()
        val waterLogs = logs?.waterLogs?.values?.toList() ?: emptyList()
        val exerciseLogsList = logs?.exerciseLogs?.values?.sortedByDescending { it.timestamp as? Long ?: 0L }?.toList() ?: emptyList()

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
            targetCaloriesBurned = user.targetCaloriesBurned,
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
        // 7 kun orqaga
        for (i in 7 downTo 1) {
            calendar.time = Date()
            calendar.add(Calendar.DAY_OF_YEAR, -i)
            dates.add(getMidnight(calendar.time))
        }
        // Bugun
        dates.add(getMidnight(Date()))
        // 7 kun oldinga
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