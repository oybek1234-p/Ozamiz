package uz.oybek.ozamiz.ui.feature.admin.community

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import uz.oybek.ozamiz.common.result.BaseResult
import uz.oybek.ozamiz.data.community.api.AdminCommunityRepository
import uz.oybek.ozamiz.data.community.model.CommunityPost
import javax.inject.Inject

data class AdminPostManagerUiState(
    val posts: List<CommunityPost> = emptyList(),
    val isLoading: Boolean = false
)

sealed interface AdminPostManagerEffect {
    data object PostApprovedSuccess : AdminPostManagerEffect
    data object PostDeletedSuccess : AdminPostManagerEffect
    data object AllPostsDeletedSuccess : AdminPostManagerEffect
    data object GenericError : AdminPostManagerEffect
}

@HiltViewModel
class AdminPostManagerViewModel @Inject constructor(
    private val adminCommunityRepository: AdminCommunityRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AdminPostManagerUiState())
    val uiState = _uiState.asStateFlow()

    private val _effect = MutableSharedFlow<AdminPostManagerEffect>()
    val effect = _effect.asSharedFlow()

    fun loadUnverifiedPosts() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            when (val result = adminCommunityRepository.getPendingPosts()) {
                is BaseResult.Success -> {
                    _uiState.update { it.copy(posts = result.data, isLoading = false) }
                }
                is BaseResult.Failure -> {
                    _uiState.update { it.copy(posts = emptyList(), isLoading = false) }
                }
            }
        }
    }

    fun approvePost(postId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            when (adminCommunityRepository.approvePost(postId)) {
                is BaseResult.Success -> {
                    _uiState.update { state ->
                        state.copy(
                            posts = state.posts.filterNot { it.id == postId },
                            isLoading = false
                        )
                    }
                    _effect.emit(AdminPostManagerEffect.PostApprovedSuccess)
                }
                is BaseResult.Failure -> {
                    _uiState.update { it.copy(isLoading = false) }
                    _effect.emit(AdminPostManagerEffect.GenericError)
                }
            }
        }
    }

    fun deletePost(postId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            when (adminCommunityRepository.adminDeletePost(postId)) {
                is BaseResult.Success -> {
                    _uiState.update { state ->
                        state.copy(
                            posts = state.posts.filterNot { it.id == postId },
                            isLoading = false
                        )
                    }
                    _effect.emit(AdminPostManagerEffect.PostDeletedSuccess)
                }
                is BaseResult.Failure -> {
                    _uiState.update { it.copy(isLoading = false) }
                    _effect.emit(AdminPostManagerEffect.GenericError)
                }
            }
        }
    }

    fun deleteAllPendingPosts() {
        val currentPosts = _uiState.value.posts
        if (currentPosts.isEmpty()) return

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            currentPosts.forEach { post ->
                adminCommunityRepository.adminDeletePost(post.id)
            }
            _uiState.update { it.copy(posts = emptyList(), isLoading = false) }
            _effect.emit(AdminPostManagerEffect.AllPostsDeletedSuccess)
        }
    }
}
