package uz.oybek.ozamiz.ui.discover

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import uz.oybek.ozamiz.data.DataWrapper
import uz.oybek.ozamiz.data.workouts.Workout
import uz.oybek.ozamiz.data.workouts.WorkoutCategory
import uz.oybek.ozamiz.data.workouts.WorkoutRepository
import uz.oybek.ozamiz.handleException
import uz.oybek.ozamiz.ui.getinfo.Event
import java.util.concurrent.atomic.AtomicInteger

sealed class DiscoverNavigationEvent {
    data class NavigateToWorkoutDetail(val workoutId: String) : DiscoverNavigationEvent()
    object NavigateToAddWorkout : DiscoverNavigationEvent()
    object NavigateToWorkoutList : DiscoverNavigationEvent()
    data class NavigateToCategoryWorkoutList(val categoryId: String, val categoryName: String): DiscoverNavigationEvent()
    object NavigateToAddWorkoutCategory : DiscoverNavigationEvent()
    object NavigateToAddExercise : DiscoverNavigationEvent()
}

class DiscoverViewModel : ViewModel() {

    private val workoutRepository = WorkoutRepository()

    private val _uiState = MutableStateFlow(DiscoverUiState())
    val uiState: StateFlow<DiscoverUiState> = _uiState.asStateFlow()

    private val _navigationEvent = MutableLiveData<Event<DiscoverNavigationEvent>>()
    val navigationEvent: LiveData<Event<DiscoverNavigationEvent>> = _navigationEvent

    private var loadCategoriesJob: Job? = null
    private var loadWorkoutsJob: Job? = null
    private var categoryJobs = mutableListOf<Job>() // Track individual category loading jobs

    init {
        loadInitialData()
    }

    fun loadInitialData(isRefresh: Boolean = false) {
        // Avoid restarting if already loading unless it's a refresh
        if (_uiState.value.isOverallLoading && !isRefresh) return
        if (_uiState.value.isRefreshing && isRefresh) return

        // Cancel previous loading jobs
        loadCategoriesJob?.cancel()
        loadWorkoutsJob?.cancel()
        categoryJobs.forEach { it.cancel() }
        categoryJobs.clear()

        _uiState.update {
            it.copy(
                isLoadingCategories = true,
                isLoadingWorkouts = true, // Assume workouts will load after categories
                isRefreshing = isRefresh,
                errorMessage = null,
                searchResults = null,
                // Clear data only on refresh
                categories = if (isRefresh) emptyList() else it.categories,
                workoutsByCategory = if (isRefresh) emptyMap() else it.workoutsByCategory
            )
        }

        loadCategoriesAndWorkouts()
    }

    private fun loadCategoriesAndWorkouts() {
        loadCategoriesJob = viewModelScope.launch {
            workoutRepository.getWorkoutCategoriesFlow()
                .catch { e ->
                    handleException(e as Exception, "DiscoverGetCategories")
                    _uiState.update { it.copy(isLoadingCategories = false, isLoadingWorkouts = false, isRefreshing = false, errorMessage = "Kategoriya yuklashda xatolik") }
                }
                .collectLatest { categoryResult ->
                    when(categoryResult) {
                        is DataWrapper.Success -> {
                            val categories = categoryResult.data
                            // Update categories and mark category loading as finished
                            _uiState.update { it.copy(isLoadingCategories = false, categories = categories) }
                            // Now load workouts for these categories
                            loadWorkoutsForCategories(categories)
                        }
                        is DataWrapper.Error -> {
                            // Stop all loading on category error
                            _uiState.update { it.copy(isLoadingCategories = false, isLoadingWorkouts = false, isRefreshing = false, errorMessage = categoryResult.error) }
                        }
                        is DataWrapper.Loading -> {
                            // Categories are loading, keep isLoadingCategories = true
                            _uiState.update { it.copy(isLoadingCategories = true) }
                        }
                    }
                }
        }
    }

    private fun loadWorkoutsForCategories(categories: List<WorkoutCategory>) {
        // Cancel previous workout jobs only
        categoryJobs.forEach { it.cancel() }
        categoryJobs.clear()
        val workoutsMap = _uiState.value.workoutsByCategory.toMutableMap()

        // If no categories, workouts are done loading
        if (categories.isEmpty()) {
            _uiState.update { it.copy(isLoadingWorkouts = false, isRefreshing = false) }
            return
        }

        _uiState.update { it.copy(isLoadingWorkouts = true) } // Mark workouts as loading

        // Counter for completed initial loads
        val loadedCount = AtomicInteger(0)
        val totalToLoad = categories.size

        categories.forEach { category ->
            val job = viewModelScope.launch { // Launch separate job for each category
                workoutRepository.getWorkoutsByCategoryFlow(category.id, limit = 6)
                    .catch { e ->
                        handleException(e as Exception, "LoadWorkoutsForCategory_${category.id}")
                        // Mark this category as "loaded" (even with error) for the counter
                        if (loadedCount.incrementAndGet() >= totalToLoad) {
                            _uiState.update { it.copy(isLoadingWorkouts = false, isRefreshing = false) }
                        }
                    }
                    .collectLatest { result ->
                        var isInitialLoadCompleteForThisCategory = false
                        when(result) {
                            is DataWrapper.Success -> {
                                workoutsMap[category.id] = result.data
                                isInitialLoadCompleteForThisCategory = true
                            }
                            is DataWrapper.Error -> {
                                workoutsMap.remove(category.id) // Remove potentially stale data
                                isInitialLoadCompleteForThisCategory = true // Mark as done even on error
                            }
                            is DataWrapper.Loading -> {
                                // Still loading, don't increment counter yet
                            }
                        }
                        // Update the map in the UI state
                        _uiState.update {
                            it.copy(workoutsByCategory = workoutsMap.toMap())
                        }

                        // Increment counter and check if all initial loads are done
                        if (isInitialLoadCompleteForThisCategory) {
                            if (loadedCount.incrementAndGet() >= totalToLoad) {
                                _uiState.update { it.copy(isLoadingWorkouts = false, isRefreshing = false) }
                            }
                        }
                    }
            }
            categoryJobs.add(job) // Add job to the list to be managed
        }
    }


    fun searchWorkouts(query: String) {
        loadCategoriesJob?.cancel()
        loadWorkoutsJob?.cancel()
        categoryJobs.forEach { it.cancel() }
        categoryJobs.clear()

        if (query.isBlank()) {
            clearSearch()
            return
        }
        _uiState.update { it.copy(isLoadingWorkouts = true, searchResults = emptyList(), errorMessage = null, isLoadingCategories = false, isRefreshing = false) } // Stop other loadings

        viewModelScope.launch {
            // searchWorkoutsOnce uses get(), benefiting from cache but not real-time
            when (val result = workoutRepository.searchWorkoutsOnce(query)) {
                is DataWrapper.Success -> {
                    _uiState.update { it.copy(isLoadingWorkouts = false, searchResults = result.data) }
                }
                is DataWrapper.Error -> {
                    _uiState.update { it.copy(isLoadingWorkouts = false, errorMessage = result.error, searchResults = emptyList()) }
                    handleException(Exception(result.error), "DiscoverSearch")
                }
                is DataWrapper.Loading -> {}
            }
        }
    }

    fun clearSearch() {
        // When search is cleared, reload the initial category/workout view
        _uiState.update { it.copy(searchResults = null) }
        loadInitialData() // Reload the default view
    }

    fun onWorkoutClicked(workoutId: String) {
        _navigationEvent.value = Event(DiscoverNavigationEvent.NavigateToWorkoutDetail(workoutId))
    }

    fun onSeeAllClicked(category: WorkoutCategory) {
        _navigationEvent.value = Event(DiscoverNavigationEvent.NavigateToCategoryWorkoutList(category.id, category.name))
    }

    fun onAddWorkoutClicked() { _navigationEvent.value = Event(DiscoverNavigationEvent.NavigateToAddWorkout) }
    fun onAddWorkoutCategoryClicked() { _navigationEvent.value = Event(DiscoverNavigationEvent.NavigateToAddWorkoutCategory) }
    fun onAddExerciseClicked() { _navigationEvent.value = Event(DiscoverNavigationEvent.NavigateToAddExercise) }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    override fun onCleared() {
        super.onCleared()
        loadCategoriesJob?.cancel()
        loadWorkoutsJob?.cancel()
        categoryJobs.forEach { it.cancel() }
    }
}