package uz.oybek.ozamiz.data.logs

import androidx.core.net.toUri
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import com.google.firebase.crashlytics.crashlytics
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ServerValue
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import uz.oybek.ozamiz.data.ai.Dietolog
import uz.oybek.ozamiz.data.firebase.FirebaseConstants
import uz.oybek.ozamiz.data.firebase.FirebasePagingSource
import uz.oybek.ozamiz.data.firebase.FirebasePaths
import uz.oybek.ozamiz.data.firebase.currentUserId
import uz.oybek.ozamiz.data.firebase.generateKey
import uz.oybek.ozamiz.data.firebase.uploader.ImagePaths
import uz.oybek.ozamiz.data.firebase.uploader.ImageUploader
import uz.oybek.ozamiz.data.logs.model.FoodLog
import uz.oybek.ozamiz.data.utils.dateApiFormat
import java.util.Date


object FoodLogRepository {

    fun foodLogsPaged(date: Date): Flow<PagingData<FoodLog>> {
        val date = date.dateApiFormat()
        val query = FirebasePaths
            .foodLogsReference(currentUserId!!, date)
            .orderByChild(FirebaseConstants.TIMESTAMP)
        return Pager(
            PagingConfig(
                pageSize = 8,
                enablePlaceholders = false
            )
        ) {
            FirebasePagingSource(
                query = query,
                valueType = FoodLog::class.java
            )
        }.flow
    }

    fun getLatestLogs(date: Date, limit: Int = 2): Flow<List<FoodLog>> = callbackFlow {
        val userId = currentUserId
        val dateStr = date.dateApiFormat()
        val query = FirebasePaths
            .foodLogsReference(userId, dateStr)
            .orderByChild(FirebaseConstants.TIMESTAMP)
            .limitToLast(limit)

        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val logs = snapshot.children.mapNotNull { it.getValue(FoodLog::class.java) }
                    .reversed()
                trySend(logs)
            }

            override fun onCancelled(error: DatabaseError) {
                trySend(emptyList())
            }
        }
        query.addValueEventListener(listener)
        awaitClose { query.removeEventListener(listener) }
    }

    fun getAllLogs(date: Date): Flow<List<FoodLog>> = callbackFlow {
        val userId = currentUserId
        val dateStr = date.dateApiFormat()
        val query = FirebasePaths
            .foodLogsReference(userId, dateStr)

        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val logs = snapshot.children.mapNotNull { it.getValue(FoodLog::class.java) }
                trySend(logs)
            }

            override fun onCancelled(error: DatabaseError) {
                if (error.code == DatabaseError.PERMISSION_DENIED) {
                    close()
                } else {
                    close(error.toException())
                }
            }
        }
        query.addValueEventListener(listener)
        awaitClose { query.removeEventListener(listener) }
    }

    suspend fun getFoodLog(logId: String, date: Date, uid: String? = null): FoodLog? {
        val userId = uid ?: currentUserId ?: return null
        val dateStr = date.dateApiFormat()
        return try {
            FirebasePaths.foodLogsReference(userId, dateStr)
                .child(logId)
                .get()
                .await()
                .getValue(FoodLog::class.java)
        } catch (e: Exception) {
            com.google.firebase.Firebase.crashlytics.recordException(e)
            null
        }
    }

    suspend fun add(image: String?, foodLog: FoodLog, date: Date): Boolean {
        currentUserId
        try {
            val dateStr = date.dateApiFormat()
            val reference = FirebasePaths.foodLogsReference(currentUserId!!, dateStr)
            foodLog.logId = generateKey()
            reference
                .child(foodLog.logId)
                .setValue(foodLog)
                .await()

            val uploadImageUrl = if (image != null)
                ImageUploader.uploadImage(
                    image.toUri(),
                    ImagePaths.ANALYZE_FOOD
                ) else null
            if (uploadImageUrl != null) {
                foodLog.photoUrl = uploadImageUrl
                reference.child(foodLog.logId)
                    .child(FoodLog::photoUrl.name)
                    .setValue(uploadImageUrl)
            }
        } catch (e: Exception) {
            com.google.firebase.Firebase.crashlytics.recordException(e)
            return false
        }

        DailySummaryRepository.updateSummary(
            date = date,
            caloriesDelta = foodLog.calories,
            proteinDelta = foodLog.protein ?: 0.0,
            carbsDelta = foodLog.carbs ?: 0.0,
            fatDelta = foodLog.fat ?: 0.0,
            mealsDelta = 1
        )

        Dietolog.onFoodLogAdded(foodLog)
        return true

    }

    suspend fun update(
        logId: String,
        updated: FoodLog,
        date: Date,
        targetUid: String? = null
    ): Boolean {
        val userId = targetUid ?: currentUserId ?: return false
        if (logId.isEmpty()) return false
        val dateStr = date.dateApiFormat()
        val reference =
            FirebasePaths.foodLogsReference(userId, dateStr)
        updated.timestamp = ServerValue.TIMESTAMP
        return try {
            reference
                .child(logId)
                .setValue(updated)
                .await()

            // Calculate deltas
            val oldLog = getFoodLog(logId, date, userId)
            val calDelta = updated.calories - (oldLog?.calories ?: 0)
            val proDelta = (updated.protein ?: 0.0) - (oldLog?.protein ?: 0.0)
            val carbDelta = (updated.carbs ?: 0.0) - (oldLog?.carbs ?: 0.0)
            val fatDelta = (updated.fat ?: 0.0) - (oldLog?.fat ?: 0.0)

            DailySummaryRepository.updateSummary(
                date = date,
                targetUid = userId,
                caloriesDelta = calDelta,
                proteinDelta = proDelta,
                carbsDelta = carbDelta,
                fatDelta = fatDelta
            )
            true
        } catch (e: Exception) {
            com.google.firebase.Firebase.crashlytics.recordException(e)
            false
        }
    }

    suspend fun delete(
        logId: String,
        date: Date,
        targetUid: String? = null
    ): Boolean {
        val userId = targetUid ?: currentUserId ?: return false
        if (logId.isEmpty()) return false
        val dateStr = date.dateApiFormat()
        val reference =
            FirebasePaths.foodLogsReference(userId, dateStr)
        return try {
            val oldLog = getFoodLog(logId, date, userId)
            reference
                .child(logId)
                .removeValue()
                .await()

            if (oldLog != null) {
                DailySummaryRepository.updateSummary(
                    date = date,
                    targetUid = userId,
                    caloriesDelta = -oldLog.calories,
                    proteinDelta = -(oldLog.protein ?: 0.0),
                    carbsDelta = -(oldLog.carbs ?: 0.0),
                    fatDelta = -(oldLog.fat ?: 0.0),
                    mealsDelta = -1
                )
            }
            true
        } catch (e: Exception) {
            com.google.firebase.Firebase.crashlytics.recordException(e)
            false
        }
    }

}