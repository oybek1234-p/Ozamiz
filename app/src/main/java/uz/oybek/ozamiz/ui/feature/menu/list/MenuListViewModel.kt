package uz.oybek.ozamiz.ui.feature.menu.list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import uz.oybek.ozamiz.data.menu.MenuRepository
import uz.oybek.ozamiz.data.menu.model.Menu
import uz.oybek.ozamiz.data.menu.model.MenuCategory

data class MenuListUiState(
    val categories: List<MenuCategory> = emptyList(),
    val menus: List<Menu> = emptyList(),
    val filteredMenus: List<Menu> = emptyList(),
    val selectedMealCategoryId: String? = null,
    val selectedNutritionCategoryId: String? = null,
    val selectedCalorieRange: Pair<Int, Int>? = null,
    val selectedDifficulty: String? = null,
    val selectedType: String = "menu",
    val isLoading: Boolean = false
)

class MenuListViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(MenuListUiState())
    val uiState = _uiState.asStateFlow()

    init {
        loadData()
    }

    private fun loadData() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            MenuRepository.getCategories().collectLatest { cats ->
                _uiState.value = _uiState.value.copy(categories = cats)
                updateFilteredList()
            }
        }
        viewModelScope.launch {
            MenuRepository.getMenus().collectLatest { allMenus ->
                _uiState.value = _uiState.value.copy(menus = allMenus)
                updateFilteredList()
            }
        }
    }

    fun selectMealCategory(categoryId: String?) {
        val currentMeal = _uiState.value.selectedMealCategoryId
        val next = if (currentMeal == categoryId) null else categoryId
        _uiState.value = _uiState.value.copy(selectedMealCategoryId = next)
        updateFilteredList()
    }

    fun selectNutritionCategory(categoryId: String?) {
        val currentNutr = _uiState.value.selectedNutritionCategoryId
        val next = if (currentNutr == categoryId) null else categoryId
        _uiState.value = _uiState.value.copy(selectedNutritionCategoryId = next)
        updateFilteredList()
    }

    fun selectCalorieRange(range: Pair<Int, Int>?) {
        val currentRange = _uiState.value.selectedCalorieRange
        val next = if (currentRange == range) null else range
        _uiState.value = _uiState.value.copy(selectedCalorieRange = next)
        updateFilteredList()
    }

    fun selectDifficulty(difficulty: String?) {
        val currentDiff = _uiState.value.selectedDifficulty
        val next = if (currentDiff == difficulty) null else difficulty
        _uiState.value = _uiState.value.copy(selectedDifficulty = next)
        updateFilteredList()
    }

    fun clearAllFilters() {
        _uiState.value = _uiState.value.copy(
            selectedMealCategoryId = null,
            selectedNutritionCategoryId = null,
            selectedCalorieRange = null,
            selectedDifficulty = null
        )
        updateFilteredList()
    }

    fun selectType(type: String) {
        _uiState.value = _uiState.value.copy(selectedType = type)
        updateFilteredList()
    }

    private fun updateFilteredList() {
        val state = _uiState.value
        val result = state.menus.filter { item ->
            val typeMatch = item.type == state.selectedType
            val mealMatch = state.selectedMealCategoryId == null || item.categoryIds.contains(state.selectedMealCategoryId)
            val nutritionMatch = state.selectedNutritionCategoryId == null || item.categoryIds.contains(state.selectedNutritionCategoryId)
            
            val calorieMatch = if (state.selectedCalorieRange != null) {
                item.calories >= state.selectedCalorieRange.first && item.calories <= state.selectedCalorieRange.second
            } else true

            val difficultyMatch = if (state.selectedDifficulty != null) {
                item.difficulty.equals(state.selectedDifficulty, ignoreCase = true)
            } else true

            typeMatch && mealMatch && nutritionMatch && calorieMatch && difficultyMatch
        }
        _uiState.value = _uiState.value.copy(filteredMenus = result, isLoading = false)
    }
}
