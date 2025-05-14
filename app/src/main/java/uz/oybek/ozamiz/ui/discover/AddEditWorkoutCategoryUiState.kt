package uz.oybek.ozamiz.ui.discover // Yoki mos paket

import uz.oybek.ozamiz.data.workouts.WorkoutCategory

data class AddEditWorkoutCategoryUiState(
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val errorMessage: String? = null,
    val categoryToEdit: WorkoutCategory? = null,
    val title: String = "Yangi Kategoriya Qo'shish",
    val saveSuccess: Boolean = false,
    val deleteSuccess: Boolean = false,
    val canDelete: Boolean = false
)