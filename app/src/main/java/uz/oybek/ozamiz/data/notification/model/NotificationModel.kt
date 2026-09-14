package uz.oybek.ozamiz.data.notification.model

import androidx.annotation.Keep
import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

@Keep
data class NotificationModel(
    var id: String = "",
    var type: String = "", // "like", "comment", etc.
    var fromUserId: String = "",
    var fromUserName: String = "",
    var fromUserAvatar: String? = null,
    var toUserId: String = "",
    var postId: String? = null,
    var message: String = "",
    var read: Boolean = false,
    @ServerTimestamp
    var timestamp: Date? = null
) {
    companion object {
        const val TYPE_LIKE = "like"
        const val TYPE_COMMENT = "comment"
    }
}
