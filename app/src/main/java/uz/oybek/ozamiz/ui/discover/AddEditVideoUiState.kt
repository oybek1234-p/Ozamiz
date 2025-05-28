package uz.oybek.ozamiz.ui.discover

import android.net.Uri
import uz.oybek.ozamiz.data.workouts.Video
import uz.oybek.ozamiz.data.workouts.WorkoutCategory // Import WorkoutCategory

data class AddEditVideoUiState(
    val isLoadingCategories: Boolean = false, // Yangi: Kategoriyalarni yuklash uchun
    val isSaving: Boolean = false,
    val errorMessage: String? = null,
    val videoToEdit: Video? = null,
    val availableCategories: List<WorkoutCategory> = emptyList(), // Yangi: Mavjud Workout kategoriyalari
    val selectedCategoryIds: List<String> = emptyList(),     // Yangi: Tanlangan kategoriya ID'lari
    val title: String = "Yangi Video Qo'shish",
    val canDelete: Boolean = false,
    val saveSuccess: Boolean = false,
    val deleteSuccess: Boolean = false,
    val thumbnailUriToDisplay: Uri? = null
)