package uz.oybek.ozamiz.data.logs

import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.ktx.getValue
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
import uz.oybek.ozamiz.handleException // handleException importi
import java.text.SimpleDateFormat
import java.util.Calendar // Calendar importi
import java.util.Date
import java.util.Locale
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine // suspendCoroutine importi

class DailyLogRepository {

    private val database: FirebaseDatabase = FirebaseInstances.database
    private val logsRootRef: DatabaseReference = database.getReference("daily_logs")

    private fun getCurrentUserId(): String? {
        return FirebaseInstances.auth.currentUser?.uid
    }

    // Sana formatini olish uchun yordamchi funksiya
    private fun getFormattedDateString(date: Date): String {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        return dateFormat.format(date)
    }

    private fun getTodayDateString(): String {
        return getFormattedDateString(Date())
    }

    fun observeTodayLogs(): Flow<DataWrapper<DailyLogData>> = callbackFlow {
        val userId = getCurrentUserId()
        if (userId.isNullOrEmpty()) {
            trySend(DataWrapper.Error("Foydalanuvchi avtorizatsiyadan o'tmagan"))
            close(); return@callbackFlow
        }
        val todayLogRef = logsRootRef.child(userId).child(getTodayDateString())
        trySend(DataWrapper.Loading())
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val dailyLogData = snapshot.getValue<DailyLogData>() ?: DailyLogData()
                trySend(DataWrapper.Success(dailyLogData))
            }
            override fun onCancelled(error: DatabaseError) {
                handleException(error.toException(), "ObserveTodayLogsCancelled") // Handle exception
                trySend(DataWrapper.Error(error.message)); close()
            }
        }
        todayLogRef.addValueEventListener(listener)
        awaitClose { todayLogRef.removeEventListener(listener) }
    }

    // <<< YANGI FUNKSIYA START >>>
    /**
     * Belgilangan oraliqdagi FoodLog'larni oladi.
     * startDate va endDate "yyyy-MM-dd" formatida bo'lishi kerak.
     * Hozircha Firebase Realtime DB ning sodda query imkoniyatlari bilan ishlaydi.
     * Agar juda ko'p data bo'lsa, optimallashtirish kerak bo'ladi (masalan, Firestore yoki Cloud Functions).
     */
    suspend fun getFoodLogsForDateRange(startDateStr: String, endDateStr: String): DataWrapper<List<FoodLog>> {
        val userId = getCurrentUserId() ?: return DataWrapper.Error("Foydalanuvchi topilmadi")
        val userLogsRef = logsRootRef.child(userId)

        return try {
            // Firebase RTDB query 'orderByKey().startAt().endAt()' orqali ishlaydi
            // Bu yerda kalitlar sana (YYYY-MM-DD) formatida bo'lishi kerak.
            val snapshot = userLogsRef
                .orderByKey()
                .startAt(startDateStr)
                .endAt(endDateStr)
                .get()
                .await()

            val allFoodLogs = mutableListOf<FoodLog>()
            if (snapshot.exists()) {
                snapshot.children.forEach { dateSnapshot -> // Har bir kun uchun snapshot
                    val dailyData = dateSnapshot.getValue<DailyLogData>()
                    dailyData?.foodLogs?.values?.let { logs ->
                        allFoodLogs.addAll(logs)
                    }
                }
            }
            // Ma'lumotlarni vaqt bo'yicha saralash (agar kerak bo'lsa)
            allFoodLogs.sortByDescending { it.timestamp as? Long ?: 0L }
            DataWrapper.Success(allFoodLogs)

        } catch (e: Exception) {
            handleException(e, "GetFoodLogsRangeError")
            DataWrapper.Error(e.localizedMessage ?: "Loglarni yuklashda xatolik")
        }
    }
    // <<< YANGI FUNKSIYA END >>>


    // Mavjud add, update, delete funksiyalari...
    suspend fun addFoodLog(foodLog: FoodLog): DataWrapper<Unit> {
        val userId = getCurrentUserId() ?: return DataWrapper.Error("Foydalanuvchi topilmadi")
        val foodLogsRef = logsRootRef.child(userId).child(getTodayDateString()).child("foodLogs")
        val newLogRef = foodLogsRef.push()
        foodLog.logId = newLogRef.key ?: ""
        return try { newLogRef.setValue(foodLog).await(); DataWrapper.Success(Unit) }
        catch (e: Exception) {
            handleException(e, "AddFoodLogError") // Handle exception
            DataWrapper.Error(e.localizedMessage ?: "Ovqat logini saqlashda xatolik")
        }
    }

    suspend fun addWaterLog(waterLog: WaterLog): DataWrapper<Unit> {
        val userId = getCurrentUserId() ?: return DataWrapper.Error("Foydalanuvchi topilmadi")
        val waterLogsRef = logsRootRef.child(userId).child(getTodayDateString()).child("waterLogs")
        val newLogRef = waterLogsRef.push(); waterLog.logId = newLogRef.key ?: ""
        return try { newLogRef.setValue(waterLog).await(); DataWrapper.Success(Unit) }
        catch (e: Exception) {
            handleException(e, "AddWaterLogError") // Handle exception
            DataWrapper.Error(e.localizedMessage ?: "Suv logini saqlashda xatolik")
        }
    }

    suspend fun addExerciseLog(exerciseLog: ExerciseLog): DataWrapper<Unit> {
        val userId = getCurrentUserId() ?: return DataWrapper.Error("Foydalanuvchi topilmadi")
        val exerciseLogsRef = logsRootRef.child(userId).child(getTodayDateString()).child("exerciseLogs")
        val newLogRef = exerciseLogsRef.push(); exerciseLog.logId = newLogRef.key ?: ""
        return try { newLogRef.setValue(exerciseLog).await(); DataWrapper.Success(Unit) }
        catch (e: Exception) {
            handleException(e, "AddExerciseLogError") // Handle exception
            DataWrapper.Error(e.localizedMessage ?: "Mashq logini saqlashda xatolik")
        }
    }

    suspend fun updateFoodLog(logId: String, updatedFoodLog: FoodLog): DataWrapper<Unit> {
        val userId = getCurrentUserId() ?: return DataWrapper.Error("Foydalanuvchi topilmadi")
        if (logId.isEmpty()) return DataWrapper.Error("Log ID bo'sh")
        val logRef = logsRootRef.child(userId).child(getTodayDateString()).child("foodLogs").child(logId)
        // O'zgartirilishi mumkin bo'lgan maydonlarni map qilish yaxshiroq
        val updates = mapOf<String, Any?>(
            "description" to updatedFoodLog.description, // "description" o'rniga FoodLog::description.name ishlatish mumkin
            "calories" to updatedFoodLog.calories,
            "protein" to updatedFoodLog.protein,
            "fat" to updatedFoodLog.fat,
            "carbs" to updatedFoodLog.carbs,
            "mealType" to updatedFoodLog.mealType,
            "photoUrl" to updatedFoodLog.photoUrl,
            "analysisInsights" to updatedFoodLog.analysisInsights,
            "estimatedByAI" to updatedFoodLog.estimatedByAI
            // timestamp o'zgarmaydi
        )
        return try { logRef.updateChildren(updates).await(); DataWrapper.Success(Unit) }
        catch (e: Exception) {
            handleException(e, "UpdateFoodLogError") // Handle exception
            DataWrapper.Error(e.localizedMessage ?: "Ovqat logini yangilashda xatolik")
        }
    }


    suspend fun deleteFoodLog(logId: String): DataWrapper<Unit> {
        val userId = getCurrentUserId() ?: return DataWrapper.Error("Foydalanuvchi topilmadi")
        if (logId.isEmpty()) return DataWrapper.Error("Log ID bo'sh")
        val logRef = logsRootRef.child(userId).child(getTodayDateString()).child("foodLogs").child(logId)
        return try { logRef.removeValue().await(); DataWrapper.Success(Unit) }
        catch (e: Exception) {
            handleException(e, "DeleteFoodLogError") // Handle exception
            DataWrapper.Error(e.localizedMessage ?: "Ovqat logini o'chirishda xatolik")
        }
    }

    suspend fun deleteWaterLog(logId: String): DataWrapper<Unit> {
        val userId = getCurrentUserId() ?: return DataWrapper.Error("Foydalanuvchi topilmadi")
        if (logId.isEmpty()) return DataWrapper.Error("Log ID bo'sh")
        val logRef = logsRootRef.child(userId).child(getTodayDateString()).child("waterLogs").child(logId)
        return try { logRef.removeValue().await(); DataWrapper.Success(Unit) }
        catch (e: Exception) {
            handleException(e, "DeleteWaterLogError") // Handle exception
            DataWrapper.Error(e.localizedMessage ?: "Suv logini o'chirishda xatolik")
        }
    }

    suspend fun deleteExerciseLog(logId: String): DataWrapper<Unit> {
        val userId = getCurrentUserId() ?: return DataWrapper.Error("Foydalanuvchi topilmadi")
        if (logId.isEmpty()) return DataWrapper.Error("Log ID bo'sh")
        val logRef = logsRootRef.child(userId).child(getTodayDateString()).child("exerciseLogs").child(logId)
        return try { logRef.removeValue().await(); DataWrapper.Success(Unit) }
        catch (e: Exception) {
            handleException(e, "DeleteExerciseLogError") // Handle exception
            DataWrapper.Error(e.localizedMessage ?: "Mashq logini o'chirishda xatolik")
        }
    }
}