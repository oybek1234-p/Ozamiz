package uz.oybek.ozamiz.ui.feature.community

import androidx.lifecycle.ViewModel
import uz.oybek.ozamiz.data.community.CommunityPostRepository
import uz.oybek.ozamiz.data.user.UserRepository

class CommunityProfileViewModel : ViewModel() {

    suspend fun getAdminUser(userId: String) = UserRepository.getAdminUser(userId)
    
    suspend fun getPublicUser(userId: String) = UserRepository.getPublicUser(userId)
    
    suspend fun getUserPosts(userId: String, limit: Int = 10, lastVisiblePostId: String? = null) =
        CommunityPostRepository.getUserPosts(userId, limit.toLong(), lastVisiblePostId)
        
    val currentUserId: String
        get() = UserRepository.user.uid

    suspend fun toggleLike(postId: String, currentUserId: String) =
        CommunityPostRepository.toggleLike(postId, currentUserId)
        
    suspend fun markViewed(postId: String, currentUserId: String) =
        CommunityPostRepository.markViewed(postId, currentUserId)
}
