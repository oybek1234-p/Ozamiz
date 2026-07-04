package uz.oybek.ozamiz.ui.feature.menu.list

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

data class MenuListUiState(
    val isLoading: Boolean = false
)

class MenuListViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(MenuListUiState())
    val uiState = _uiState.asStateFlow()
}
