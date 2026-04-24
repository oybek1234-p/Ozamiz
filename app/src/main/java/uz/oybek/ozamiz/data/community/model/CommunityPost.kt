package uz.oybek.ozamiz.data.community.model

import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

data class CommunityPost(
    var id: String = "",
    var userId: String = "",
    var userName: String = "",
    var userAvatarUrl: String? = null,
    var photoUrl: String = "",
    var photos: List<String> = emptyList(),
    var title: String = "",
    var description: String = "",
    var foodName: String = "",
    var calories: Int = 0,
    var likesCount: Int = 0,
    val likedBy: List<String> = emptyList(),
    var viewsCount: Int = 0,
    val viewedBy: List<String> = emptyList(),
    @ServerTimestamp
    var timestamp: Date? = null,
    var comment: String? = null,
    var verified: Boolean = false,
    var isVisible: Boolean = false,
    var healthScore: Int = 0,
    var userScore: Int = 0
)
