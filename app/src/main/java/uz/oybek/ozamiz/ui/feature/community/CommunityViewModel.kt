package uz.oybek.ozamiz.ui.feature.community

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import uz.oybek.ozamiz.common.result.BaseResult
import uz.oybek.ozamiz.data.community.api.CommunityPostRepository
import uz.oybek.ozamiz.data.community.api.PublicUserRepository
import uz.oybek.ozamiz.data.community.model.CommunityPost
import uz.oybek.ozamiz.data.community.model.CommunityPostFilter
import uz.oybek.ozamiz.data.profile.api.ProfileRepo
import uz.oybek.ozamiz.data.profile.model.User
import javax.inject.Inject

data class CommunityUiState(
    val filter: CommunityPostFilter = CommunityPostFilter.ALL,
    val posts: List<CommunityPost> = emptyList(),
    val users: List<User> = emptyList(),
    val isLoading: Boolean = false,
    val isPaging: Boolean = false,
    val hasReachedEnd: Boolean = false,
    val currentUserId: String = ""
)

sealed interface CommunityUiEffect {
    data object PostDeletedSuccess : CommunityUiEffect
    data object GenericError : CommunityUiEffect
}

@HiltViewModel
class CommunityViewModel @Inject constructor(
    private val postRepository: CommunityPostRepository,
    private val publicUserRepository: PublicUserRepository,
    profileRepo: ProfileRepo
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        CommunityUiState(currentUserId = profileRepo.userId)
    )
    val uiState: StateFlow<CommunityUiState> = _uiState.asStateFlow()

    private val _effectChannel = Channel<CommunityUiEffect>(Channel.BUFFERED)
    val effect = _effectChannel.receiveAsFlow()

    init {
        loadData(isInitial = true)
    }

    fun setFilter(newFilter: CommunityPostFilter) {
        if (_uiState.value.filter == newFilter) return
        _uiState.update {
            it.copy(
                filter = newFilter,
                posts = emptyList(),
                users = emptyList(),
                hasReachedEnd = false
            )
        }
        loadData(isInitial = true)
    }

    fun refresh() {
        _uiState.update { it.copy(hasReachedEnd = false) }
        loadData(isInitial = true)
    }

    fun loadNextPage() {
        val state = _uiState.value
        if (state.isLoading || state.isPaging || state.hasReachedEnd) return
        loadData(isInitial = false)
    }

    private fun loadData(isInitial: Boolean) {
        val currentFilter = _uiState.value.filter

        _uiState.update {
            it.copy(
                isLoading = isInitial,
                isPaging = !isInitial
            )
        }

        viewModelScope.launch {
            if (currentFilter == CommunityPostFilter.ACTIVE_USERS) {
                loadUsers(isInitial)
            } else {
                loadPosts(isInitial, currentFilter)
            }
        }
    }

    private suspend fun loadPosts(isInitial: Boolean, filter: CommunityPostFilter) {
        val lastTimestamp = if (isInitial) null else _uiState.value.posts.lastOrNull()?.createdAt
        val pageSize = 10L

        when (val result = postRepository.getPosts(
            filter = filter, limit =
                pageSize, lastTimestamp = lastTimestamp
        )) {
            is BaseResult.Success -> {
                val newPosts = result.data
                val updatedList = if (isInitial) {
                    newPosts.distinctBy { it.id }
                } else {
                    (_uiState.value.posts + newPosts).distinctBy { it.id }
                }
                val reachedEnd = newPosts.size < pageSize

                _uiState.update {
                    it.copy(
                        posts = updatedList,
                        isLoading = false,
                        isPaging = false,
                        hasReachedEnd = reachedEnd
                    )
                }
            }

            is BaseResult.Failure -> {
                _uiState.update {
                    it.copy(isLoading = false, isPaging = false)
                }
                _effectChannel.send(CommunityUiEffect.GenericError)
            }
        }
    }

    private suspend fun loadUsers(isInitial: Boolean) {
        val lastTimestamp = if (isInitial) null else _uiState.value.users.lastOrNull()?.lastSeenTime
        val pageSize = 15L

        when (val result =
            publicUserRepository.getPublicUsers(limit = pageSize, lastTimestamp = lastTimestamp)) {
            is BaseResult.Success -> {
                val newUsers = result.data
                val updatedList = if (isInitial) {
                    newUsers.distinctBy { it.uid }
                } else {
                    (_uiState.value.users + newUsers).distinctBy { it.uid }
                }
                val reachedEnd = newUsers.size < pageSize

                _uiState.update {
                    it.copy(
                        users = updatedList,
                        isLoading = false,
                        isPaging = false,
                        hasReachedEnd = reachedEnd
                    )
                }
            }

            is BaseResult.Failure -> {
                _uiState.update {
                    it.copy(isLoading = false, isPaging = false)
                }
                _effectChannel.send(CommunityUiEffect.GenericError)
            }
        }
    }

    fun toggleLike(post: CommunityPost) {
        val uid = _uiState.value.currentUserId
        if (uid.isBlank()) return

        val isLiked = post.likedBy.contains(uid)
        val newCount = if (isLiked) post.likesCount - 1 else post.likesCount + 1
        val newLikedBy = if (isLiked) post.likedBy - uid else post.likedBy + uid
        val updatedPost = post.copy(likesCount = newCount, likedBy = newLikedBy)

        _uiState.update { state ->
            state.copy(
                posts = state.posts.map { if (it.id == post.id) updatedPost else it }
            )
        }

        viewModelScope.launch {
            postRepository.togglePostLike(post.id)
        }
    }

    fun updateCommentCount(postId: String, delta: Int) {
        if (postId.isBlank() || delta == 0) return
        _uiState.update { state ->
            state.copy(
                posts = state.posts.map { post ->
                    if (post.id == postId) {
                        val newCount = (post.commentsCount + delta).coerceAtLeast(0)
                        post.copy(commentsCount = newCount)
                    } else {
                        post
                    }
                }
            )
        }
    }

    fun deletePost(post: CommunityPost) {
        _uiState.update { state ->
            state.copy(posts = state.posts.filter { it.id != post.id })
        }

        viewModelScope.launch {
            when (postRepository.deleteMyPost(post.id)) {
                is BaseResult.Success -> {
                    _effectChannel.send(CommunityUiEffect.PostDeletedSuccess)
                }

                is BaseResult.Failure -> {
                    _effectChannel.send(CommunityUiEffect.GenericError)
                }
            }
        }
    }
}
