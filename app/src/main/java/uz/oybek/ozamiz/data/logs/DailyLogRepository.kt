package uz.oybek.ozamiz.data.logs

import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.getValue
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import uz.oybek.ozamiz.data.DataWrapper
import uz.oybek.ozamiz.data.firebase.FirebaseInstances
import uz.oybek.ozamiz.data.info.DailyLogData
import uz.oybek.ozamiz.data.info.ExerciseLog
import uz.oybek.ozamiz.data.info.FoodLog
import uz.oybek.ozamiz.data.info.WaterLog
import uz.oybek.ozamiz.handleException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class DailyLogRepository {

    private val database: FirebaseDatabase = FirebaseInstances.database
    private val logsRootRef: DatabaseReference = database.getReference("daily_logs")

    private fun getCurrentUserId(): String? {
        return FirebaseInstances.auth.currentUser?.uid
    }

    private fun getFormattedDateString(date: Date): String {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        return dateFormat.format(date)
    }

    fun observeLogsForDate(date: Date): Flow<DataWrapper<DailyLogData>> = callbackFlow {
        val userId = getCurrentUserId()
        if (userId.isNullOrEmpty()) {
            trySend(DataWrapper.Error("Foydalanuvchi avtorizatsiyadan o'tmagan"))
            close(); return@callbackFlow
        }
        val dateString = getFormattedDateString(date)
        val logRefForDate = logsRootRef.child(userId).child(dateString)

        trySend(DataWrapper.Loading())
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val dailyLogData = snapshot.getValue<DailyLogData>() ?: DailyLogData()
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
        val userId = getCurrentUserId() ?: return DataWrapper.Error("Foydalanuvchi topilmadi")
        val userLogsRef = logsRootRef.child(userId)

        return try {
            val snapshot =
                userLogsRef.orderByKey().startAt(startDateStr).endAt(endDateStr).get().await()

            val allFoodLogs = mutableListOf<FoodLog>()
            if (snapshot.exists()) {
                snapshot.children.forEach { dateSnapshot ->
                    val dailyData = dateSnapshot.getValue<DailyLogData>()
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
        val userId = getCurrentUserId() ?: return DataWrapper.Error("Foydalanuvchi topilmadi")
        val dateString = getFormattedDateString(date)
        val foodLogsRef = logsRootRef.child(userId).child(dateString).child("foodLogs")
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
        val userId = getCurrentUserId() ?: return DataWrapper.Error("Foydalanuvchi topilmadi")
        val dateString = getFormattedDateString(date)
        val waterLogsRef = logsRootRef.child(userId).child(dateString).child("waterLogs")
        val newLogRef = waterLogsRef.push(); waterLog.logId = newLogRef.key ?: ""
        return try {
            newLogRef.setValue(waterLog).await(); DataWrapper.Success(Unit)
        } catch (e: Exception) {
            handleException(e, "AddWaterLogError")
            DataWrapper.Error(e.localizedMessage ?: "Suv logini saqlashda xatolik")
        }
    }

    suspend fun addExerciseLog(exerciseLog: ExerciseLog, date: Date): DataWrapper<Unit> {
        val userId = getCurrentUserId() ?: return DataWrapper.Error("Foydalanuvchi topilmadi")
        val dateString = getFormattedDateString(date)
        val exerciseLogsRef = logsRootRef.child(userId).child(dateString).child("exerciseLogs")
        val newLogRef = exerciseLogsRef.push(); exerciseLog.logId = newLogRef.key ?: ""
        return try {
            newLogRef.setValue(exerciseLog).await(); DataWrapper.Success(Unit)
        } catch (e: Exception) {
            handleException(e, "AddExerciseLogError")
            DataWrapper.Error(e.localizedMessage ?: "Mashq logini saqlashda xatolik")
        }
    }

    suspend fun updateFoodLog(logId: String, updatedFoodLog: FoodLog, date: Date): DataWrapper<Unit> {
        val userId = getCurrentUserId() ?: return DataWrapper.Error("Foydalanuvchi topilmadi")
        val dateString = getFormattedDateString(date)
        if (logId.isEmpty()) return DataWrapper.Error("Log ID bo'sh")
        val logRef = logsRootRef.child(userId).child(dateString).child("foodLogs").child(logId)
        val updates = mapOf<String, Any?>(
            "description" to updatedFoodLog.description,
            "calories" to updatedFoodLog.calories,
            "protein" to updatedFoodLog.protein,
            "fat" to updatedFoodLog.fat,
            "carbs" to updatedFoodLog.carbs,
            "mealType" to updatedFoodLog.mealType,
            "photoUrl" to updatedFoodLog.photoUrl,
            "analysisInsights" to updatedFoodLog.analysisInsights,
            "estimatedByAI" to updatedFoodLog.estimatedByAI
        )
        return try {
            logRef.updateChildren(updates).await(); DataWrapper.Success(Unit)
        } catch (e: Exception) {
            handleException(e, "UpdateFoodLogError")
            DataWrapper.Error(e.localizedMessage ?: "Ovqat logini yangilashda xatolik")
        }
    }

    suspend fun deleteFoodLog(logId: String, date: Date): DataWrapper<Unit> {
        val userId = getCurrentUserId() ?: return DataWrapper.Error("Foydalanuvchi topilmadi")
        val dateString = getFormattedDateString(date)
        if (logId.isEmpty()) return DataWrapper.Error("Log ID bo'sh")
        val logRef = logsRootRef.child(userId).child(dateString).child("foodLogs").child(logId)
        return try {
            logRef.removeValue().await(); DataWrapper.Success(Unit)
        } catch (e: Exception) {
            handleException(e, "DeleteFoodLogError")
            DataWrapper.Error(e.localizedMessage ?: "Ovqat logini o'chirishda xatolik")
        }
    }

    suspend fun deleteWaterLog(logId: String, date: Date): DataWrapper<Unit> {
        val userId = getCurrentUserId() ?: return DataWrapper.Error("Foydalanuvchi topilmadi")
        val dateString = getFormattedDateString(date)
        if (logId.isEmpty()) return DataWrapper.Error("Log ID bo'sh")
        val logRef = logsRootRef.child(userId).child(dateString).child("waterLogs").child(logId)
        return try {
            logRef.removeValue().await(); DataWrapper.Success(Unit)
        } catch (e: Exception) {
            handleException(e, "DeleteWaterLogError")
            DataWrapper.Error(e.localizedMessage ?: "Suv logini o'chirishda xatolik")
        }
    }

    suspend fun deleteExerciseLog(logId: String, date: Date): DataWrapper<Unit> {
        val userId = getCurrentUserId() ?: return DataWrapper.Error("Foydalanuvchi topilmadi")
        val dateString = getFormattedDateString(date)
        if (logId.isEmpty()) return DataWrapper.Error("Log ID bo'sh")
        val logRef = logsRootRef.child(userId).child(dateString).child("exerciseLogs").child(logId)
        return try {
            logRef.removeValue().await(); DataWrapper.Success(Unit)
        } catch (e: Exception) {
            handleException(e, "DeleteExerciseLogError")
            DataWrapper.Error(e.localizedMessage ?: "Mashq logini o'chirishda xatolik")
        }
    }
}