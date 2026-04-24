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

class AdminUsersListViewModel : ViewModel() {

    private val _users = MutableStateFlow<List<User>>(emptyList())
    private val _isPremiumFilterActive = MutableStateFlow(false)
    val isPremiumFilterActive = _isPremiumFilterActive.asStateFlow()

    val users: StateFlow<List<User>> = combine(_users, _isPremiumFilterActive) { users, isFilterActive ->
        if (isFilterActive) {
            users.filter { it.premium }
        } else {
            users
        }
    }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    init {
        loadUsers()
    }

    private fun loadUsers() {
        _isLoading.value = true
        UserRepository.loadAllUsers(viewModelScope) { list ->
            // Sort by lastSeenTime descending (newest first)
            val sortedList = list.sortedByDescending { it.lastSeenTime }
            _users.value = sortedList
            _isLoading.value = false
        }
    }
    
    fun togglePremiumFilter(isActive: Boolean) {
        _isPremiumFilterActive.value = isActive
    }

    fun refresh() {
        loadUsers()
    }
}

