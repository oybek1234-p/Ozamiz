package uz.oybek.ozamiz.data.community.model

import com.google.firebase.firestore.PropertyName
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
    var protein: Double? = null,
    var fat: Double? = null,
    var carbs: Double? = null,
    var likesCount: Int = 0,
    val likedBy: List<String> = emptyList(),
    var viewsCount: Int = 0,
    val viewedBy: List<String> = emptyList(),
    @ServerTimestamp
    var timestamp: Date? = null,
    var comment: String? = null,
    var verified: Boolean = false,
    @get:PropertyName("visible")
    @set:PropertyName("visible")
    var isVisible: Boolean = false,
    var healthScore: Int = 0,
    var userScore: Int = 0,
    var type: String = PostType.MANUAL,
    @get:PropertyName("premiumUser")
    @set:PropertyName("premiumUser")
    var isPremiumUser: Boolean = false
)

object PostType {
    const val FOOD = "FOOD"
    const val WEIGHT_LOSS = "WEIGHT_LOSS"
    const val WATER = "WATER"
    const val MANUAL = "MANUAL"
}
