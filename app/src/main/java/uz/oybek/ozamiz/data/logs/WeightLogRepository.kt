package uz.oybek.ozamiz.data.logs

import android.net.Uri
import com.google.firebase.Firebase
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.firestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.tasks.await
import uz.oybek.ozamiz.data.firebase.generateKey
import uz.oybek.ozamiz.data.firebase.uploader.ImagePaths
import uz.oybek.ozamiz.data.firebase.uploader.ImageUploader
import uz.oybek.ozamiz.data.logs.model.WeightLog
import uz.oybek.ozamiz.data.user.local.LocalUser
import uz.oybek.ozamiz.data.utils.DataWrapper
import uz.oybek.ozamiz.util.handleException
import java.util.concurrent.TimeUnit

object WeightLogRepository {

    private const val INTERVAL_DAYS = 7
    private const val COLLECTION_USERS = "users"
    private const val COLLECTION_WEIGHT_LOGS = "weightLogs"

    suspend fun add(
        userId: String,
        weightKg: Double,
        photoUri: Uri
    ): DataWrapper<WeightLog> {
        val uploadedUrl =
            ImageUploader.uploadImage(photoUri, ImagePaths.weightLog)

        val logId = generateKey()

        val newLog = WeightLog(
            logId = logId,
            userId = userId,
            weightKg = weightKg,
            photoUrl = uploadedUrl.toString()
        )

        return try {
            Firebase.firestore.collection(COLLECTION_USERS)
                .document(userId)
                .collection(COLLECTION_WEIGHT_LOGS)
                .document(logId)
                .set(newLog)
                .await()
            DataWrapper.Success(newLog)
        } catch (e: Exception) {
            handleException(e)
            DataWrapper.Error(e.localizedMessage)
        }
    }

    fun getLogsFlow(userId: String): Flow<DataWrapper<List<WeightLog>>> = flow {

        emitAll(callbackFlow {
            val registration = Firebase.firestore.collection(COLLECTION_USERS)
                .document(userId)
                .collection(COLLECTION_WEIGHT_LOGS)
                .orderBy("timestampLong", Query.Direction.DESCENDING)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        trySend(DataWrapper.Error(error.message))
                        return@addSnapshotListener
                    }

                    if (snapshot != null) {
                        val logs = snapshot.toObjects(WeightLog::class.java)
                        trySend(DataWrapper.Success(logs))
                    } else {
                        trySend(DataWrapper.Success(emptyList()))
                    }
                }

            awaitClose { registration.remove() }
        })
    }

    suspend fun hasAny(userId: String): Boolean {
        return try {
            val snapshot = Firebase.firestore.collection(COLLECTION_USERS)
                .document(userId)
                .collection(COLLECTION_WEIGHT_LOGS)
                .limit(1)
                .get()
                .await()
            !snapshot.isEmpty
        } catch (e: Exception) {
            handleException(e)
            false
        }
    }

    fun timeToWeightLog(): Boolean {
        val user = LocalUser.user
        val lastLogTime = user.lastWeightLogTimestamp
        val daysSince = if (lastLogTime > 0) {
            TimeUnit.MILLISECONDS.toDays(System.currentTimeMillis() - lastLogTime)
        } else {
            (INTERVAL_DAYS + 1).toLong()
        }
        return daysSince >= INTERVAL_DAYS
    }

    suspend fun deleteLog(log: WeightLog): DataWrapper<Unit> {
        return try {
            Firebase.firestore.collection(COLLECTION_USERS)
                .document(log.userId)
                .collection(COLLECTION_WEIGHT_LOGS)
                .document(log.logId)
                .delete()
                .await()

            val photoUrl = log.photoUrl
            if (photoUrl.isNotEmpty()) {
                ImageUploader.deleteImage(photoUrl)
            }

            DataWrapper.Success(Unit)
        } catch (e: Exception) {
            handleException(e)
            DataWrapper.Error(e.localizedMessage)
        }
    }
}