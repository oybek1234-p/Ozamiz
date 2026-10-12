package uz.oybek.ozamiz.ui.feature.community

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import uz.oybek.ozamiz.common.result.BaseResult
import uz.oybek.ozamiz.data.community.api.CommunityPostRepository
import uz.oybek.ozamiz.data.community.api.PublicUserRepository
import uz.oybek.ozamiz.data.profile.api.ProfileRepo
import uz.oybek.ozamiz.data.profile.model.User
import javax.inject.Inject

@HiltViewModel
class CommunityProfileViewModel @Inject constructor(
    private val publicUserRepository: PublicUserRepository,
    private val postRepository: CommunityPostRepository,
    private val profileRepo: ProfileRepo
) : ViewModel() {

    suspend fun getPublicUser(userId: String): User? {
        return when (val result = publicUserRepository.getUserProfile(userId)) {
            is BaseResult.Success -> result.data
            is BaseResult.Failure -> null
        }
    }

    suspend fun getUserPosts(userId: String, limit: Long = 20, lastTimestamp: Long? = null) =
        postRepository.getUserPosts(userId, limit, lastTimestamp)

    val currentUserId: String
        get() = profileRepo.userId

    suspend fun toggleLike(postId: String) =
        postRepository.togglePostLike(postId)
}
