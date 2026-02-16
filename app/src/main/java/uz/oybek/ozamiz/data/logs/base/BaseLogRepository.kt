package uz.oybek.ozamiz.data.logs.base

import com.google.firebase.database.DatabaseReference
import kotlinx.coroutines.tasks.await
import uz.oybek.ozamiz.data.firebase.currentUserId
import uz.oybek.ozamiz.data.firebase.generateKey
import uz.oybek.ozamiz.data.utils.dateApiFormat
import java.util.Date

/**
 * Base repository for log operations with Firebase Realtime Database.
 * Eliminates code duplication across FoodLogRepository, WaterLogRepository, ExerciseLogRepository.
 */
abstract class BaseLogRepository<T : LogItem> {

    /**
     * Get the Firebase DatabaseReference for the specific log type
     */
    abstract fun getReference(userId: String, date: String): DatabaseReference

    /**
     * Assigns a logId to the log item (template method for setting ID)
     */
    protected open fun assignLogId(log: T): String {
        val id = generateKey()
        log.logId = id
        return id
    }

    /**
     * Add a new log entry
     */
    open suspend fun add(log: T, date: Date): Boolean {
        val userId = currentUserId ?: return false
        val dateStr = date.dateApiFormat()
        val reference = getReference(userId, dateStr)
        val logId = assignLogId(log)
        return try {
            reference.child(logId).setValue(log).await()
            true
        } catch (_: Exception) {
            false
        }
    }

    /**
     * Delete an existing log entry
     */
    open suspend fun delete(logId: String, date: Date): Boolean {
        val userId = currentUserId ?: return false
        if (logId.isEmpty()) return false
        val dateStr = date.dateApiFormat()
        return try {
            getReference(userId, dateStr).child(logId).removeValue().await()
            true
        } catch (_: Exception) {
            false
        }
    }
}
