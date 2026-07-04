package uz.oybek.ozamiz.ui.feature.admin.users

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import uz.oybek.ozamiz.data.user.UserRepository
import uz.oybek.ozamiz.data.user.model.User
import uz.oybek.ozamiz.data.utils.DateUtils

class AdminUsersListViewModel : ViewModel() {

    private val _users = MutableStateFlow<List<User>>(emptyList())
    private val _isPremiumFilterActive = MutableStateFlow(false)
    val isPremiumFilterActive = _isPremiumFilterActive.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    val users: StateFlow<List<User>> =
        combine(_users, _isPremiumFilterActive, _searchQuery) { users, isFilterActive, query ->
            var filteredList = users
            if (isFilterActive) {
                filteredList = filteredList.filter { it.premium }
            }
            if (query.isNotEmpty()) {
                filteredList = filteredList.filter {
                    it.name.contains(query, ignoreCase = true) ||
                            it.gmail.contains(query, ignoreCase = true) ||
                            it.uid.contains(query, ignoreCase = true)
                }
            }
            filteredList
        }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    init {
        loadUsers()
    }

    private fun loadUsers() {
        _isLoading.value = true
        UserRepository.loadAllUsers(viewModelScope) { list ->
            _users.value = list.sortedByDescending {
                val timestampt = DateUtils.toLongTimestamp(it.lastSeenTime)
                timestampt
            }
            _isLoading.value = false
        }
    }

    fun togglePremiumFilter(isActive: Boolean) {
        _isPremiumFilterActive.value = isActive
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

}

