package uz.oybek.ozamiz.ui.feature.admin.users

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import uz.oybek.ozamiz.data.user.AdminRepository
import uz.oybek.ozamiz.data.user.model.User
import uz.oybek.ozamiz.data.user.model.UserSortType

class AdminUsersListViewModel : ViewModel() {

    private val _users = MutableStateFlow<List<User>>(emptyList())
    private val _isPremiumFilterActive = MutableStateFlow(false)
    val isPremiumFilterActive = _isPremiumFilterActive.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    val users: StateFlow<List<User>> =
        combine(_users, _searchQuery) { users, query ->
            if (query.isNotEmpty()) {
                users.filter {
                    it.name.contains(query, ignoreCase = true) ||
                            it.gmail.contains(query, ignoreCase = true) ||
                            it.uid.contains(query, ignoreCase = true)
                }
            } else {
                users
            }
        }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private var lastUserId: String? = null
    private var isLastPage = false

    init {
        loadUsers()
    }

    fun loadUsers(isRefresh: Boolean = false) {
        if (_isLoading.value) return
        if (isRefresh) {
            lastUserId = null
            isLastPage = false
        }
        if (isLastPage && !isRefresh) return

        _isLoading.value = true
        viewModelScope.launch {
            AdminRepository.getUsersFromDatabase(
                limit = 20,
                lastUserId = lastUserId,
                sortBy = UserSortType.RECENT_ACTIVITY,
                premiumOnly = _isPremiumFilterActive.value
            ).onSuccess { newList ->
                if (newList.isEmpty()) {
                    if (isRefresh) _users.value = emptyList()
                    isLastPage = true
                } else {
                    lastUserId = newList.last().uid
                    if (isRefresh) {
                        _users.value = newList
                    } else {
                        _users.value = _users.value + newList
                    }
                    if (newList.size < 20) isLastPage = true
                }
            }.onFailure {
                // Handle error
            }
            _isLoading.value = false
        }
    }

    fun loadNextPage() {
        if (!isLastPage && !_isLoading.value) {
            loadUsers()
        }
    }

    fun togglePremiumFilter(isActive: Boolean) {
        if (_isPremiumFilterActive.value != isActive) {
            _isPremiumFilterActive.value = isActive
            loadUsers(isRefresh = true)
        }
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }
}
