package uz.oybek.ozamiz.ui.feature.admin.users

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import uz.oybek.ozamiz.data.user.UserRepository
import uz.oybek.ozamiz.data.user.model.User

class AdminUsersListViewModel : ViewModel() {

    private val _users = MutableStateFlow<List<User>>(emptyList())
    val users: StateFlow<List<User>> = _users.asStateFlow()

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
    
    fun refresh() {
        loadUsers()
    }
}
