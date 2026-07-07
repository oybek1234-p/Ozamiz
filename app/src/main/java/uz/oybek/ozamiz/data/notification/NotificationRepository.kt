package uz.oybek.ozamiz.data.notification

import com.google.firebase.firestore.Query
import uz.oybek.ozamiz.data.firebase.firestore
import kotlinx.coroutines.tasks.await
import uz.oybek.ozamiz.data.notification.model.NotificationModel
import uz.oybek.ozamiz.data.user.UserProfileRepository

import uz.oybek.ozamiz.data.firebase.notificationsCollection

import uz.oybek.ozamiz.util.returnResult

object NotificationRepository {
    private val notificationsCollection = firestore.notificationsCollection

    suspend fun sendNotification(notification: NotificationModel): Result<Unit> {
        return returnResult {
            val docRef = notificationsCollection.document()
            notification.id = docRef.id
            docRef.set(notification).await()

            UserProfileRepository.incrementUnreadMessages(notification.toUserId)
        }
    }

    suspend fun getNotifications(userId: String, limit: Long = 50): Result<List<NotificationModel>> {
        return returnResult {
            val snapshot = notificationsCollection
                .whereEqualTo(NotificationModel::toUserId.name, userId)
                .orderBy(NotificationModel::timestamp.name, Query.Direction.DESCENDING)
                .limit(limit)
                .get().await()
            snapshot.documents.mapNotNull { it.toObject(NotificationModel::class.java) }
        }
    }
}
