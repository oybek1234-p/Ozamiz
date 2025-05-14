package uz.oybek.ozamiz.ui.discover

import android.net.Uri
import uz.oybek.ozamiz.data.workouts.Exercise
import uz.oybek.ozamiz.data.workouts.Workout
import uz.oybek.ozamiz.data.workouts.WorkoutCategory

data class AddEditWorkoutUiState(
    val isLoading: Boolean = true, // Kategoriyalar va mashqlarni yuklash uchun
    val isSaving: Boolean = false,
    val errorMessage: String? = null,
    val workoutToEdit: Workout? = null,
    val allCategories: List<WorkoutCategory> = emptyList(),
    val allExercises: List<Exercise> = emptyList(),
    val selectedCategoryIds: List<String> = emptyList(),
    val selectedExerciseIds: List<String> = emptyList(), // Tanlangan mashqlar ID lari (tartiblangan)
    val title: String = "Yangi Workout Qo'shish",
    val canDelete: Boolean = false,
    val saveSuccess: Boolean = false,
    val deleteSuccess: Boolean = false,
    val imageUriToDisplay: Uri? = null // Faqat UI da ko'rsatish uchun
)