package uz.oybek.ozamiz.ui.feature.menu.list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import uz.oybek.ozamiz.common.result.BaseResult
import uz.oybek.ozamiz.data.food.model.FoodTag
import uz.oybek.ozamiz.data.menu.api.MenuRepo
import uz.oybek.ozamiz.data.menu.model.Menu
import uz.oybek.ozamiz.data.profile.api.ProfileRepo
import uz.oybek.ozamiz.data.profile.api.ProfileManagerRepo
import uz.oybek.ozamiz.data.profile.model.User
import javax.inject.Inject

data class MenuListUiState(
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val isLoadingMore: Boolean = false,
    val hasMore: Boolean = true,
    val isAdmin: Boolean = false,
    val selectedTag: FoodTag? = null,
    val searchQuery: String = "",
    val filterItems: List<MenuTagFilterItem> = emptyList(),
    val popularMenus: List<Menu> = emptyList(),
    val rawMenus: List<Menu> = emptyList(),
    val menus: List<Menu> = emptyList(),
    val errorMessage: String? = null
)

sealed interface MenuListUiEffect {
    data class NavigateToDetail(val menu: Menu) : MenuListUiEffect
    data object NavigateToAddMenu : MenuListUiEffect
    data object NavigateToLikedMenus : MenuListUiEffect
}

@HiltViewModel
class MenuListViewModel @Inject constructor(
    private val menuRepo: MenuRepo,
    private val profileRepo: ProfileRepo,
    private val profileManagerRepo: ProfileManagerRepo
) : ViewModel() {

    companion object {
        const val PAGE_SIZE = 6 // Internetdan timestamp bo'yicha 6 tadan o'qiladi
    }

    private val _uiState = MutableStateFlow(MenuListUiState())
    val uiState: StateFlow<MenuListUiState> = _uiState.asStateFlow()

    private val _uiEffect = MutableSharedFlow<MenuListUiEffect>()
    val uiEffect: SharedFlow<MenuListUiEffect> = _uiEffect.asSharedFlow()

    private val currentUser: User
        get() = profileRepo.currentUser

    private val currentUserId: String
        get() = currentUser.uid

    private var searchJob: Job? = null

    init {
        val initialTag = MenuTagFilters.getCurrentMealTag()
        val isAdminUser = profileRepo.currentUser.admin
        _uiState.update {
            it.copy(
                isAdmin = isAdminUser,
                selectedTag = initialTag,
                filterItems = MenuTagFilters.getFiltersForCurrentTime(initialTag)
            )
        }
        loadMenus(tag = initialTag, isRefresh = false)
    }

    fun onTagItemClicked(item: MenuTagFilterItem) {
        if (item.isLikedTab) {
            viewModelScope.launch {
                _uiEffect.emit(MenuListUiEffect.NavigateToLikedMenus)
            }
            return
        }
        selectTag(item.tag)
    }

    fun onLikedButtonClicked() {
        viewModelScope.launch {
            _uiEffect.emit(MenuListUiEffect.NavigateToLikedMenus)
        }
    }

    fun selectTag(tag: FoodTag?) {
        val targetTag = tag ?: MenuTagFilters.getCurrentMealTag()
        if (_uiState.value.selectedTag == targetTag && _uiState.value.menus.isNotEmpty()) return

        val updatedFilters = _uiState.value.filterItems.map { item ->
            item.copy(isSelected = !item.isLikedTab && item.tag == targetTag)
        }

        _uiState.update {
            it.copy(
                selectedTag = targetTag,
                filterItems = updatedFilters,
                menus = emptyList(),
                rawMenus = emptyList(),
                popularMenus = emptyList(),
                hasMore = true,
                isLoadingMore = false
            )
        }

        loadMenus(tag = targetTag, isRefresh = false)
    }

    fun onSearchQueryChanged(query: String) {
        _uiState.update { it.copy(searchQuery = query) }

        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            val trimmed = query.trim()
            if (trimmed.isNotBlank()) {
                delay(200)
                val hasExisting = _uiState.value.menus.isNotEmpty()
                if (!hasExisting) {
                    _uiState.update { it.copy(isLoading = true) }
                }
                when (val result = menuRepo.searchMenus(
                    queryText = trimmed,
                    tag = _uiState.value.selectedTag,
                    currentUserId = currentUserId,
                    pageSize = 30
                )) {
                    is BaseResult.Success -> {
                        _uiState.update {
                            it.copy(
                                rawMenus = result.data,
                                menus = result.data,
                                popularMenus = emptyList(), // Qidiruv vaqtida faqat qidiruv natijalarini ko'rsatamiz
                                isLoading = false,
                                isRefreshing = false,
                                hasMore = false,
                                isLoadingMore = false,
                                errorMessage = null
                            )
                        }
                    }
                    is BaseResult.Failure -> {
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                isRefreshing = false,
                                hasMore = false,
                                isLoadingMore = false,
                                errorMessage = result.error?.message
                            )
                        }
                    }
                }
            } else {
                // Qidiruv tozalandi: tanlangan kategoriya bo'yicha yana boshidan olib kelamiz
                loadMenus(tag = _uiState.value.selectedTag, isRefresh = false)
            }
        }
    }

    fun refresh() {
        val currentQuery = _uiState.value.searchQuery.trim()
        if (currentQuery.isNotBlank()) {
            onSearchQueryChanged(currentQuery)
        } else {
            loadMenus(tag = _uiState.value.selectedTag, isRefresh = true)
        }
    }

    fun loadMenus(tag: FoodTag? = _uiState.value.selectedTag, isRefresh: Boolean = false) {
        viewModelScope.launch {
            val hasExistingData = _uiState.value.menus.isNotEmpty()
            if (isRefresh) {
                _uiState.update { it.copy(isRefreshing = true) }
            } else if (!hasExistingData) {
                _uiState.update { it.copy(isLoading = true) }
            }

            // 1. Shu kategoriya bo'yicha eng ko'p yoqtirilgan (layk to'plagan) taomlarni olib kelamiz
            launch {
                when (val popResult = menuRepo.getPopularMenus(tag = tag, limit = 10, currentUserId = currentUserId)) {
                    is BaseResult.Success -> {
                        _uiState.update { it.copy(popularMenus = popResult.data) }
                    }
                    is BaseResult.Failure -> {
                        // Xato bo'lsa avvalgisi saqlanadi yoki bo'sh bo'ladi
                    }
                }
            }

            // 2. Internetdan tanlangan kategoriya va timestamp bo'yicha to'g'ridan-to'g'ri 6 ta olib keladi
            when (val result = menuRepo.getMenus(
                tag = tag,
                isRecipe = null,
                currentUserId = currentUserId,
                lastTimestamp = null,
                pageSize = PAGE_SIZE,
                forceRefresh = isRefresh
            )) {
                is BaseResult.Success -> {
                    val fetched = result.data
                    _uiState.update {
                        it.copy(
                            rawMenus = fetched,
                            menus = fetched,
                            isLoading = false,
                            isRefreshing = false,
                            hasMore = fetched.size >= PAGE_SIZE,
                            isLoadingMore = false,
                            errorMessage = null
                        )
                    }
                }
                is BaseResult.Failure -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            isRefreshing = false,
                            isLoadingMore = false,
                            errorMessage = result.error?.message
                        )
                    }
                }
            }
        }
    }

    fun loadNextPage() {
        val state = _uiState.value
        if (state.isLoading || state.isRefreshing || state.isLoadingMore || !state.hasMore) {
            return
        }
        if (state.searchQuery.isNotBlank()) {
            return
        }

        val lastTimestamp = state.menus.lastOrNull()?.createdAt

        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingMore = true) }

            when (val result = menuRepo.getMenus(
                tag = state.selectedTag,
                isRecipe = null,
                currentUserId = currentUserId,
                lastTimestamp = lastTimestamp,
                pageSize = PAGE_SIZE
            )) {
                is BaseResult.Success -> {
                    val nextItems = result.data
                    val existingIds = state.menus.map { it.id }.toSet()
                    val distinctNextItems = nextItems.filterNot { it.id in existingIds }

                    _uiState.update { current ->
                        current.copy(
                            menus = current.menus + distinctNextItems,
                            rawMenus = current.rawMenus + distinctNextItems,
                            isLoadingMore = false,
                            hasMore = nextItems.size >= PAGE_SIZE,
                            errorMessage = null
                        )
                    }
                }
                is BaseResult.Failure -> {
                    _uiState.update {
                        it.copy(
                            isLoadingMore = false,
                            errorMessage = result.error?.message
                        )
                    }
                }
            }
        }
    }

    fun toggleLike(menu: Menu) {
        val uid = currentUserId
        if (uid.isBlank()) return

        // Optimistic UI update
        val isLikedNow = !menu.isLiked
        val updatedLikesCount = if (isLikedNow) menu.likesCount + 1 else (menu.likesCount - 1).coerceAtLeast(0)
        val updatedMenu = menu.copy(isLiked = isLikedNow, likesCount = updatedLikesCount)

        _uiState.update { state ->
            val updatedList = state.menus.map {
                if (it.id == menu.id) updatedMenu else it
            }
            val updatedPopular = state.popularMenus.map {
                if (it.id == menu.id) updatedMenu else it
            }
            state.copy(menus = updatedList, popularMenus = updatedPopular)
        }

        viewModelScope.launch {
            when (val result = menuRepo.toggleLike(menuId = menu.id, userId = uid)) {
                is BaseResult.Success -> {
                    _uiState.update { state ->
                        val updatedList = state.menus.map {
                            if (it.id == menu.id) result.data.copy(isLiked = result.data.likedUserIds.contains(uid)) else it
                        }
                        val updatedPopular = state.popularMenus.map {
                            if (it.id == menu.id) result.data.copy(isLiked = result.data.likedUserIds.contains(uid)) else it
                        }
                        state.copy(menus = updatedList, popularMenus = updatedPopular)
                    }
                }
                is BaseResult.Failure -> {
                    _uiState.update { state ->
                        val rollbackList = state.menus.map {
                            if (it.id == menu.id) menu else it
                        }
                        val rollbackPopular = state.popularMenus.map {
                            if (it.id == menu.id) menu else it
                        }
                        state.copy(menus = rollbackList, popularMenus = rollbackPopular)
                    }
                }
            }
        }
    }

    fun onMenuClicked(menu: Menu) {
        viewModelScope.launch {
            _uiEffect.emit(MenuListUiEffect.NavigateToDetail(menu))
        }
    }

    fun onAddMenuClicked() {
        viewModelScope.launch {
            _uiEffect.emit(MenuListUiEffect.NavigateToAddMenu)
        }
    }

    fun removeMenuLocally(menuId: String) {
        _uiState.update { state ->
            val updatedMenus = state.menus.filterNot { it.id == menuId }
            val updatedRaw = state.rawMenus.filterNot { it.id == menuId }
            val updatedPopular = state.popularMenus.filterNot { it.id == menuId }
            state.copy(menus = updatedMenus, rawMenus = updatedRaw, popularMenus = updatedPopular)
        }
    }
}
