package uz.oybek.ozamiz.data.notification

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.tasks.await
import uz.oybek.ozamiz.data.notification.model.NotificationModel
import uz.oybek.ozamiz.data.user.UserRepository

object NotificationRepository {
    private val db = FirebaseFirestore.getInstance()
    private val notificationsCollection = db.collection("notifications")

    suspend fun sendNotification(notification: NotificationModel): Result<Unit> {
        return try {
            val docRef = notificationsCollection.document()
            notification.id = docRef.id
            docRef.set(notification).await()

            UserRepository.incrementUnreadMessages(notification.toUserId)
            
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getNotifications(userId: String, limit: Long = 50): Result<List<NotificationModel>> {
        return try {
            val snapshot = notificationsCollection
                .whereEqualTo("toUserId", userId)
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .limit(limit)
                .get().await()
            val notifications = snapshot.documents.mapNotNull { it.toObject(NotificationModel::class.java) }
            Result.success(notifications)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
