package uz.oybek.ozamiz.data.logs

import androidx.core.net.toUri
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import com.google.firebase.crashlytics.crashlytics
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import uz.oybek.ozamiz.applicationScope
import uz.oybek.ozamiz.data.community.CommunityPostRepository
import uz.oybek.ozamiz.data.community.model.CommunityPost
import uz.oybek.ozamiz.data.community.model.PostType.FOOD
import uz.oybek.ozamiz.data.firebase.FirebaseConstants
import uz.oybek.ozamiz.data.firebase.FirebasePagingSource
import uz.oybek.ozamiz.data.firebase.FirebasePaths
import uz.oybek.ozamiz.data.firebase.crashlytics
import uz.oybek.ozamiz.data.firebase.currentUserId
import uz.oybek.ozamiz.data.firebase.generateKey
import uz.oybek.ozamiz.data.firebase.uploader.ImageUploader
import uz.oybek.ozamiz.data.logs.model.FoodLog
import uz.oybek.ozamiz.data.user.local.user
import uz.oybek.ozamiz.data.utils.dateApiFormat
import uz.oybek.ozamiz.ui.feature.premium.PremiumUtils
import java.util.Date


object FoodLogRepository {

    fun foodLogsPaged(date: Date): Flow<PagingData<FoodLog>> {
        val userId = currentUserId
        if (userId.isBlank()) {
            return kotlinx.coroutines.flow.flowOf(PagingData.empty())
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

    fun getLatestLogs(date: Date, limit: Int = 2): Flow<List<FoodLog>> = callbackFlow {
        val userId = currentUserId
        if (userId.isBlank()) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }
        val dateStr = date.dateApiFormat()
        val query = FirebasePaths.Database
            .foodLogs(userId, dateStr)
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
        try {
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
        } catch (e: Exception) {
            com.google.firebase.Firebase.crashlytics.recordException(e)
            close(e)
        }
    }

    suspend fun getFoodLog(logId: String, date: Date, uid: String? = null): FoodLog? {
        val userId = uid ?: currentUserId
        if (userId.isBlank()) return null
        val dateStr = date.dateApiFormat()
        return try {
            FirebasePaths.Database.foodLogs(userId, dateStr)
                .child(logId)
                .get()
                .await()
                .getValue(FoodLog::class.java)
        } catch (e: Exception) {
            com.google.firebase.Firebase.crashlytics.recordException(e)
            null
        }
    }

    suspend fun add(
        image: String?,
        foodLog: FoodLog,
        date: Date,
        shouldShare: Boolean
    ): Boolean {
        val userId = currentUserId
        if (userId.isBlank()) return false

        try {
            foodLog.logId = generateKey()
            val tempPhotoUrl = foodLog.photoUrl
            val dateStr = date.dateApiFormat()
            val reference = FirebasePaths.Database.foodLogs(userId, dateStr)

            reference
                .child(foodLog.logId)
                .setValue(foodLog)
                .await()

            uz.oybek.ozamiz.data.config.AppPreference.lastFoodLogTime = System.currentTimeMillis()

            LogRepository.updateSummary(
                date = date,
                caloriesDelta = foodLog.calories,
                proteinDelta = foodLog.protein ?: 0.0,
                carbsDelta = foodLog.carbs ?: 0.0,
                fatDelta = foodLog.fat ?: 0.0,
                mealsDelta = 1
            )
            applicationScope.launch {
                try {
                    val uploadImageUrl = if (image != null && !image.startsWith("http"))
                        ImageUploader.uploadImage(
                            image.toUri(),
                            FirebasePaths.Storage.foodLogs(userId)
                        ) else tempPhotoUrl

                    if (uploadImageUrl != tempPhotoUrl) {
                        foodLog.photoUrl = uploadImageUrl
                        reference
                            .child(foodLog.logId)
                            .setValue(foodLog)
                            .await()
                    }

                    if (shouldShare) {
                        launch {
                            if (user.valid) {
                                runCatching {
                                    val post = CommunityPost(
                                        userId = user.uid,
                                        userName = user.name.ifBlank { "Foydalanuvchi" },
                                        userAvatarUrl = user.avatarUrl,
                                        photoUrl = uploadImageUrl.orEmpty(),
                                        photos = emptyList(),
                                        title = foodLog.description,
                                        description = "",
                                        foodName = foodLog.description,
                                        calories = foodLog.calories,
                                        protein = foodLog.protein,
                                        fat = foodLog.fat,
                                        carbs = foodLog.carbs,
                                        healthScore = foodLog.healthLevel ?: 0,
                                        userScore = user.scorePoints,
                                        verified = true,
                                        isVisible = true,
                                        type = FOOD,
                                        isPremiumUser = PremiumUtils.isPremium(user)
                                    )
                                    CommunityPostRepository.addPost(post)
                                }
                            }
                        }
                    }
                } catch (e: Exception) {
                    crashlytics.recordException(e)
                }
            }
        } catch (e: Exception) {
            crashlytics.recordException(e)
            return false
        }
        return true
    }

    suspend fun update(
        logId: String,
        updated: FoodLog,
        date: Date,
        targetUid: String? = null
    ): Boolean {
        val userId = targetUid ?: currentUserId
        if (userId.isBlank() || logId.isEmpty()) return false
        val dateStr = date.dateApiFormat()
        val reference =
            FirebasePaths.Database.foodLogs(userId, dateStr)
        updated.timestamp = System.currentTimeMillis()
        return try {
            val oldLog = getFoodLog(logId, date, userId)
            reference
                .child(logId)
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
            com.google.firebase.Firebase.crashlytics.recordException(e)
            false
        }
    }

    suspend fun delete(
        logId: String,
        date: Date,
        targetUid: String? = null
    ): Boolean {
        val userId = targetUid ?: currentUserId
        if (userId.isBlank() || logId.isEmpty()) return false
        val dateStr = date.dateApiFormat()
        val reference =
            FirebasePaths.Database.foodLogs(userId, dateStr)
        return try {
            val oldLog = getFoodLog(logId, date, userId)
            reference
                .child(logId)
                .removeValue()
                .await()

            if (oldLog != null) {
                LogRepository.updateSummary(
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