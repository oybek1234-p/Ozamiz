package uz.oybek.ozamiz.data.logs

import com.google.firebase.database.DatabaseReference
import kotlinx.coroutines.tasks.await
import uz.oybek.ozamiz.data.firebase.FirebasePaths
import uz.oybek.ozamiz.data.firebase.currentUserId
import uz.oybek.ozamiz.data.logs.base.BaseLogRepository
import uz.oybek.ozamiz.data.logs.model.WaterLog
import uz.oybek.ozamiz.data.utils.dateApiFormat
import java.util.Date

/**
 * Repository for water log operations.
 * Extends BaseLogRepository to eliminate code duplication.
 */
object WaterLogRepository : BaseLogRepository<WaterLog>() {

    override fun getReference(userId: String, date: String): DatabaseReference {
        return FirebasePaths.waterLogsReference(userId, date)
    }

    override suspend fun add(log: WaterLog, date: Date): Boolean {
        val success = super.add(log, date)
        if (success) {
            DailySummaryRepository.updateSummary(
                date = date,
                waterDelta = log.amountMl
            )
        }
        return success
    }

    suspend fun delete(logId: String, date: Date, targetUid: String? = null): Boolean {
        // Need to fetch old log to know amount
        val userId = targetUid ?: currentUserId ?: return false
        val dateStr = date.dateApiFormat()
        val oldLog = try {
            getReference(userId, dateStr).child(logId).get().await().getValue(WaterLog::class.java)
        } catch (e: Exception) {
            null
        }

        val reference = getReference(userId, dateStr)
        val success = try {
            reference.child(logId).removeValue().await()
            true
        } catch (e: Exception) {
            false
        }

        if (success && oldLog != null) {
            DailySummaryRepository.updateSummary(
                date = date,
                targetUid = userId,
                waterDelta = -oldLog.amountMl
            )
        }
        return success
    }
}