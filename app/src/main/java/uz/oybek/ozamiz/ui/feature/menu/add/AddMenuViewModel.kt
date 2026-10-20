package uz.oybek.ozamiz.ui.feature.menu.add

import android.net.Uri
import androidx.annotation.Keep
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import uz.oybek.ozamiz.common.result.BaseResult
import uz.oybek.ozamiz.data.food.model.FoodTag
import uz.oybek.ozamiz.data.menu.api.MenuRepo
import uz.oybek.ozamiz.data.menu.model.CreateMenuRequest
import uz.oybek.ozamiz.data.menu.model.Menu
import uz.oybek.ozamiz.data.menu.model.MenuDifficulty
import uz.oybek.ozamiz.data.menu.model.MenuItemFood
import javax.inject.Inject

@Keep
data class AddMenuUiState(
    val isLoading: Boolean = false,
    val isSaved: Boolean = false,
    val savedMenu: Menu? = null,
    val isValidationError: Boolean = false,
    val selectedTags: Set<FoodTag> = emptySet(),
    val errorMessage: String? = null
)

sealed interface AddMenuUiEffect {
    data object SaveSuccess : AddMenuUiEffect
    data class SaveError(val error: String?) : AddMenuUiEffect
}

@HiltViewModel
class AddMenuViewModel @Inject constructor(
    private val menuRepo: MenuRepo
) : ViewModel() {

    private val _uiState = MutableStateFlow(AddMenuUiState())
    val uiState: StateFlow<AddMenuUiState> = _uiState.asStateFlow()

    private val _uiEffect = MutableSharedFlow<AddMenuUiEffect>()
    val uiEffect: SharedFlow<AddMenuUiEffect> = _uiEffect.asSharedFlow()

    companion object {
        private val MEAL_TAGS = setOf(
            FoodTag.BREAKFAST,
            FoodTag.LUNCH,
            FoodTag.DINNER,
            FoodTag.SNACK
        )
    }

    fun resetState() {
        _uiState.value = AddMenuUiState()
    }

    fun toggleTag(tag: FoodTag) {
        if (tag == FoodTag.VEGETABLES_GREENS) return // Keraksiz tag

        _uiState.update { state ->
            val updated = state.selectedTags.toMutableSet()
            if (updated.contains(tag)) {
                updated.remove(tag)
            } else {
                // Qat'iy qoida: agar ovqatlanish mahali tanlansa, boshqa har qanday mahal olib tashlanadi
                if (MEAL_TAGS.contains(tag)) {
                    updated.removeAll(MEAL_TAGS)
                }

                // Sho'rva va Salat bir-birini inkor qiladi
                if (tag == FoodTag.SOUP) {
                    updated.remove(FoodTag.SALAD_APPETIZER)
                }
                if (tag == FoodTag.SALAD_APPETIZER) {
                    updated.remove(FoodTag.SOUP)
                }

                updated.add(tag)
            }
            state.copy(selectedTags = updated)
        }
    }

    fun setSelectedTags(tags: Set<FoodTag>) {
        val sanitized = tags.toMutableSet().apply {
            remove(FoodTag.VEGETABLES_GREENS)
            val selectedMeals = intersect(MEAL_TAGS)
            if (selectedMeals.size > 1) {
                val singleMeal = selectedMeals.first()
                removeAll(MEAL_TAGS)
                add(singleMeal)
            }
            if (contains(FoodTag.SOUP)) {
                remove(FoodTag.SALAD_APPETIZER)
            }
        }
        _uiState.update { it.copy(selectedTags = sanitized) }
    }

    fun saveMenu(
        menuId: String? = null,
        name: String,
        description: String,
        isRecipe: Boolean,
        items: List<MenuItemFood>,
        steps: List<String>,
        prepTimeMinutes: Int?,
        difficulty: MenuDifficulty = MenuDifficulty.EASY,
        imageUri: Uri?,
        existingImageUrl: String?
    ) {
        if (name.isBlank() || items.isEmpty()) {
            _uiState.update { it.copy(isValidationError = true) }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, isValidationError = false) }

            val request = CreateMenuRequest(
                id = menuId,
                name = name,
                description = description,
                isRecipe = isRecipe,
                items = items,
                steps = steps,
                prepTimeMinutes = prepTimeMinutes,
                tags = _uiState.value.selectedTags.map { it.name },
                difficulty = difficulty,
                imageUri = imageUri,
                existingImageUrl = existingImageUrl
            )

            when (val result = menuRepo.saveMenu(request)) {
                is BaseResult.Success -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            isSaved = true,
                            savedMenu = result.data,
                            errorMessage = null
                        )
                    }
                    _uiEffect.emit(AddMenuUiEffect.SaveSuccess)
                }
                is BaseResult.Failure -> {
                    val errorMsg = result.error?.message
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = errorMsg
                        )
                    }
                    _uiEffect.emit(AddMenuUiEffect.SaveError(errorMsg))
                }
            }
        }
    }
}
