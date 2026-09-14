package uz.oybek.ozamiz.data.logs

import androidx.core.net.toUri
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.tasks.await
import uz.oybek.ozamiz.applicationScope
import uz.oybek.ozamiz.data.config.AppPreference
import uz.oybek.ozamiz.data.firebase.FirebaseConstants
import uz.oybek.ozamiz.data.firebase.FirebasePagingSource
import uz.oybek.ozamiz.data.firebase.FirebasePaths
import uz.oybek.ozamiz.data.firebase.currentUserId
import uz.oybek.ozamiz.data.firebase.generateKey
import uz.oybek.ozamiz.data.firebase.uploader.ImageUploader
import uz.oybek.ozamiz.data.level.PointsEvent
import uz.oybek.ozamiz.data.level.PointsRepository
import uz.oybek.ozamiz.data.logs.model.FoodLog
import uz.oybek.ozamiz.data.streak.StreakRepository
import uz.oybek.ozamiz.data.utils.dateApiFormat
import uz.oybek.ozamiz.data.utils.recordException
import uz.oybek.ozamiz.data.utils.roundTo1Decimal
import yads.nu
import yads.tr
import java.util.Date


object FoodLogRepository {

    fun foodLogsPaged(date: Date): Flow<PagingData<FoodLog>> {
        val userId = currentUserId
        if (userId.isBlank()) {
            return flowOf(PagingData.empty())
        }
        val dateStr = date.dateApiFormat()
        val query = FirebasePaths.Database
            .foodLogs(userId, dateStr)
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

    fun getAllLogs(date: Date): Flow<List<FoodLog>> = callbackFlow {
        val userId = currentUserId
        if (userId.isBlank()) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }
        val dateStr = date.dateApiFormat()
        val query = FirebasePaths.Database.foodLogs(userId, dateStr)

        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val logs = snapshot.children.mapNotNull {
                    it.getValue(FoodLog::class.java)
                }
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
        val userId = uid ?: currentUserId
        if (userId.isBlank() || logId.isBlank()) return null
        val dateStr = date.dateApiFormat()
        return try {
            FirebasePaths.Database.foodLogs(userId, dateStr)
                .child(logId)
                .get()
                .await()
                .getValue(FoodLog::class.java)
        } catch (e: Exception) {
            recordException(e)
            null
        }
    }

    suspend fun add(
        foodLog: FoodLog,
        date: Date
    ): Boolean {
        val userId = currentUserId
        if (userId.isBlank()) return false

        return applicationScope.async(Dispatchers.IO) {
            try {
                foodLog.logId = generateKey()
                val dateStr = date.dateApiFormat()
                val reference = FirebasePaths.Database.foodLogs(userId, dateStr)

                val isSuccess = suspendCancellableCoroutine { continuation ->
                    reference
                        .child(foodLog.logId)
                        .setValue(foodLog)
                        .addOnSuccessListener {
                            if (continuation.isActive) {
                                continuation.resume(true, null)
                            }
                        }
                        .addOnFailureListener { exception ->
                            recordException(exception)
                            if (continuation.isActive) {
                                continuation.resume(false, null)
                            }
                        }
                }
                if (isSuccess) {
                    applicationScope.launch {
                        if (!foodLog.photoUrl.isNullOrBlank()) {
                            try {
                                val photo = uploadImage(foodLog.photoUrl, userId)
                                reference
                                    .child(foodLog.logId)
                                    .child(FoodLog::photoUrl.name)
                                    .setValue(photo)
                            } catch (e: Exception) {
                                recordException(e)
                            }
                        }
                    }
                    if (foodLog.aiGenerated) {
                        AppPreference.incrementFoodLogLimitCount()
                    }
                    LogRepository.updateSummary(
                        date = date,
                        caloriesDelta = foodLog.calories,
                        proteinDelta = foodLog.protein ?: 0.0,
                        carbsDelta = foodLog.carbs ?: 0.0,
                        fatDelta = foodLog.fat ?: 0.0,
                        mealsDelta = 1
                    )
                    StreakRepository.updateStreak()
                    PointsRepository.onEvent(
                        PointsEvent.AddFood(foodLog.healthLevel ?: 0)
                    )
                }
                isSuccess
            } catch (e: Exception) {
                recordException(e)
                false
            }
        }.await()
    }

    private suspend fun uploadImage(
        image: String?,
        userId: String
    ): String {
        return applicationScope.async {
            try {
                return@async if (image != null && !image.startsWith("http")) {
                    ImageUploader.uploadImage(
                        image.toUri(),
                        FirebasePaths.Storage.foodLogs(userId)
                    ) ?: ""
                } else ""
            } catch (e: Exception) {
                recordException(e)
                return@async ""
            }
        }.await()
    }

    suspend fun update(
        oldLog: FoodLog?,
        updated: FoodLog,
        date: Date,
        targetUid: String? = null
    ): Boolean {
        val userId = targetUid ?: currentUserId

        if (userId.isBlank() || updated.logId.isEmpty()) return false
        val dateStr = date.dateApiFormat()
        val reference = FirebasePaths.Database.foodLogs(userId, dateStr)

        return try {
            reference
                .child(updated.logId)
                .setValue(updated)
                .await()

            val calDelta = updated.calories - (oldLog?.calories ?: 0)
            val proDelta = (updated.protein ?: 0.0) - (oldLog?.protein ?: 0.0)
            val carbDelta = (updated.carbs ?: 0.0) - (oldLog?.carbs ?: 0.0)
            val fatDelta = (updated.fat ?: 0.0) - (oldLog?.fat ?: 0.0)

            LogRepository.updateSummary(
                date = date,
                targetUid = userId,
                caloriesDelta = calDelta,
                proteinDelta = proDelta,
                carbsDelta = carbDelta,
                fatDelta = fatDelta
            )
            true
        } catch (e: Exception) {
            recordException(e)
            false
        }
    }

    suspend fun delete(
        foodLog: FoodLog,
        date: Date,
        targetUid: String? = null
    ): Boolean {
        val userId = targetUid ?: currentUserId
        val logId = foodLog.logId
        if (userId.isBlank() || logId.isEmpty()) return false
        val dateStr = date.dateApiFormat()
        val reference = FirebasePaths.Database.foodLogs(userId, dateStr)
        return try {
            reference
                .child(logId)
                .removeValue()
                .await()

            LogRepository.updateSummary(
                date = date,
                targetUid = userId,
                caloriesDelta = -foodLog.calories,
                proteinDelta = -(foodLog.protein ?: 0.0),
                carbsDelta = -(foodLog.carbs ?: 0.0),
                fatDelta = -(foodLog.fat ?: 0.0),
                mealsDelta = -1
            )
            true
        } catch (e: Exception) {
            recordException(e)
            false
        }
    }

}