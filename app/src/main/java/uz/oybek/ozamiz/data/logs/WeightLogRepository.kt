package uz.oybek.ozamiz.data.logs

import uz.oybek.ozamiz.common.util.orZero
import android.net.Uri
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import uz.oybek.ozamiz.common.result.BaseResult
import uz.oybek.ozamiz.data.firebase.FirebasePaths
import uz.oybek.ozamiz.data.firebase.generateKey
import uz.oybek.ozamiz.data.firebase.uploader.ImageUploader
import uz.oybek.ozamiz.data.logs.model.WeightLog
import uz.oybek.ozamiz.data.user.local.LocalUser
import uz.oybek.ozamiz.data.user.local.user
import uz.oybek.ozamiz.util.handleException
import java.util.concurrent.TimeUnit

object WeightLogRepository {

    private const val INTERVAL_DAYS = 7

    suspend fun add(
        userId: String,
        weightKg: Double,
        photoUri: Uri?,
        shouldBlur: Boolean = false
    ): BaseResult<WeightLog, String> {
        val uploadedUrl = if (photoUri != null) {
            ImageUploader.uploadImage(
                photoUri,
                FirebasePaths.Storage.weightLogs(userId), shouldBlurFaces = shouldBlur
            ) ?: ""
        } else {
            ""
        }

        val logId = generateKey()

        val currentTime = System.currentTimeMillis()
        val newLog = WeightLog(
            logId = logId,
            userId = userId,
            weightKg = weightKg,
            photoUrl = uploadedUrl,
            timestamp = com.google.firebase.firestore.FieldValue.serverTimestamp(),
            timestampLong = currentTime
        )

        return try {
            FirebasePaths.Firestore.weightLogs(userId)
                .document(logId)
                .set(newLog)
                .await()
            BaseResult.Success(newLog)
        } catch (e: Exception) {
            handleException(e)
            BaseResult.Failure(e.message ?: "")
        }
    }

    fun getLogsFlow(userId: String): Flow<BaseResult<List<WeightLog>, String>> = flow {
        if (userId.isEmpty()) return@flow
        emitAll(callbackFlow {
            val registration = FirebasePaths.Firestore.weightLogs(userId)
                .orderBy(WeightLog::timestamp.name, Query.Direction.DESCENDING)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        if (error.code == com.google.firebase.firestore.FirebaseFirestoreException.Code.PERMISSION_DENIED) {
                            close()
                        } else {
                            trySend(BaseResult.Failure(error.message ?: "Xatolik"))
                        }
                        return@addSnapshotListener
                    }

                    if (snapshot != null) {
                        val logs = snapshot.toObjects(WeightLog::class.java)
                        trySend(BaseResult.Success(logs))
                    } else {
                        trySend(BaseResult.Success(emptyList()))
                    }
                }

            awaitClose { registration.remove() }
        })
    }

    fun needShowWeightLog(userId: String): Flow<Boolean> {
        val hasStreak = (user.streakInfo?.currentStreak.orZero()) > 1
        return getLogsFlow(userId).map { result ->
            if (result is BaseResult.Success) {
                val logs = result.data.sortedBy { it.timestampLong }
                if (logs.isNotEmpty()) {
                    val lastLog = logs.last()
                    val oneWeekMs = 7L * 24 * 60 * 60 * 1000
                    System.currentTimeMillis() - lastLog.timestampLong > oneWeekMs
                } else {
                    hasStreak
                }
            } else {
                hasStreak
            }
        }
    }

    suspend fun hasAny(userId: String): Boolean {
        return try {
            val snapshot = FirebasePaths.Firestore.weightLogs(userId)
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

    suspend fun deleteLog(log: WeightLog): BaseResult<Unit, String> {
        return try {
            FirebasePaths.Firestore.weightLogs(log.userId)
                .document(log.logId)
                .delete()
                .await()

            val photoUrl = log.photoUrl
            if (photoUrl.isNotEmpty()) {
                ImageUploader.deleteImage(photoUrl)
            }

            BaseResult.Success(Unit)
        } catch (e: Exception) {
            handleException(e)
            BaseResult.Failure(e.message ?: "")
        }
    }
}