package uz.oybek.ozamiz.data.logs

import com.google.firebase.database.DatabaseReference
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.tasks.await
import uz.oybek.ozamiz.data.firebase.FirebasePaths
import uz.oybek.ozamiz.data.firebase.currentUserId
import uz.oybek.ozamiz.data.logs.base.BaseLogRepository
import uz.oybek.ozamiz.data.logs.model.WaterLog
import uz.oybek.ozamiz.data.utils.dateApiFormat
import java.util.Date

object WaterLogRepository : BaseLogRepository<WaterLog>() {

    override fun getReference(userId: String, date: String): DatabaseReference {
        return FirebasePaths.Database.waterLogs(userId, date)
    }

    override suspend fun add(log: WaterLog, date: Date): Boolean {
        if (log.timestampLong <= 0L) {
            log.timestampLong = System.currentTimeMillis()
        }
        if (log.timestamp == null) {
            log.timestamp = System.currentTimeMillis()
        }
        val success = super.add(log, date)
        if (success) {
            LogRepository.updateSummary(
                date = date,
                waterDelta = log.amountMl
            )
        }
        return success
    }

    suspend fun delete(logId: String, date: Date, targetUid: String? = null): Boolean {
        // Need to fetch old log to know points
        val userId = targetUid ?: currentUserId
        if (userId.isBlank()) return false
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
            LogRepository.updateSummary(
                date = date,
                targetUid = userId,
                waterDelta = -oldLog.amountMl
            )
        }
        return success
    }

    fun getLogs(date: Date): kotlinx.coroutines.flow.Flow<List<WaterLog>> = kotlinx.coroutines.flow.callbackFlow {
        val userId = currentUserId
        if (userId.isBlank()) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }
        val dateStr = date.dateApiFormat()
        val query = getReference(userId, dateStr)

        val listener = object : com.google.firebase.database.ValueEventListener {
            override fun onDataChange(snapshot: com.google.firebase.database.DataSnapshot) {
                val logs = snapshot.children.mapNotNull { it.getValue(WaterLog::class.java) }
                trySend(logs)
            }

            override fun onCancelled(error: com.google.firebase.database.DatabaseError) {
                trySend(emptyList())
            }
        }
        query.addValueEventListener(listener)
        awaitClose { query.removeEventListener(listener) }
    }
}