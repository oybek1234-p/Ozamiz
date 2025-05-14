package uz.oybek.ozamiz.ui.foods // Foods paketida

import uz.oybek.ozamiz.data.foods.FoodCategory

// AddEditCategoryViewModel uchun UI state
data class AddEditCategoryUiState(
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val errorMessage: String? = null,
    val categoryToEdit: FoodCategory? = null,
    val title: String = "Yangi Kategoriya",
    val saveSuccess: Boolean = false,
    val deleteSuccess: Boolean = false, // O'chirish holati uchun
    val canDelete: Boolean = false      // O'chirish tugmasini ko'rsatish uchun
)