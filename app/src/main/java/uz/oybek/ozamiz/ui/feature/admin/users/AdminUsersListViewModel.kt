package uz.oybek.ozamiz.ui.feature.admin.users

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import uz.oybek.ozamiz.common.result.BaseResult
import uz.oybek.ozamiz.data.profile.model.User
import uz.oybek.ozamiz.data.user.api.AdminUserRepo
import uz.oybek.ozamiz.data.user.model.UserSortType
import javax.inject.Inject

data class AdminUsersListUiState(
    val users: List<User> = emptyList(),
    val isLoading: Boolean = false,
    val isPremiumFilterActive: Boolean = false,
    val searchQuery: String = "",
    val isLastPage: Boolean = false
)

@HiltViewModel
class AdminUsersListViewModel @Inject constructor(
    private val adminUserRepo: AdminUserRepo
) : ViewModel() {

    companion object {
        private const val PAGE_SIZE = 20
        private const val SEARCH_DEBOUNCE_MS = 350L
    }

    private val _uiState = MutableStateFlow(AdminUsersListUiState())
    val uiState: StateFlow<AdminUsersListUiState> = _uiState.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private var loadJob: Job? = null
    private var lastUserId: String? = null
    private var lastSeenTime: Long? = null
    private var lastName: String? = null
    private var isLastPage = false

    init {
        // Debounce search query to trigger server-side Firestore search without flooding
        viewModelScope.launch {
            _searchQuery
                .drop(1)
                .debounce(SEARCH_DEBOUNCE_MS)
                .distinctUntilChanged()
                .collect { query ->
                    _uiState.update { it.copy(searchQuery = query) }
                    loadUsers(isRefresh = true)
                }
        }
        loadUsers(isRefresh = true)
    }

    fun loadUsers(isRefresh: Boolean = false) {
        if (isRefresh) {
            loadJob?.cancel()
            lastUserId = null
            lastSeenTime = null
            lastName = null
            isLastPage = false
            _uiState.update { it.copy(isLoading = true, users = emptyList()) }
        } else {
            if (_uiState.value.isLoading || isLastPage) return
            _uiState.update { it.copy(isLoading = true) }
        }

        val currentQuery = _searchQuery.value.trim()
        val premiumOnly = _uiState.value.isPremiumFilterActive

        val job = viewModelScope.launch {
            val result = adminUserRepo.getUsers(
                limit = PAGE_SIZE,
                lastUserId = lastUserId,
                lastSeenTime = lastSeenTime,
                lastName = lastName,
                sortBy = UserSortType.RECENT_ACTIVITY,
                premiumOnly = premiumOnly,
                searchQuery = currentQuery.ifBlank { null }
            )

            when (result) {
                is BaseResult.Success -> {
                    val newList = result.data
                    if (newList.isEmpty()) {
                        isLastPage = true
                        _uiState.update {
                            it.copy(
                                users = if (isRefresh) emptyList() else it.users,
                                isLoading = false,
                                isLastPage = true
                            )
                        }
                    } else {
                        val lastItem = newList.last()
                        lastUserId = lastItem.uid
                        lastSeenTime = lastItem.lastSeenTime
                        lastName = lastItem.name
                        isLastPage = newList.size < PAGE_SIZE

                        _uiState.update { current ->
                            val currentIds = if (isRefresh) emptySet() else current.users.map { u -> u.uid }.toSet()
                            val newUniqueItems = newList.filter { u -> u.uid !in currentIds }

                            if (!isRefresh && newUniqueItems.isEmpty()) {
                                isLastPage = true
                            }

                            val updatedList = if (isRefresh) {
                                newList
                            } else {
                                current.users + newUniqueItems
                            }

                            current.copy(
                                users = updatedList,
                                isLoading = false,
                                isLastPage = isLastPage
                            )
                        }
                    }
                }
                is BaseResult.Failure -> {
                    _uiState.update {
                        it.copy(
                            users = if (isRefresh && currentQuery.isNotBlank()) emptyList() else it.users,
                            isLoading = false
                        )
                    }
                }
            }
        }
        loadJob = job
    }

    fun loadNextPage() {
        val state = _uiState.value
        if (!isLastPage && !state.isLoading && state.users.isNotEmpty()) {
            loadUsers(isRefresh = false)
        }
    }

    fun togglePremiumFilter(isActive: Boolean) {
        if (_uiState.value.isPremiumFilterActive != isActive) {
            _uiState.update { it.copy(isPremiumFilterActive = isActive) }
            loadUsers(isRefresh = true)
        }
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }
}
