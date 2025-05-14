package uz.oybek.ozamiz.ui.discover

import uz.oybek.ozamiz.data.workouts.Workout

data class WorkoutListUiState(
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
    val workouts: List<Workout> = emptyList(),
    val listTitle: String = ""
)