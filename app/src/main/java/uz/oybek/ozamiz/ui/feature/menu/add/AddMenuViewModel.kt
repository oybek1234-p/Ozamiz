package uz.oybek.ozamiz.ui.feature.menu.add

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import uz.oybek.ozamiz.data.firebase.FirebasePaths
import uz.oybek.ozamiz.data.firebase.uploader.ImageCompressor
import uz.oybek.ozamiz.data.firebase.uploader.ImageUploader
import uz.oybek.ozamiz.data.menu.FilterItem
import uz.oybek.ozamiz.data.menu.CalorieFilter
import uz.oybek.ozamiz.data.menu.MenuCategories
import uz.oybek.ozamiz.data.menu.MenuRepository
import uz.oybek.ozamiz.data.menu.model.Menu
import java.util.Date
import java.util.UUID

data class AddMenuUiState(
    val isLoading: Boolean = false,
    val isSaved: Boolean = false,
    val savedMenu: Menu? = null,
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

    fun resetState() {
        _uiState.value = AddMenuUiState()
        _selectedCategories.value = emptyList()
    }

    fun setSelectedCategories(categories: List<FilterItem>) {
        _selectedCategories.value = categories
    }

    fun saveMenu(
        menuId: String? = null,
        imageUrl: String? = null,
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
        servingSize: String,
        selectedCategoryIds: List<String>,
        imageUri: Uri?,
        type: String,
        preparationMethod: List<String>,
        about: String,
        preparationTime: String
    ) {
        if (name.isBlank() || selectedCategoryIds.isEmpty()) {
            _uiState.value = _uiState.value.copy(isValidationError = true)
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, isValidationError = false)

            val cals = calories.toIntOrNull() ?: 0
            val finalCategoryIds = selectedCategoryIds.filterNot { it.startsWith("CAL_") }.toMutableList()
            val matchingCalorieFilter = CalorieFilter.entries.find {
                cals >= it.min && cals <= it.max
            }
            if (matchingCalorieFilter != null) {
                finalCategoryIds.add(matchingCalorieFilter.name)
            }

            val menu = Menu(
                id = menuId ?: "",
                imageUrl = imageUrl,
                name = name,
                description = description,
                ingredients = ingredients,
                categoryIds = finalCategoryIds,
                calories = cals,
                protein = protein.toDoubleOrNull() ?: 0.0,
                fat = fat.toDoubleOrNull() ?: 0.0,
                carbs = carbs.toDoubleOrNull() ?: 0.0,
                fiber = fiber.toDoubleOrNull() ?: 0.0,
                sugar = sugar.toDoubleOrNull() ?: 0.0,
                salt = salt.toDoubleOrNull() ?: 0.0,
                servingSize = servingSize,
                type = type,
                preparationMethod = preparationMethod,
                about = about,
                preparationTime = preparationTime.toIntOrNull() ?: 15
            )

            val savedMenu = if (imageUri != null) {
                val id = menu.id.ifEmpty { UUID.randomUUID().toString() }
                val compressedUri = ImageCompressor.compressImageToUri(imageUri) ?: imageUri
                val photoUrl = ImageUploader.uploadImage(compressedUri, FirebasePaths.Storage.menuPhotos(id))
                menu.copy(id = id, imageUrl = photoUrl, createdAt = menu.createdAt ?: Date())
            } else {
                menu.copy(id = menu.id.ifEmpty { UUID.randomUUID().toString() }, createdAt = menu.createdAt ?: Date())
            }

            val result = MenuRepository.addMenu(savedMenu, null)
            _uiState.value = _uiState.value.copy(
                isLoading = false,
                isSaved = result,
                savedMenu = if (result) savedMenu else null
            )
        }
    }

    fun clearSavedState() {
        _uiState.value = _uiState.value.copy(isSaved = false, savedMenu = null)
    }

    fun clearValidationError() {
        _uiState.value = _uiState.value.copy(isValidationError = false)
    }
}
