package uz.oybek.ozamiz.ui.feature.menu.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import uz.oybek.ozamiz.common.result.BaseResult
import uz.oybek.ozamiz.data.menu.api.MenuRepo
import uz.oybek.ozamiz.data.menu.model.Menu
import uz.oybek.ozamiz.data.menu.model.MenuItemFood
import uz.oybek.ozamiz.data.profile.api.ProfileRepo
import javax.inject.Inject

data class MenuDetailUiState(
    val menu: Menu? = null,
    val isLoading: Boolean = false,
    val isDeleting: Boolean = false,
    val isAdmin: Boolean = false,
    val targetCalories: Int = 0,
    val calculatedItems: List<MenuItemFood> = emptyList(),
    val errorMessage: String? = null
)

sealed interface MenuDetailUiEffect {
    data class DeleteSuccess(val menuId: String) : MenuDetailUiEffect
    data class DeleteError(val error: String?) : MenuDetailUiEffect
}

@HiltViewModel
class MenuDetailViewModel @Inject constructor(
    private val menuRepo: MenuRepo,
    private val profileRepo: ProfileRepo
) : ViewModel() {

    private val _uiState = MutableStateFlow(MenuDetailUiState())
    val uiState: StateFlow<MenuDetailUiState> = _uiState.asStateFlow()

    private val _uiEffect = MutableSharedFlow<MenuDetailUiEffect>()
    val uiEffect: SharedFlow<MenuDetailUiEffect> = _uiEffect.asSharedFlow()

    private val currentUserId: String
        get() = profileRepo.userId

    fun setMenu(menu: Menu) {
        val isLiked = menu.likedUserIds.contains(currentUserId)
        _uiState.update {
            it.copy(
                menu = menu.copy(isLiked = isLiked),
                isAdmin = profileRepo.currentUser.admin,
                targetCalories = menu.totalCalories,
                calculatedItems = menu.items
            )
        }
    }

    fun loadMenuById(menuId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            when (val result = menuRepo.getMenuById(menuId, currentUserId)) {
                is BaseResult.Success -> {
                    val menu = result.data
                    _uiState.update {
                        it.copy(
                            menu = menu,
                            isAdmin = profileRepo.currentUser.admin,
                            targetCalories = menu.totalCalories,
                            calculatedItems = menu.items,
                            isLoading = false
                        )
                    }
                }
                is BaseResult.Failure -> {
                    _uiState.update {
                        it.copy(isLoading = false, errorMessage = result.error?.message)
                    }
                }
            }
        }
    }

    fun toggleLike() {
        val currentMenu = _uiState.value.menu ?: return
        val uid = currentUserId
        if (uid.isBlank()) return

        val isLikedNow = !currentMenu.isLiked
        val updatedLikesCount = if (isLikedNow)
            currentMenu.likesCount + 1 else (currentMenu.likesCount - 1).coerceAtLeast(0)
        val updatedMenu = currentMenu.copy(isLiked = isLikedNow, likesCount = updatedLikesCount)

        _uiState.update { it.copy(menu = updatedMenu) }

        viewModelScope.launch {
            when (val result = menuRepo.toggleLike(currentMenu.id, uid)) {
                is BaseResult.Success -> {
                    _uiState.update {
                        it.copy(menu = result.data.copy(isLiked = result.data.likedUserIds.contains(uid)))
                    }
                }
                is BaseResult.Failure -> {
                    _uiState.update { it.copy(menu = currentMenu) }
                }
            }
        }
    }

    fun calculatePortion(targetCals: Int) {
        val menu = _uiState.value.menu ?: return
        if (targetCals <= 0) return

        val scaled = menu.scaleForCalories(targetCals)
        _uiState.update {
            it.copy(
                targetCalories = targetCals,
                calculatedItems = scaled
            )
        }
    }

    fun deleteMenu(menuId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isDeleting = true) }
            val imageUrl = _uiState.value.menu?.imageUrl
            when (val result = menuRepo.deleteMenu(menuId = menuId, imageUrl = imageUrl)) {
                is BaseResult.Success -> {
                    _uiState.update { it.copy(isDeleting = false) }
                    _uiEffect.emit(MenuDetailUiEffect.DeleteSuccess(menuId))
                }
                is BaseResult.Failure -> {
                    _uiState.update { it.copy(isDeleting = false) }
                    _uiEffect.emit(MenuDetailUiEffect.DeleteError(result.error?.message))
                }
            }
        }
    }
}
