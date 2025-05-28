package uz.oybek.ozamiz.ui.discover

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import uz.oybek.ozamiz.data.DataWrapper
import uz.oybek.ozamiz.data.workouts.Video
import uz.oybek.ozamiz.data.workouts.VideoRepository
import uz.oybek.ozamiz.data.workouts.Workout
import uz.oybek.ozamiz.data.workouts.WorkoutCategory
import uz.oybek.ozamiz.data.workouts.WorkoutRepository
import uz.oybek.ozamiz.handleException
import uz.oybek.ozamiz.ui.getinfo.Event
import java.util.concurrent.atomic.AtomicInteger

sealed class DiscoverNavigationEvent {
    data class NavigateToWorkoutDetail(val workoutId: String) : DiscoverNavigationEvent()
    data class NavigateToVideoDetail(val video: Video) : DiscoverNavigationEvent()
    object NavigateToAddWorkout : DiscoverNavigationEvent()
    data class NavigateToCategoryWorkoutList(val categoryId: String, val categoryName: String): DiscoverNavigationEvent()
    data class NavigateToCategoryVideoList(val categoryId: String, val categoryName: String): DiscoverNavigationEvent()
    object NavigateToAddWorkoutCategory : DiscoverNavigationEvent()
    object NavigateToAddExercise : DiscoverNavigationEvent()
    object NavigateToAddVideo : DiscoverNavigationEvent()
}

class DiscoverViewModel : ViewModel() {

    private val workoutRepository = WorkoutRepository()
    private val videoRepository = VideoRepository()

    private val _uiState = MutableStateFlow(DiscoverUiState())
    val uiState: StateFlow<DiscoverUiState> = _uiState.asStateFlow()

    private val _navigationEvent = MutableLiveData<Event<DiscoverNavigationEvent>>()
    val navigationEvent: LiveData<Event<DiscoverNavigationEvent>> = _navigationEvent

    private var loadAllDataJob: Job? = null

    init {
        loadInitialData()
    }

    fun loadInitialData(isRefresh: Boolean = false) {
        if (_uiState.value.isOverallLoading && !isRefresh) return
        if (_uiState.value.isRefreshing && isRefresh) return

        loadAllDataJob?.cancel()

        _uiState.update {
            it.copy(
                isLoadingCategories = true, // Boshida kategoriyalar yuklanadi
                isLoadingWorkouts = true,   // Ularga qarab workoutlar
                isLoadingVideos = true,     // va videolar
                isRefreshing = isRefresh,
                errorMessage = null,
                searchResults = null,
                videoSearchResults = null,
                categories = it.categories,
                workoutCategorySections = it.workoutCategorySections,
                videoCategorySections = it.videoCategorySections
            )
        }

        loadAllDataJob = viewModelScope.launch {
            // 1. Kategoriyalarni yuklash
            when (val categoryResult = workoutRepository.getWorkoutCategoriesFlow().firstOrNull { it !is DataWrapper.Loading }) {
                is DataWrapper.Success -> {
                    val categories = categoryResult.data
                    _uiState.update { it.copy(isLoadingCategories = false, categories = categories) }

                    if (categories.isNotEmpty()) {
                        // 2. Har bir kategoriya uchun workout va videolarni parallel yuklash
                        val workoutLoadingJobs = mutableListOf<Job>()
                        val videoLoadingJobs = mutableListOf<Job>()

                        val tempWorkoutsByCategory = mutableMapOf<String, List<Workout>>()
                        val tempVideosByCategory = mutableMapOf<String, List<Video>>()

                        categories.forEach { category ->
                            workoutLoadingJobs.add(launch {
                                try {
                                    val result = workoutRepository.getWorkoutsByCategoryFlow(category.id, limit = 6)
                                        .first { it !is DataWrapper.Loading }

                                    if (result is DataWrapper.Success && result.data.isNotEmpty()) {
                                        tempWorkoutsByCategory[category.id] = result.data
                                    }

                                    updateWorkoutSections(categories, tempWorkoutsByCategory)
                                } catch (e: Exception) {
                                    handleException(e, "LoadWorkoutsForCat_${category.id}")
                                }
                            })

                            videoLoadingJobs.add(launch {
                                try {
                                    val result = videoRepository.getVideosByWorkoutCategoryFlow(category.id)
                                        .first { it !is DataWrapper.Loading }

                                    if (result is DataWrapper.Success && result.data.isNotEmpty()) {
                                        tempVideosByCategory[category.id] = result.data
                                    }

                                    updateVideoSections(categories, tempVideosByCategory)
                                } catch (e: Exception) {
                                    handleException(e, "LoadVideosForWorkoutCat_${category.id}")
                                }
                            })
                        }

                        // Barcha workout va video yuklash joblari tugashini kutish
                        // Bu joyda joinAll() ishlatish UI ni bloklashi mumkin,
                        // chunki collectLatest har bir natija kelganda UI'ni yangilaydi.
                        // Loading state'larni individual boshqarish yaxshiroq.
                        // Biz onCompletion orqali umumiy loadingni tugatamiz.
                        val allSubJobs = workoutLoadingJobs + videoLoadingJobs
                        if (allSubJobs.isNotEmpty()) {
                            try {
                                allSubJobs.joinAll()
                            } finally {
                                _uiState.update { it.copy(isLoadingWorkouts = false, isLoadingVideos = false, isRefreshing = false) }
                            }
                        } else {
                            _uiState.update { it.copy(isLoadingWorkouts = false, isLoadingVideos = false, isRefreshing = false) }
                        }

                    } else { // Kategoriyalar yo'q
                        _uiState.update { it.copy(isLoadingWorkouts = false, isLoadingVideos = false, isRefreshing = false) }
                    }
                }
                is DataWrapper.Error -> {
                    _uiState.update { it.copy(isLoadingCategories = false, isLoadingWorkouts = false, isLoadingVideos = false, isRefreshing = false, errorMessage = (it.errorMessage ?: "") + "\n" + categoryResult.error) }
                }
                null -> { // Flow biror narsa qaytarmasa (kamdan-kam holat)
                    _uiState.update { it.copy(isLoadingCategories = false, isLoadingWorkouts = false, isLoadingVideos = false, isRefreshing = false, errorMessage = "Kategoriyalarni yuklashda noma'lum xatolik") }
                }
                else -> {} // Loading
            }
        }
    }

    private fun updateWorkoutSections(allCategories: List<WorkoutCategory>, workoutsMap: Map<String, List<Workout>>) {
        val sections = allCategories.mapNotNull { category ->
            workoutsMap[category.id]?.let { workouts ->
                if (workouts.isNotEmpty()) {
                    Pair(category, workouts)
                } else null
            }
        }
        _uiState.update { it.copy(workoutCategorySections = sections, isLoadingWorkouts = false) }
    }

    private fun updateVideoSections(allCategories: List<WorkoutCategory>, videosMap: Map<String, List<Video>>) {
        val sections = allCategories.mapNotNull { category ->
            videosMap[category.id]?.let { videos ->
                if (videos.isNotEmpty()) {
                    Pair(category, videos) // Adapter WorkoutCategory va List<Video> kutadi
                } else null
            }
        }
        _uiState.update { it.copy(videoCategorySections = sections, isLoadingVideos = false) }
    }


    fun search(query: String) {
        loadAllDataJob?.cancel()
        if (query.isBlank()) {
            clearSearch()
            return
        }
        _uiState.update { it.copy(isLoadingWorkouts = true, isLoadingVideos = false, searchResults = emptyList(), videoSearchResults = null, errorMessage = null, isLoadingCategories = false, isRefreshing = false) }

        viewModelScope.launch {
            val workoutSearchDeferred = async { workoutRepository.searchWorkoutsOnce(query) }
            // TODO: VideoRepository.searchVideosOnce(query)
            // val videoSearchDeferred = async { videoRepository.searchVideosOnce(query) }

            val workoutResult = workoutSearchDeferred.await()
            // val videoResult = videoSearchDeferred.await()

            var workoutData: List<Workout> = emptyList()
            var combinedError: String? = null

            when (workoutResult) {
                is DataWrapper.Success -> workoutData = workoutResult.data
                is DataWrapper.Error -> combinedError = "Workout qidiruv xatosi: ${workoutResult.error}"
                else -> {}
            }
            // TODO: videoResult
            _uiState.update { it.copy(isLoadingWorkouts = false, isLoadingVideos = false, searchResults = workoutData, errorMessage = combinedError) }
        }
    }


    fun clearSearch() {
        _uiState.update { it.copy(searchResults = null, videoSearchResults = null) }
        loadInitialData()
    }

    fun onWorkoutClicked(workoutId: String) {
        _navigationEvent.value = Event(DiscoverNavigationEvent.NavigateToWorkoutDetail(workoutId))
    }
    fun onVideoClicked(video: Video) {
        _navigationEvent.value = Event(DiscoverNavigationEvent.NavigateToVideoDetail(video))
    }

    fun onSeeAllWorkoutsClicked(category: WorkoutCategory) {
        _navigationEvent.value = Event(DiscoverNavigationEvent.NavigateToCategoryWorkoutList(category.id, category.name))
    }

    fun onSeeAllVideosClicked(categoryId: String, categoryName: String) {
        _navigationEvent.value = Event(DiscoverNavigationEvent.NavigateToCategoryVideoList(categoryId, categoryName))
    }

    fun onAddWorkoutClicked() { _navigationEvent.value = Event(DiscoverNavigationEvent.NavigateToAddWorkout) }
    fun onAddWorkoutCategoryClicked() { _navigationEvent.value = Event(DiscoverNavigationEvent.NavigateToAddWorkoutCategory) }
    fun onAddExerciseClicked() { _navigationEvent.value = Event(DiscoverNavigationEvent.NavigateToAddExercise) }
    fun onAddVideoClicked() { _navigationEvent.value = Event(DiscoverNavigationEvent.NavigateToAddVideo) }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    override fun onCleared() {
        super.onCleared()
        loadAllDataJob?.cancel()
    }
}