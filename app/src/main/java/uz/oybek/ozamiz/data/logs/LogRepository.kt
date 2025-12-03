package uz.oybek.ozamiz.data.logs

import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.database
import com.google.firebase.database.getValue
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.tasks.await
import uz.oybek.ozamiz.data.firebase.FirebasePagingSource
import uz.oybek.ozamiz.data.logs.model.DailyLogs
import uz.oybek.ozamiz.data.logs.model.ExerciseLog
import uz.oybek.ozamiz.data.logs.model.FoodLog
import uz.oybek.ozamiz.data.logs.model.WaterLog
import uz.oybek.ozamiz.data.utils.DataWrapper
import uz.oybek.ozamiz.data.utils.DateUtils
import uz.oybek.ozamiz.data.utils.DateUtils.getFormattedDateString
import uz.oybek.ozamiz.util.handleException
import java.util.Date

object LogRepository {

    private val database: FirebaseDatabase = Firebase.database
    private val logsRefs: DatabaseReference =
        database.getReference("logs")

    const val FOOD_LOGS_REF = "foodLogs"
    const val TIMESTAMP = "timestamp"

    private fun currentUserId(): String? {
        return Firebase.auth.currentUser?.uid
    }

    fun getTodayFoodLogsPaginated(date: Date)
            : Flow<PagingData<FoodLog>> {
        val userId = currentUserId() ?: return emptyFlow()
        val dateString = getFormattedDateString(
            date,
            DateUtils.YYYY_MM_DD
        )

        val query = logsRefs
            .child(userId)
            .child(dateString)
            .child(FOOD_LOGS_REF)
            .orderByChild(TIMESTAMP)

        return Pager(
            config = PagingConfig(
                pageSize = 8,
                enablePlaceholders = false
            ),
            pagingSourceFactory = {
                FirebasePagingSource(
                    query = query,
                    valueType = FoodLog::class.java
                )
            }
        ).flow
    }

    fun observeLogsForDate(date: Date):
            Flow<DataWrapper<DailyLogs>> = callbackFlow {

        val userId = currentUserId()
        if (userId.isNullOrEmpty()) {
            trySend(DataWrapper.Error())
            close(); return@callbackFlow
        }
        val dateString = getFormattedDateString(
            date,
            DateUtils.YYYY_MM_DD
        )
        val logRefForDate = logsRefs.child(userId).child(dateString)

        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val dailyLogData = snapshot.getValue<DailyLogs>() ?: DailyLogs()
                trySend(DataWrapper.Success(dailyLogData))
            }

            override fun onCancelled(error: DatabaseError) {
                handleException(error.toException(), "ObserveLogsForDateCancelled")
                trySend(DataWrapper.Error(error.message)); close()
            }
        }
        logRefForDate.addValueEventListener(listener)
        awaitClose { logRefForDate.removeEventListener(listener) }
    }

    suspend fun getFoodLogsForDateRange(
        startDateStr: String, endDateStr: String
    ): DataWrapper<List<FoodLog>> {
        val userId = currentUserId() ?: return DataWrapper.Error("Foydalanuvchi topilmadi")
        val userLogsRef = logsRefs.child(userId)

        return try {
            val snapshot =
                userLogsRef.orderByKey().startAt(startDateStr).endAt(endDateStr).get().await()

            val allFoodLogs = mutableListOf<FoodLog>()
            if (snapshot.exists()) {
                snapshot.children.forEach { dateSnapshot ->
                    val dailyData = dateSnapshot.getValue<DailyLogs>()
                    dailyData?.foodLogs?.values?.let { logs ->
                        allFoodLogs.addAll(logs)
                    }
                }
            }
            allFoodLogs.sortByDescending { it.timestamp as? Long ?: 0L }
            DataWrapper.Success(allFoodLogs)

        } catch (e: Exception) {
            handleException(e, "GetFoodLogsRangeError")
            DataWrapper.Error(e.localizedMessage ?: "Loglarni yuklashda xatolik")
        }
    }

    suspend fun addFoodLog(foodLog: FoodLog, date: Date): DataWrapper<Unit> {
        val userId = currentUserId() ?: return DataWrapper.Error("Foydalanuvchi topilmadi")
        val dateString = getFormattedDateString(date)
        val foodLogsRef = logsRefs.child(userId).child(dateString).child("foodLogs")
        val newLogRef = foodLogsRef.push()
        foodLog.logId = newLogRef.key ?: ""
        return try {
            newLogRef.setValue(foodLog).await(); DataWrapper.Success(Unit)
        } catch (e: Exception) {
            handleException(e, "AddFoodLogError")
            DataWrapper.Error(e.localizedMessage ?: "Ovqat logini saqlashda xatolik")
        }
    }

    suspend fun addWaterLog(waterLog: WaterLog, date: Date): DataWrapper<Unit> {
        val userId = currentUserId() ?: return DataWrapper.Error("Foydalanuvchi topilmadi")
        val dateString = getFormattedDateString(date)
        val waterLogsRef = logsRefs.child(userId).child(dateString).child("waterLogs")
        val newLogRef = waterLogsRef.push(); waterLog.logId = newLogRef.key ?: ""
        return try {
            newLogRef.setValue(waterLog).await(); DataWrapper.Success(Unit)
        } catch (e: Exception) {
            handleException(e, "AddWaterLogError")
            DataWrapper.Error(e.localizedMessage ?: "Suv logini saqlashda xatolik")
        }
    }

    suspend fun addExerciseLog(exerciseLog: ExerciseLog, date: Date): DataWrapper<Unit> {
        val userId = currentUserId() ?: return DataWrapper.Error("Foydalanuvchi topilmadi")
        val dateString = getFormattedDateString(date)
        val exerciseLogsRef = logsRefs.child(userId).child(dateString).child("exerciseLogs")
        val newLogRef = exerciseLogsRef.push(); exerciseLog.logId = newLogRef.key ?: ""
        return try {
            newLogRef.setValue(exerciseLog).await(); DataWrapper.Success(Unit)
        } catch (e: Exception) {
            handleException(e, "AddExerciseLogError")
            DataWrapper.Error(e.localizedMessage ?: "Mashq logini saqlashda xatolik")
        }
    }

    suspend fun updateFoodLog(
        logId: String,
        updatedFoodLog: FoodLog,
        date: Date
    ): DataWrapper<Unit> {
        val userId = currentUserId() ?: return DataWrapper.Error("Foydalanuvchi topilmadi")
        val dateString = getFormattedDateString(date)
        if (logId.isEmpty()) return DataWrapper.Error("Log ID bo'sh")
        val logRef = logsRefs.child(userId).child(dateString).child("foodLogs").child(logId)
        val updates = mapOf<String, Any?>(
            "description" to updatedFoodLog.description,
            "calories" to updatedFoodLog.calories,
            "protein" to updatedFoodLog.protein,
            "fat" to updatedFoodLog.fat,
            "carbs" to updatedFoodLog.carbs,
            "mealType" to updatedFoodLog.mealType,
            "photoUrl" to updatedFoodLog.photoUrl,
            "aiInsights" to updatedFoodLog.aiInsights,
            "aiGenerated" to updatedFoodLog.aiGenerated
        )
        return try {
            logRef.updateChildren(updates).await(); DataWrapper.Success(Unit)
        } catch (e: Exception) {
            handleException(e, "UpdateFoodLogError")
            DataWrapper.Error(e.localizedMessage ?: "Ovqat logini yangilashda xatolik")
        }
    }

    suspend fun deleteFoodLog(logId: String, date: Date): DataWrapper<Unit> {
        val userId = currentUserId() ?: return DataWrapper.Error("Foydalanuvchi topilmadi")
        val dateString = getFormattedDateString(date)
        if (logId.isEmpty()) return DataWrapper.Error("Log ID bo'sh")
        val logRef = logsRefs.child(userId).child(dateString).child("foodLogs").child(logId)
        return try {
            logRef.removeValue().await(); DataWrapper.Success(Unit)
        } catch (e: Exception) {
            handleException(e, "DeleteFoodLogError")
            DataWrapper.Error(e.localizedMessage ?: "Ovqat logini o'chirishda xatolik")
        }
    }

    suspend fun deleteWaterLog(logId: String, date: Date): DataWrapper<Unit> {
        val userId = currentUserId() ?: return DataWrapper.Error("Foydalanuvchi topilmadi")
        val dateString = getFormattedDateString(date)
        if (logId.isEmpty()) return DataWrapper.Error("Log ID bo'sh")
        val logRef = logsRefs.child(userId).child(dateString).child("waterLogs").child(logId)
        return try {
            logRef.removeValue().await(); DataWrapper.Success(Unit)
        } catch (e: Exception) {
            handleException(e, "DeleteWaterLogError")
            DataWrapper.Error(e.localizedMessage ?: "Suv logini o'chirishda xatolik")
        }
    }

    suspend fun deleteExerciseLog(logId: String, date: Date): DataWrapper<Unit> {
        val userId = currentUserId() ?: return DataWrapper.Error("Foydalanuvchi topilmadi")
        val dateString = getFormattedDateString(date)
        if (logId.isEmpty()) return DataWrapper.Error("Log ID bo'sh")
        val logRef = logsRefs.child(userId).child(dateString).child("exerciseLogs").child(logId)
        return try {
            logRef.removeValue().await(); DataWrapper.Success(Unit)
        } catch (e: Exception) {
            handleException(e, "DeleteExerciseLogError")
            DataWrapper.Error(e.localizedMessage ?: "Mashq logini o'chirishda xatolik")
        }
    }
}