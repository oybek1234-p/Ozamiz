package uz.oybek.ozamiz.ui.feature.menu.category

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import uz.oybek.ozamiz.data.menu.MenuRepository
import uz.oybek.ozamiz.data.menu.model.MenuCategory

data class CategoryUiState(
    val categories: List<MenuCategory> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)

class CategoryManagerViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(CategoryUiState())
    val uiState = _uiState.asStateFlow()

    init {
        loadCategories()
    }

    private fun loadCategories() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            MenuRepository.getCategories().collectLatest { list ->
                _uiState.value = _uiState.value.copy(categories = list, isLoading = false)
            }
        }
    }

    fun addCategory(name: String, isSecondary: Boolean) {
        if (name.isBlank()) return
        viewModelScope.launch {
            MenuRepository.addCategory(name.trim(), isSecondary)
            // No need to reload manually, flow will update
        }
    }

    fun deleteCategory(id: String) {
        viewModelScope.launch {
            MenuRepository.deleteCategory(id)
        }
    }

    fun updateCategory(category: MenuCategory) {
        viewModelScope.launch {
            MenuRepository.updateCategory(category)
        }
    }
}
