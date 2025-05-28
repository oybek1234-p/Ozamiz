package uz.oybek.ozamiz.ui.discover

import uz.oybek.ozamiz.data.workouts.Video
import uz.oybek.ozamiz.data.workouts.Workout
import uz.oybek.ozamiz.data.workouts.WorkoutCategory

data class DiscoverUiState(
    val isLoadingCategories: Boolean = false,
    val isLoadingWorkouts: Boolean = false,
    val isLoadingVideos: Boolean = false,
    val isRefreshing: Boolean = false,
    val errorMessage: String? = null,
    val categories: List<WorkoutCategory> = emptyList(), // Barcha yuklangan kategoriyalar
    val workoutCategorySections: List<Pair<WorkoutCategory, List<Workout>>> = emptyList(), // Faqat workouti bor kategoriyalar
    val videoCategorySections: List<Pair<WorkoutCategory, List<Video>>> = emptyList(),     // Faqat videosi bor kategoriyalar
    val searchResults: List<Workout>? = null,
    val videoSearchResults: List<Video>? = null
) {
    val isOverallLoading: Boolean
        get() = (isLoadingCategories || isLoadingWorkouts || isLoadingVideos) && !isRefreshing && searchResults == null && videoSearchResults == null
}