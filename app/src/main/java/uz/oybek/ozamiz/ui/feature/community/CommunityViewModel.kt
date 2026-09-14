package uz.oybek.ozamiz.ui.feature.community

import uz.oybek.ozamiz.data.community.model.CommunityPostFilter.ALL
import uz.oybek.ozamiz.data.notification.NotificationRepository
import uz.oybek.ozamiz.data.notification.model.NotificationModel
import uz.oybek.ozamiz.data.user.local.LocalUser
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import uz.oybek.ozamiz.data.community.CommunityPostRepository
import uz.oybek.ozamiz.data.user.UserProfileRepository
import androidx.annotation.Keep
import uz.oybek.ozamiz.data.community.model.CommunityPost
import uz.oybek.ozamiz.data.community.model.CommunityPostFilter
import uz.oybek.ozamiz.data.notification.model.NotificationModel.Companion.TYPE_LIKE

@Keep
data class CommunityState(
    val posts: List<CommunityPost> = emptyList(),
    val isLoading: Boolean = false,
    val isPaging: Boolean = false,
    val hasReachedEnd: Boolean = false,
    val error: String? = null,
    val filter: CommunityPostFilter = CommunityPostFilter.WEIGHT_LOSS
)

class CommunityViewModel : ViewModel() {
    private val _state = MutableStateFlow(CommunityState())
    val state: StateFlow<CommunityState> = _state

    private var lastSnapshot: com.google.firebase.firestore.DocumentSnapshot? = null

    init {
        loadPosts()
    }

    fun setFilter(filter: CommunityPostFilter) {
        if (_state.value.filter == filter) return
        _state.update { it.copy(filter = filter, posts = emptyList(), hasReachedEnd = false) }
        lastSnapshot = null
        loadPosts()
    }

    fun refresh() {
        lastSnapshot = null
        _state.update { it.copy(hasReachedEnd = false) }
        loadPosts()
    }

    fun loadPosts() {
        if (_state.value.isLoading || _state.value.isPaging || _state.value.hasReachedEnd) return

        val isFirstLoad = lastSnapshot == null
        _state.update { it.copy(isLoading = isFirstLoad, isPaging = !isFirstLoad, error = null) }

        val requestLimit = 10L

        viewModelScope.launch {
            val result = CommunityPostRepository.getPosts(
                limit = requestLimit,
                lastSnapshot = lastSnapshot,
                filter = _state.value.filter
            )
            result.onSuccess { fetchResult ->
                lastSnapshot = fetchResult.lastSnapshot
                val newPosts = fetchResult.posts
                val updatedPosts = if (isFirstLoad) newPosts else _state.value.posts + newPosts
                _state.update {
                    it.copy(
                        posts = updatedPosts,
                        isLoading = false,
                        isPaging = false,
                        hasReachedEnd = fetchResult.hasReachedEnd
                    )
                }
            }.onFailure { e ->
                _state.update { it.copy(isLoading = false, isPaging = false, error = e.message) }
            }
        }
    }

    fun toggleLike(postId: String) {
        val user = LocalUser.user
        if (user.uid.isEmpty()) return

        viewModelScope.launch {
            val post = _state.value.posts.find { it.id == postId } ?: return@launch
            val result = CommunityPostRepository.toggleLike(postId, user.uid)
            result.onSuccess { isLiked ->
                _state.update { currentState ->
                    val newPosts = currentState.posts.map { p ->
                        if (p.id == postId) {
                            val newLikesCount = if (isLiked) p.likesCount + 1 else p.likesCount - 1
                            val newLikedBy = p.likedBy.toMutableList()
                            if (isLiked) newLikedBy.add(user.uid) else newLikedBy.remove(user.uid)
                            p.copy(likesCount = newLikesCount, likedBy = newLikedBy)
                        } else p
                    }
                    currentState.copy(posts = newPosts)
                }
                if (isLiked && post.userId != user.uid) {
                    val notification = NotificationModel(
                        type = TYPE_LIKE,
                        fromUserId = user.uid,
                        fromUserName = user.name,
                        fromUserAvatar = user.avatarUrl,
                        toUserId = post.userId,
                        postId = post.id,
                        message = "Sizning postingizga layk bosdi",
                        timestamp = java.util.Date()
                    )
                    NotificationRepository.sendNotification(
                        notification
                    )
                }
            }
        }
    }

    fun deletePost(postId: String) {
        viewModelScope.launch {
            val isAdmin = UserProfileRepository.user.admin
            val result = if (isAdmin) {
                CommunityPostRepository.adminDeletePost(postId)
            } else {
                CommunityPostRepository.deletePost(postId)
            }
            result.onSuccess {
                _state.update { currentState ->
                    val newPosts = currentState.posts.filter { it.id != postId }
                    currentState.copy(posts = newPosts)
                }
            }.onFailure { e ->
                _state.update { it.copy(error = "O'chirishda xatolik: ${e.localizedMessage}") }
            }
        }
    }

    fun markViewed(postId: String) {
        val userId = UserProfileRepository.user.uid
        if (userId.isEmpty()) return
        viewModelScope.launch {
            CommunityPostRepository.markViewed(postId, userId)
        }
    }

    fun observeUnReadMessages(action: (Int) -> Unit) {
        UserProfileRepository.observeUnReadMessages(action)
    }
}
