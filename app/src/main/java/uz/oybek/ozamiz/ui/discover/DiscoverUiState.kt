package uz.oybek.ozamiz.ui.discover // Yoki mos paket

import uz.oybek.ozamiz.data.workouts.Workout
import uz.oybek.ozamiz.data.workouts.WorkoutCategory

data class DiscoverUiState(
    val isLoadingCategories: Boolean = false,
    val isLoadingWorkouts: Boolean = false,
    val isRefreshing: Boolean = false,
    val errorMessage: String? = null,
    val categories: List<WorkoutCategory> = emptyList(),
    val workoutsByCategory: Map<String, List<Workout>> = emptyMap(),
    val searchResults: List<Workout>? = null
) {
    val isOverallLoading: Boolean
        get() = (isLoadingCategories || isLoadingWorkouts) && !isRefreshing && searchResults == null
}
