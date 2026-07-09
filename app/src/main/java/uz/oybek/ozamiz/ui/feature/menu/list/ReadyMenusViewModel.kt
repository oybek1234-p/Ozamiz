package uz.oybek.ozamiz.ui.feature.menu.list

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import uz.oybek.ozamiz.data.menu.MenuRepository
import uz.oybek.ozamiz.data.menu.model.Menu
import uz.oybek.ozamiz.data.menu.model.MenuCategory

class ReadyMenusViewModel : ViewModel() {

    private val _menus = MutableLiveData<List<Menu>>()
    val menus: LiveData<List<Menu>> = _menus

    private val _categories = MutableLiveData<List<MenuCategory>>()
    val categories: LiveData<List<MenuCategory>> = _categories

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    init {
        loadCategories()
        loadMenus()
    }

    private fun loadCategories() {
        viewModelScope.launch {
            MenuRepository.getCategories().collectLatest {
                _categories.value = it
            }
        }
    }

    fun loadMenus(categoryId: String? = null) {

        viewModelScope.launch {
            _isLoading.value = true
            MenuRepository.getMenus(categoryId).collectLatest {
                _menus.value = it
                _isLoading.value = false
            }
        }
    }
}
