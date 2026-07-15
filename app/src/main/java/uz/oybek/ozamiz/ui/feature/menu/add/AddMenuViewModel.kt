package uz.oybek.ozamiz.ui.feature.menu.add

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import uz.oybek.ozamiz.data.menu.FilterItem
import uz.oybek.ozamiz.data.menu.MenuCategories
import uz.oybek.ozamiz.data.menu.MenuRepository
import uz.oybek.ozamiz.data.menu.model.Menu

data class AddMenuUiState(
    val isLoading: Boolean = false,
    val isSaved: Boolean = false,
    val isValidationError: Boolean = false
)

class AddMenuViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(AddMenuUiState())
    val uiState = _uiState.asStateFlow()

    private val _allCategories = MutableStateFlow<List<FilterItem>>(emptyList())
    val allCategories = _allCategories.asStateFlow()

    private val _selectedCategories = MutableStateFlow<List<FilterItem>>(emptyList())
    val selectedCategories = _selectedCategories.asStateFlow()

    init {
        loadCategories()
    }

    private fun loadCategories() {
        _allCategories.value = MenuCategories.mainMeals + 
            MenuCategories.methods + 
            MenuCategories.diets + 
            MenuCategories.ingredients
    }

    fun setSelectedCategories(categories: List<FilterItem>) {
        _selectedCategories.value = categories
    }

    fun saveMenu(
        name: String,
        description: String,
        ingredients: List<String>,
        calories: String,
        protein: String,
        fat: String,
        carbs: String,
        fiber: String,
        sugar: String,
        salt: String,
        selectedCategoryIds: List<String>,
        imageUri: Uri?,
        type: String,
        preparationMethod: List<String>,
        about: String,
        richIn: String,
        consumptionTime: String,
        preparationTime: String
    ) {
        if (name.isBlank() || selectedCategoryIds.isEmpty() || imageUri == null) {
            _uiState.value = _uiState.value.copy(isValidationError = true)
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, isValidationError = false)

            val menu = Menu(
                name = name,
                description = description,
                ingredients = ingredients,
                categoryIds = selectedCategoryIds,
                calories = calories.toIntOrNull() ?: 0,
                protein = protein.toDoubleOrNull() ?: 0.0,
                fat = fat.toDoubleOrNull() ?: 0.0,
                carbs = carbs.toDoubleOrNull() ?: 0.0,
                fiber = fiber.toDoubleOrNull() ?: 0.0,
                sugar = sugar.toDoubleOrNull() ?: 0.0,
                salt = salt.toDoubleOrNull() ?: 0.0,
                type = type,
                preparationMethod = preparationMethod,
                about = about,
                richIn = richIn,
                consumptionTime = consumptionTime,
                preparationTime = preparationTime.toIntOrNull() ?: 15
            )

            val result = MenuRepository.addMenu(menu, imageUri)
            _uiState.value = _uiState.value.copy(
                isLoading = false,
                isSaved = result
            )
        }
    }

    fun clearValidationError() {
        _uiState.value = _uiState.value.copy(isValidationError = false)
    }
}
