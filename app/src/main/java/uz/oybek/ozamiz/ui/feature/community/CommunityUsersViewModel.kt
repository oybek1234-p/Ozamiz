package uz.oybek.ozamiz.ui.feature.community

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import uz.oybek.ozamiz.data.user.AdminRepository
import uz.oybek.ozamiz.data.user.model.User
import uz.oybek.ozamiz.data.user.model.UserSortType

data class CommunityUsersState(
    val users: List<User> = emptyList(),
    val isLoading: Boolean = false,
    val isPaging: Boolean = false,
    val hasReachedEnd: Boolean = false,
    val error: String? = null,
    val sortBy: UserSortType = UserSortType.MOST_POINTS
)

class CommunityUsersViewModel : ViewModel() {
    private val _state = MutableStateFlow(CommunityUsersState())
    val state: StateFlow<CommunityUsersState> = _state

    private var lastUserId: String? = null

    init {
        loadUsers()
    }

    fun setSortBy(sortBy: UserSortType) {
        if (_state.value.sortBy == sortBy) return
        _state.update { it.copy(sortBy = sortBy, users = emptyList(), hasReachedEnd = false) }
        lastUserId = null
        loadUsers()
    }

    fun refresh() {
        lastUserId = null
        _state.update { it.copy(hasReachedEnd = false) }
        loadUsers()
    }

    fun loadUsers() {
        if (_state.value.isLoading || _state.value.isPaging || _state.value.hasReachedEnd) return

        val isFirstLoad = lastUserId == null
        _state.update { it.copy(isLoading = isFirstLoad, isPaging = !isFirstLoad, error = null) }

        viewModelScope.launch {
            val result = AdminRepository.getUsers(
                limit = 15,
                lastUserId = lastUserId,
                sortBy = _state.value.sortBy
            )

            result.onSuccess { newUsers ->
                if (newUsers.isEmpty()) {
                    _state.update { it.copy(isLoading = false, isPaging = false, hasReachedEnd = true) }
                } else {
                    val users = if (isFirstLoad) newUsers else _state.value.users + newUsers
                    
                    lastUserId = newUsers.lastOrNull()?.uid

                    _state.update {
                        it.copy(
                            users = users,
                            isLoading = false,
                            isPaging = false,
                            hasReachedEnd = newUsers.size < 15
                        )
                    }
                }
            }.onFailure { e ->
                _state.update { it.copy(isLoading = false, isPaging = false, error = e.localizedMessage) }
            }
        }
    }
}
