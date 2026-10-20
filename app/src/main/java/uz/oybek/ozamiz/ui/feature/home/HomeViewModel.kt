package uz.oybek.ozamiz.ui.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import uz.oybek.ozamiz.common.util.DateFormatter
import uz.oybek.ozamiz.common.util.dateApiFormat
import uz.oybek.ozamiz.common.util.isToday
import uz.oybek.ozamiz.common.util.orZero
import uz.oybek.ozamiz.common.util.toDayOfMonth
import uz.oybek.ozamiz.data.dietolog.api.DietologRepository
import uz.oybek.ozamiz.data.dietolog.engine.DietologEvaluationEngine
import uz.oybek.ozamiz.data.limit.api.LimitManagerRepo
import uz.oybek.ozamiz.data.profile.api.ProfileRepo
import uz.oybek.ozamiz.data.tracker.api.DailyTrackerRepo
import uz.oybek.ozamiz.data.tracker.api.FoodTrackerRepo
import uz.oybek.ozamiz.data.tracker.model.DailyCalorieMacroSummary
import uz.oybek.ozamiz.data.tracker.model.DailyCalorieProgress
import uz.oybek.ozamiz.data.tracker.session.SelectedDateManager
import uz.oybek.ozamiz.data.user.local.user
import uz.oybek.ozamiz.ui.feature.limit.FoodLimitArgs
import uz.oybek.ozamiz.ui.utils.showToast
import uz.oybek.ozamiz.common.util.logx
import java.util.Date
import javax.inject.Inject

sealed interface HomeUiEffect {
    data class NavigateToFoodCamera(val mealType: String) : HomeUiEffect
    data class ShowFoodLimitSheet(val args: FoodLimitArgs) : HomeUiEffect
}

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val dailyTrackerRepo: DailyTrackerRepo,
    private val foodTrackerRepo: FoodTrackerRepo,
    private val profileRepo: ProfileRepo,
    private val selectedDateManager: SelectedDateManager,
    private val limitManagerRepo: LimitManagerRepo,
    private val dietologRepository: DietologRepository,
    private val evaluationEngine: DietologEvaluationEngine
) : ViewModel() {

    private val _effects = Channel<HomeUiEffect>(Channel.BUFFERED)
    val effects = _effects.receiveAsFlow()

    fun onAddMealClicked(mealId: String) {
        val limits = limitManagerRepo.getLimits()
        viewModelScope.launch {
            if (limits.canLogFood) {
                _effects.send(HomeUiEffect.NavigateToFoodCamera(mealId))
            } else {
                _effects.send(
                    HomeUiEffect.ShowFoodLimitSheet(
                        FoodLimitArgs(
                            remainingLimit = limits.remainingFoodLimit,
                            targetLimit = limits.foodLimit,
                            mealType = mealId
                        )
                    )
                )
            }
        }
    }

    private val scrollerDates = DateFormatter.generateRangeDays(7)

    private val _uiState = MutableStateFlow(
        HomeUiState(
            calendar = CalendarUiState(
                selectedDate = selectedDateManager.getSelectedDate(),
                items = scrollerDates.map { date ->
                    createDateItem(date, selectedDateManager.getSelectedDate())
                }
            ),
            isLoading = true
        )
    )
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch { loadCalendarSummaries() }
        observeSelectedDate()
        observeUser()
        observeDietolog()
        // Ilovaga kirganda aqlli eslatmalar logikasini baholash
        viewModelScope.launch {
            evaluationEngine.evaluateAndTriggerReminders()
        }
    }

    private fun observeDietolog() {
        viewModelScope.launch {
            dietologRepository.getLatestDietologAutoReminderFlow().collectLatest { latestReminder ->
                _uiState.update { state ->
                    state.copy(
                        dietolog = HomeDietologUiState(
                            lastMessageText = latestReminder?.text,
                            messageCount = if (latestReminder != null) 1 else 0
                        )
                    )
                }
            }
        }
    }

    fun checkDateCalendar() = selectedDateManager.checkDate()

    fun onDateSelected(date: Date) = selectedDateManager.updateDate(date)

    private fun createDateItem(
        date: Date, selectedDate: Date, progress: DailyCalorieProgress? = null
    ) = DateScrollerItem(
        date = date,
        dayOfMonth = date.toDayOfMonth(),
        isToday = date.isToday(),
        isSelected = DateFormatter.isSameDay(date, selectedDate),
        progressPercent = progress?.progressPercent.orZero(),
        isExceeded = progress?.isExceededWarning ?: false,
        isCompleted = progress?.isCompleted ?: false
    )

    private suspend fun loadCalendarSummaries() {
        if (scrollerDates.isEmpty()) return
        val summaries =
            dailyTrackerRepo.getDailySummaries(scrollerDates.first(), scrollerDates.last())
        _uiState.update { state ->
            val items = scrollerDates.map { date ->
                createDateItem(
                    date, state.calendar.selectedDate, summaries[date.dateApiFormat()]
                )
            }
            state.copy(calendar = state.calendar.copy(items = items))
        }
    }

    private fun observeUser() {
        viewModelScope.launch {
            profileRepo.user.collect { user ->
                if (user.valid) {
                    val remindersMap = user.mealReminders.associate { it.type to it.time }
                    val profileState = HomeProfileUiState(
                        userGoal = user.userGoal,
                        showWeightLogCard = user.shouldLogWeight,
                        paymentChecking = user.paymentChecking,
                        userPoints = user.scorePoints,
                        daysFromRegister = user.daysFromRegister,
                        isRated = user.isRated,
                        unreadMessages = user.unreadMessages
                    )
                    _uiState.update { it.copy(profile = profileState, mealReminders = remindersMap) }
                } else {
                    _uiState.update { it.copy(profile = HomeProfileUiState()) }
                }
            }
        }
    }

    private fun observeSelectedDate() {
        viewModelScope.launch {
            selectedDateManager.selectedDate.distinctUntilChanged { old, new ->
                DateFormatter.isSameDay(
                    old,
                    new
                )
            }.collectLatest { date ->
                _uiState.update {
                    it.copy(
                        calendar = it.calendar.selectDate(date),
                        isLoading = true
                    )
                }

                coroutineScope {
                    launch {
                        dailyTrackerRepo.observeDailyCalMacros(date).collect { macro ->
                            _uiState.update { state ->
                                state.copy(
                                    calorieMacro = macro,
                                    water = macro.waterSummary,
                                    exercise = macro.exerciseSummary,
                                    calendar = state.calendar.updateProgress(date, macro),
                                    isLoading = false
                                )
                            }
                        }
                    }
                    launch {
                        foodTrackerRepo.observeDailyMeals(date).collect { meals ->
                            _uiState.update { state ->
                                state.copy(meals = meals)
                            }
                        }
                    }
                }
            }
        }
    }
    private fun CalendarUiState.selectDate(date: Date): CalendarUiState {
        val updated = items.map { item ->
            val isSelected = DateFormatter.isSameDay(item.date, date)
            if (item.isSelected != isSelected) item.copy(isSelected = isSelected) else item
        }
        return copy(selectedDate = date, items = updated)
    }

    private fun CalendarUiState.updateProgress(
        date: Date, macro: DailyCalorieMacroSummary
    ): CalendarUiState {
        val current = items.find { DateFormatter.isSameDay(it.date, date) } ?: return this
        if (current.progressPercent == macro.progressPercent && current.isExceeded == macro.isExceededWarning && current.isCompleted == macro.isCompleted) return this

        val updated = items.map { item ->
            if (item === current) {
                item.copy(
                    progressPercent = macro.progressPercent,
                    isExceeded = macro.isExceededWarning,
                    isCompleted = macro.isCompleted
                )
            } else item
        }
        return copy(items = updated)
    }

    fun refresh() {
        viewModelScope.launch {
            _uiState.update { it.copy(calendar = it.calendar.copy(isRefreshing = true)) }
            try {
                loadCalendarSummaries()
            } finally {
                _uiState.update { it.copy(calendar = it.calendar.copy(isRefreshing = false)) }
            }
        }
    }
}
