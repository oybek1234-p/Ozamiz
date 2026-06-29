package uz.oybek.ozamiz.ui.feature.menu.detail

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import androidx.lifecycle.viewModelScope
import uz.oybek.ozamiz.data.menu.MenuRepository
import uz.oybek.ozamiz.data.utils.DataWrapper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class MenuDetailViewModel : ViewModel() {

    private val _deleteSuccess = MutableSharedFlow<Boolean>()
    val deleteSuccess = _deleteSuccess.asSharedFlow()

    fun deleteMenu(menuId: String) {
        viewModelScope.launch {
            val result = MenuRepository.deleteMenu(menuId)
            if (result is DataWrapper.Success) {
                _deleteSuccess.emit(true)
            }
        }
    }
}
