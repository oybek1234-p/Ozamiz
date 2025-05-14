package uz.oybek.ozamiz.ui.discover

import android.net.Uri
import uz.oybek.ozamiz.data.workouts.Exercise

data class AddEditExerciseUiState(
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val errorMessage: String? = null,
    val exerciseToEdit: Exercise? = null,
    val title: String = "Yangi Mashq Qo'shish",
    val canDelete: Boolean = false,
    val saveSuccess: Boolean = false,
    val deleteSuccess: Boolean = false,
    val mediaUriToDisplay: Uri? = null // Faqat UI uchun
)