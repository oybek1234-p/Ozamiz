package uz.oybek.ozamiz.ui.discover

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import uz.oybek.ozamiz.data.DataWrapper
import uz.oybek.ozamiz.data.workouts.Workout // Import Workout
import uz.oybek.ozamiz.data.workouts.WorkoutRepository
import uz.oybek.ozamiz.handleException
import uz.oybek.ozamiz.ui.getinfo.Event

sealed class WorkoutListNavigationEvent {
    data class NavigateToWorkoutDetail(val workoutId: String) : WorkoutListNavigationEvent()
    object NavigateToAddWorkout : WorkoutListNavigationEvent()
}

class WorkoutListViewModel : ViewModel() {

    private val repository = WorkoutRepository()

    private val _uiState = MutableStateFlow(WorkoutListUiState())
    val uiState: StateFlow<WorkoutListUiState> = _uiState.asStateFlow()

    private val _navigationEvent = MutableLiveData<Event<WorkoutListNavigationEvent>>()
    val navigationEvent: LiveData<Event<WorkoutListNavigationEvent>> = _navigationEvent

    private var currentCategoryId: String? = null
    private var currentSearchQuery: String? = null
    private var loadWorkoutsJob: Job? = null

    fun initialize(categoryId: String?, searchQuery: String?, title: String) {
        if (currentCategoryId == categoryId && currentSearchQuery == searchQuery && !_uiState.value.isLoading) {
            if (_uiState.value.listTitle != title) {
                _uiState.update { it.copy(listTitle = title) }
            }
            return
        }
        currentCategoryId = categoryId
        currentSearchQuery = searchQuery
        _uiState.update { WorkoutListUiState(listTitle = title, isLoading = true) } // Reset state
        loadWorkouts()
    }

    fun loadWorkouts() {
        loadWorkoutsJob?.cancel()
        _uiState.update { it.copy(isLoading = true, errorMessage = null) }

        loadWorkoutsJob = viewModelScope.launch {
            if (!currentSearchQuery.isNullOrBlank()) {
                // Handle Search (uses get(), benefits from cache)
                when (val result = repository.searchWorkoutsOnce(currentSearchQuery!!, limit = 50)) {
                    is DataWrapper.Success -> {
                        _uiState.update { it.copy(isLoading = false, workouts = result.data) }
                    }
                    is DataWrapper.Error -> {
                        _uiState.update { it.copy(isLoading = false, errorMessage = result.error) }
                        handleException(Exception(result.error), "LoadWorkoutList_Search")
                    }
                    is DataWrapper.Loading -> {} // Handled by initial state update
                }
            } else if (!currentCategoryId.isNullOrBlank()) {
                // Handle Category List (uses Flow)
                repository.getWorkoutsByCategoryFlow(currentCategoryId!!, limit = 50) // Use Flow version
                    .catch { e ->
                        handleException(e as Exception, "LoadWorkoutList_Category")
                        _uiState.update { it.copy(isLoading = false, errorMessage = "Xatolik: ${e.localizedMessage}") }
                    }
                    .collectLatest { result ->
                        when (result) {
                            is DataWrapper.Success -> {
                                _uiState.update { it.copy(isLoading = false, workouts = result.data) }
                            }
                            is DataWrapper.Error -> {
                                _uiState.update { it.copy(isLoading = false, errorMessage = result.error) }
                            }
                            is DataWrapper.Loading -> {
                                // Already handled by initial update, or could show specific loading here
                            }
                        }
                    }
            } else {
                // No category or search query provided
                _uiState.update { it.copy(isLoading = false, workouts = emptyList(), errorMessage = "Kategoriya yoki qidiruv so'rovi yo'q") }
            }
        }
    }

    fun onWorkoutClicked(workoutId: String) {
        _navigationEvent.value = Event(WorkoutListNavigationEvent.NavigateToWorkoutDetail(workoutId))
    }

    fun onAddWorkoutClicked() {
        _navigationEvent.value = Event(WorkoutListNavigationEvent.NavigateToAddWorkout)
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }
}