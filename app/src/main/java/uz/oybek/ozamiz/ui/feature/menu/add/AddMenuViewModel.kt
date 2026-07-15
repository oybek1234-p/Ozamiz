package uz.oybek.ozamiz.ui.feature.menu.add

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import uz.oybek.ozamiz.data.menu.MenuRepository
import uz.oybek.ozamiz.data.menu.model.Menu
import uz.oybek.ozamiz.data.menu.model.MenuCategory
import uz.oybek.ozamiz.data.utils.DataWrapper

data class AddMenuUiState(
    val categories: List<MenuCategory> = emptyList(),
    val isLoading: Boolean = false,
    val isSaved: Boolean = false,
    val isValidationError: Boolean = false
)

class AddMenuViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(AddMenuUiState())
    val uiState = _uiState.asStateFlow()

    init {
        loadCategories()
    }

    private fun loadCategories() {
        viewModelScope.launch {
            MenuRepository.getCategories().collectLatest {
                _uiState.value = _uiState.value.copy(categories = it)
            }
        }
    }

    fun saveMenu(
        name: String,
        description: String,
        ingredients: String,
        calories: String,
        protein: String,
        fat: String,
        carbs: String,
        selectedCategoryIds: List<String>,
        imageUri: Uri?,
        type: String,
        preparationMethod: String,
        about: String,
        richIn: String,
        benefits: String,
        consumptionTime: String,
        preparationTime: String,
        difficulty: String
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
                type = type,
                preparationMethod = preparationMethod,
                about = about,
                richIn = richIn,
                benefits = benefits,
                consumptionTime = consumptionTime,
                preparationTime = preparationTime.toIntOrNull() ?: 15,
                difficulty = difficulty.ifEmpty { "Oson" }
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
