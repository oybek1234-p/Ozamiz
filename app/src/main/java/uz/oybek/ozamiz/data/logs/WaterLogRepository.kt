package uz.oybek.ozamiz.data.logs

import com.google.firebase.database.FirebaseDatabase
import kotlinx.coroutines.tasks.await
import uz.oybek.ozamiz.data.firebase.FirebasePaths
import uz.oybek.ozamiz.data.firebase.currentUserId
import uz.oybek.ozamiz.data.logs.model.WaterLog
import uz.oybek.ozamiz.data.utils.DateUtils
import java.util.Date

object WaterLogRepository {

    private val db = FirebaseDatabase.getInstance().reference

    suspend fun addWaterLog(
        log: WaterLog?,
        date: Date = Date()
    ): Boolean {
        val userId = currentUserId ?: return false
        val dateKey = DateUtils.getFormattedDateString(date)
        val ref = FirebasePaths.waterLogsRef(userId, dateKey).push()
        log?.logId = ref.key.orEmpty()

        return try {
            ref.setValue(log).await()
            true
        } catch (_: Exception) {
            false
        }
    }

    suspend fun deleteWaterLog(
        logId: String?,
        date: Date = Date()
    ): Boolean {
        val userId = currentUserId ?: return false
        val dateKey = DateUtils.getFormattedDateString(date)
        val ref = FirebasePaths.waterLogsRef(userId, dateKey).child(logId.orEmpty())

        return try {
            ref.removeValue().await()
            true
        } catch (_: Exception) {
            false
        }
    }
}