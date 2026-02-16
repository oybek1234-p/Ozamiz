package uz.oybek.ozamiz.ui.feature.menu.list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import uz.oybek.ozamiz.data.menu.MenuRepository
import uz.oybek.ozamiz.data.menu.model.Menu
import uz.oybek.ozamiz.data.menu.model.MenuCategory
import uz.oybek.ozamiz.data.utils.DataWrapper

data class MenuListUiState(
    val primaryCategories: List<MenuCategory> = emptyList(),
    val secondaryCategories: List<MenuCategory> = emptyList(),
    val menus: List<Menu> = emptyList(),
    val selectedPrimaryId: String = "",
    val selectedSecondaryId: String = "",
    val isLoading: Boolean = false
)

class MenuListViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(MenuListUiState())
    val uiState = _uiState.asStateFlow()
    private var searchJob: Job? = null

    private var allMenus: List<Menu> = emptyList()

    init {
        loadCategories()
        loadMenus()
    }

    private fun loadCategories() {
        viewModelScope.launch {
            MenuRepository.getCategories().collectLatest { list ->
                val primary = list.filter { !it.isSecondary }
                val secondary = list.filter { it.isSecondary }
                
                var selectedPrimaryId = _uiState.value.selectedPrimaryId
                if (selectedPrimaryId.isEmpty() && primary.isNotEmpty()) {
                    selectedPrimaryId = primary.find { it.name.lowercase().contains("nonushta") }?.id ?: primary.first().id
                }

                var selectedSecondaryId = _uiState.value.selectedSecondaryId
                
                _uiState.value = _uiState.value.copy(
                    primaryCategories = primary,
                    secondaryCategories = secondary,
                    selectedPrimaryId = selectedPrimaryId,
                    selectedSecondaryId = selectedSecondaryId
                )
                applyFilters()
            }
        }
    }

    private fun loadMenus() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            MenuRepository.getMenus().collectLatest { list ->
                allMenus = list
                applyFilters()
            }
        }
    }

    fun onPrimaryCategorySelected(categoryId: String) {
        _uiState.value = _uiState.value.copy(selectedPrimaryId = categoryId)
        applyFilters()
    }

    fun onSecondaryCategorySelected(categoryId: String) {
        val current = _uiState.value.selectedSecondaryId
        val next = if (current == categoryId) "" else categoryId
        _uiState.value = _uiState.value.copy(selectedSecondaryId = next)
        applyFilters()
    }

    private fun applyFilters() {
        val state = _uiState.value
        val filtered = allMenus.filter { menu ->
            val matchesPrimary = state.selectedPrimaryId.isEmpty() || menu.categoryIds.contains(state.selectedPrimaryId)
            val matchesSecondary = state.selectedSecondaryId.isEmpty() || menu.categoryIds.contains(state.selectedSecondaryId)
            matchesPrimary && matchesSecondary
        }
        _uiState.value = _uiState.value.copy(menus = filtered, isLoading = false)
    }

    fun onSearchQuery(query: String) {
        searchJob?.cancel()
        if (query.isBlank()) {
            applyFilters()
            return
        }
        searchJob = viewModelScope.launch {
            delay(500)
            _uiState.value = _uiState.value.copy(isLoading = true)
            val result = MenuRepository.searchMenus(query)
            if (result is DataWrapper.Success) {
                val searchResult = result.data
                val state = _uiState.value
                val filtered = searchResult.filter { menu ->
                    val matchesPrimary = state.selectedPrimaryId.isEmpty() || menu.categoryIds.contains(state.selectedPrimaryId)
                    val matchesSecondary = state.selectedSecondaryId.isEmpty() || menu.categoryIds.contains(state.selectedSecondaryId)
                    matchesPrimary && matchesSecondary
                }
                _uiState.value = _uiState.value.copy(menus = filtered, isLoading = false)
            }
        }
    }

    fun refresh() {
        loadMenus()
    }
}
