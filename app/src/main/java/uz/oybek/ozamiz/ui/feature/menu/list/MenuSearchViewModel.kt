package uz.oybek.ozamiz.ui.feature.menu.list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import androidx.annotation.Keep
import uz.oybek.ozamiz.data.menu.MenuRepository
import uz.oybek.ozamiz.data.menu.model.Menu
import uz.oybek.ozamiz.common.result.BaseResult

class MenuSearchViewModel : ViewModel() {

    @Keep
    data class MenuSearchState(
        val categoryId: String? = null,
        val query: String? = null,
        val loading: Boolean = false,
        val list: List<Menu> = emptyList()
    )

    // Keshni xavfsiz va toza saqlash uchun oddiy MutableMap
    private val cacheMap = mutableMapOf<String, List<Menu>>()
    private var searchJob: Job? = null

    private val _state = MutableStateFlow(MenuSearchState())
    val state: StateFlow<MenuSearchState> = _state.asStateFlow()

    fun setCategory(categoryId: String?) {
        if (_state.value.categoryId != categoryId) {
            searchJob?.cancel()

            // Yangi kategoriya tanlanganda keshdan eski ma'lumotlarni tekshiramiz
            val cachedList = cacheMap[categoryId ?: ""] ?: emptyList()

            _state.update {
                it.copy(
                    categoryId = categoryId,
                    query = null,
                    list = cachedList, // Agar keshda bo'lsa darhol ko'rsatamiz, bo'lmasa bo'sh bo'ladi
                    loading = false
                )
            }

            // Faqat keshda ma'lumot umuman bo'lmagandagina tarmoqdan yuklaymiz
            if (cachedList.isEmpty()) {
                loadList()
            }
        }
    }

    private fun loadList() {
        val catId = _state.value.categoryId ?: return

        if (_state.value.loading) return

        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            _state.update { it.copy(loading = true) }

            val currentList = _state.value.list
            val lastMenuId = currentList.lastOrNull()?.id
            val lastTimeStamp = currentList.lastOrNull()?.createdAt?.time

            val result = MenuRepository.loadMenus(
                categoryId = catId,
                lastMenuId = lastMenuId,
                lastTimestamp = lastTimeStamp,
                pageSize = 10
            )

            if (result is BaseResult.Success) {
                val newList = currentList + result.data
                cacheMap[catId] = newList

                _state.update {
                    it.copy(
                        list = newList,
                        loading = false
                    )
                }
            } else {
                _state.update { it.copy(loading = false) }
            }
        }
    }

    fun loadNext() {
        loadList()
    }

    fun setSearchQuery(query: String?) {
        if (_state.value.query != query) {
            searchJob?.cancel()
            _state.update {
                it.copy(query = query, categoryId = null, list = emptyList(), loading = false)
            }
        }
    }

    fun onMenuUpdated(updatedMenu: Menu) {
        for (entry in cacheMap.entries) {
            cacheMap[entry.key] = entry.value.map {
                if (it.id == updatedMenu.id) updatedMenu else it
            }
        }
        _state.update { currentState ->
            val updatedList = currentState.list.map {
                if (it.id == updatedMenu.id) updatedMenu else it
            }
            currentState.copy(list = updatedList)
        }
    }

    fun onMenuDeleted(deletedMenuId: String) {
        for (entry in cacheMap.entries) {
            cacheMap[entry.key] = entry.value.filter { it.id != deletedMenuId }
        }
        _state.update { currentState ->
            val updatedList = currentState.list.filter { it.id != deletedMenuId }
            currentState.copy(list = updatedList)
        }
    }
}