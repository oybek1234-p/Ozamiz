package uz.oybek.ozamiz.ui.feature.community

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import uz.oybek.ozamiz.data.community.CommunityPostRepository
import uz.oybek.ozamiz.data.community.model.CommunityPost

data class CommunityState(
    val posts: List<CommunityPost> = emptyList(),
    val isLoading: Boolean = false,
    val isPaging: Boolean = false,
    val hasReachedEnd: Boolean = false,
    val error: String? = null
)

class CommunityViewModel : ViewModel() {
    private val _state = MutableStateFlow(CommunityState())
    val state: StateFlow<CommunityState> = _state
    
    private var lastVisiblePostId: String? = null

    init {
        loadPosts()
    }

    fun refresh() {
        lastVisiblePostId = null
        _state.update { it.copy(hasReachedEnd = false) }
        loadPosts()
    }

    fun loadPosts() {
        if (_state.value.isLoading || _state.value.isPaging || _state.value.hasReachedEnd) return

        val isFirstLoad = lastVisiblePostId == null
        _state.update { it.copy(isLoading = isFirstLoad, isPaging = !isFirstLoad, error = null) }

        viewModelScope.launch {
            val result = CommunityPostRepository.getPosts(limit = 8, lastVisiblePostId = lastVisiblePostId, verifiedOnly = true)
            result.onSuccess { newPosts ->
                if (newPosts.isEmpty()) {
                    _state.update { it.copy(isLoading = false, isPaging = false, hasReachedEnd = true) }
                } else {
                    lastVisiblePostId = newPosts.last().id
                    val updatedPosts = if (isFirstLoad) newPosts else _state.value.posts + newPosts
                    _state.update { 
                        it.copy(
                            posts = updatedPosts, 
                            isLoading = false, 
                            isPaging = false,
                            hasReachedEnd = newPosts.size < 8
                        ) 
                    }
                }
            }.onFailure { e ->
                _state.update { it.copy(isLoading = false, isPaging = false, error = e.message) }
            }
        }
    }

    fun toggleLike(postId: String) {
        val user = uz.oybek.ozamiz.data.user.local.LocalUser.user
        if (user.uid.isEmpty()) return
        
        viewModelScope.launch {
            val post = _state.value.posts.find { it.id == postId } ?: return@launch
            val result = CommunityPostRepository.toggleLike(postId, user.uid)
            result.onSuccess { isLiked ->
                // Optimistically update the UI
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
                    val notification = uz.oybek.ozamiz.data.notification.model.NotificationModel(
                        type = uz.oybek.ozamiz.data.notification.model.NotificationModel.TYPE_LIKE,
                        fromUserId = user.uid,
                        fromUserName = user.name,
                        fromUserAvatar = user.avatarUrl,
                        toUserId = post.userId,
                        postId = post.id,
                        message = "Sizning postingizga layk bosdi",
                        timestamp = java.util.Date()
                    )
                    uz.oybek.ozamiz.data.notification.NotificationRepository.sendNotification(notification)
                }
            }
        }
    }

    fun deletePost(postId: String) {
        viewModelScope.launch {
            val result = CommunityPostRepository.deletePost(postId)
            result.onSuccess {
                _state.update { currentState ->
                    val newPosts = currentState.posts.filter { it.id != postId }
                    currentState.copy(posts = newPosts)
                }
            }
        }
    }
    fun markViewed(postId: String) {
        val userId = Firebase.auth.currentUser?.uid ?: return
        viewModelScope.launch {
            CommunityPostRepository.markViewed(postId, userId)
        }
    }
}
