package uz.oybek.ozamiz.data.logs

import com.google.firebase.database.FirebaseDatabase
import kotlinx.coroutines.tasks.await
import uz.oybek.ozamiz.data.firebase.FirebasePaths
import uz.oybek.ozamiz.data.logs.model.FoodLog
import uz.oybek.ozamiz.data.utils.DateUtils
import uz.oybek.ozamiz.data.utils.currentUserId
import java.util.Date

object FoodLogRepository {

    private val db = FirebaseDatabase.getInstance().reference

    suspend fun addFoodLog(
        log: FoodLog,
        date: Date = Date()
    ): Boolean {
        val userId = currentUserId ?: return false
        val dateKey = DateUtils.getFormattedDateString(date)
        val ref = FirebasePaths.foodLogsRef(userId, dateKey).push()
        log.logId = ref.key.orEmpty()

        return try {
            ref.setValue(log).await()
            true
        } catch (e: Exception) {
            false
        }
    }

    suspend fun updateFoodLog(
        logId: String,
        updatedLog: FoodLog,
        date: Date = Date()
    ): Boolean {
        val userId = currentUserId ?: return false
        val dateKey = DateUtils.getFormattedDateString(date)
        val ref = FirebasePaths.foodLogsRef(userId, dateKey).child(logId)

        return try {
            ref.updateChildren(updatedLog.toMap()).await()
            true
        } catch (e: Exception) {
            false
        }
    }

    suspend fun deleteFoodLog(
        logId: String,
        date: Date = Date()
    ): Boolean {
        val userId = currentUserId ?: return false
        val dateKey = DateUtils.getFormattedDateString(date)
        val ref = FirebasePaths.foodLogsRef(userId, dateKey).child(logId)

        return try {
            ref.removeValue().await()
            true
        } catch (e: Exception) {
            false
        }
    }
}