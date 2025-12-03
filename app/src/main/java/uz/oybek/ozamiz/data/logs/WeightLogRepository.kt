package uz.oybek.ozamiz.data.logs

import android.net.Uri
import com.google.firebase.Firebase
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.firestore
import com.google.firebase.firestore.toObject
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import uz.oybek.ozamiz.data.firebase.uploader.ImagePaths
import uz.oybek.ozamiz.data.firebase.uploader.ImageUploader
import uz.oybek.ozamiz.data.logs.model.WeightLog
import uz.oybek.ozamiz.data.user.UserRepository
import uz.oybek.ozamiz.data.user.local.LocalUser
import uz.oybek.ozamiz.data.utils.DataWrapper
import java.util.Date
import java.util.UUID
import java.util.concurrent.TimeUnit

class WeightLogRepository {

    private val firestore = Firebase.firestore

    private fun weightLogsCollection(userId: String) =
        firestore
            .collection(FirebaseCollections.USERS)
            .document(userId)
            .collection(FirebaseCollections.WEIGHT_PHOTOS)

    suspend fun addLog(
        weightKg: Double,
        photoUri: Uri
    ): DataWrapper<WeightLog> {
        var uploadedImageUrl: String? = null
        return try {
            when (val uploadResult =
                ImageUploader.uploadImage(photoUri, ImagePaths.weightLog)) {
                is DataWrapper.Success -> uploadedImageUrl = uploadResult.data
                is DataWrapper.Error -> {
                    return DataWrapper.Error()
                }
            }
            val newLog = WeightLog(
                id = UUID.randomUUID().toString(),
                userId = LocalUser.user.uid,
                weightKg = weightKg,
                photoUrl = uploadedImageUrl,
                timestamp = null
            )

            weightLogsCollection(LocalUser.user.uid)
                .document(newLog.id)
                .set(newLog, SetOptions.merge()).await()
            LocalUser.user = LocalUser.user.copy(
                weight = newLog.weightKg,
                lastWeightLogTimestamp = System.currentTimeMillis(),
                avatarUrl = newLog.photoUrl
            )
            LocalUser.saveUser()
            UserRepository.updateUser {
                it.weight = newLog.weightKg
                it.lastWeightLogTimestamp = LocalUser.user.lastWeightLogTimestamp
                it.avatarUrl = LocalUser.user.avatarUrl
            }
            DataWrapper.Success(newLog.copy(timestamp = Date()))
        } catch (_: Exception) {
            uploadedImageUrl?.let { ImageUploader.deleteImage(it) }
            DataWrapper.Error()
        }
    }

    fun getLogsFlow(userId: String): Flow<DataWrapper<List<WeightLog>>> =
        callbackFlow {
            val listenerRegistration = weightLogsCollection(userId)
                .orderBy(timestamp, Query.Direction.DESCENDING)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        trySend(DataWrapper.Error())
                        close(error)
                        return@addSnapshotListener
                    }
                    if (snapshot != null) {
                        val logs = snapshot.documents.mapNotNull {
                            it.toObject<WeightLog>()
                        }
                        trySend(DataWrapper.Success(logs))
                    } else {
                        trySend(DataWrapper.Success(emptyList()))
                    }
                }
            awaitClose { listenerRegistration.remove() }
        }

    suspend fun getLatestLog(userId: String): WeightLog? {
        return try {
            val snapshot = weightLogsCollection(userId)
                .orderBy(timestamp, Query.Direction.DESCENDING)
                .limit(1)
                .get()
                .await()
            val log = snapshot.documents
                .firstOrNull()
                ?.toObject<WeightLog>()
            return log
        } catch (_: Exception) {
            null
        }
    }

    suspend fun hasAnyLog(userId: String): DataWrapper<Boolean> {
        return try {
            val snapshot = weightLogsCollection(userId)
                .limit(1)
                .get()
                .await()
            DataWrapper.Success(!snapshot.isEmpty)
        } catch (e: Exception) {
            DataWrapper.Error(e.message.toString())
        }
    }

    companion object {
        private val timestamp = "timestamp"
        private const val INTERVAL_DAYS = 7

        fun timeToWeightLog(): Boolean {
            val user = LocalUser.user
            val lastLogTime = user.lastWeightLogTimestamp
            val daysSinceLastLog = if (lastLogTime > 0) {
                TimeUnit.MILLISECONDS.toDays(System.currentTimeMillis() - lastLogTime)
            } else {
                (INTERVAL_DAYS + 1).toLong()
            }
            return daysSinceLastLog >= INTERVAL_DAYS
        }
    }

}