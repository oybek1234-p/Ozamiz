package uz.oybek.ozamiz.data.notification

import com.google.firebase.firestore.Query
import uz.oybek.ozamiz.data.firebase.firestore
import kotlinx.coroutines.tasks.await
import uz.oybek.ozamiz.data.notification.model.NotificationModel
import uz.oybek.ozamiz.data.user.UserRepository

import uz.oybek.ozamiz.data.firebase.notificationsCollection

import uz.oybek.ozamiz.util.returnResult

object NotificationRepository {
    private val notificationsCollection = firestore.notificationsCollection

    suspend fun sendNotification(notification: NotificationModel): Result<Unit> {
        return returnResult {
            val docRef = notificationsCollection.document()
            notification.id = docRef.id
            docRef.set(notification).await()

            UserRepository.incrementUnreadMessages(notification.toUserId)
        }
    }

    suspend fun getNotifications(userId: String, limit: Long = 50): Result<List<NotificationModel>> {
        return returnResult {
            val snapshot = notificationsCollection
                .whereEqualTo("toUserId", userId)
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .limit(limit)
                .get().await()
            snapshot.documents.mapNotNull { it.toObject(NotificationModel::class.java) }
        }
    }
}
