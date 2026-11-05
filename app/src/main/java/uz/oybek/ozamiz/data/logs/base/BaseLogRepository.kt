package uz.oybek.ozamiz.data.logs.base

import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.MutableData
import com.google.firebase.database.Transaction
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.tasks.await
import uz.oybek.ozamiz.common.util.dateApiFormat
import uz.oybek.ozamiz.common.util.round
import uz.oybek.ozamiz.data.firebase.FirebasePaths
import uz.oybek.ozamiz.data.firebase.currentUserId
import uz.oybek.ozamiz.data.firebase.generateKey
import uz.oybek.ozamiz.data.logs.model.DailySummary
import uz.oybek.ozamiz.util.recordException
import java.util.Date
import kotlin.coroutines.resume

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
        val userId = currentUserId
        if (userId.isBlank()) return false
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
        val userId = currentUserId
        if (userId.isBlank() || logId.isEmpty()) return false
        val dateStr = date.dateApiFormat()
        return try {
            getReference(userId, dateStr).child(logId).removeValue().await()
            true
        } catch (_: Exception) {
            false
        }
    }

    protected suspend fun updateSummary(
        date: Date,
        targetUid: String? = null,
        caloriesDelta: Int = 0,
        waterDelta: Int = 0,
        exerciseDelta: Int = 0,
        proteinDelta: Double = 0.0,
        carbsDelta: Double = 0.0,
        fatDelta: Double = 0.0,
        mealsDelta: Int = 0
    ) = suspendCancellableCoroutine { cont ->
        val userId = targetUid ?: currentUserId
        if (userId.isBlank()) {
            cont.resume(Unit)
            return@suspendCancellableCoroutine
        }
        val dateStr = date.dateApiFormat()
        val ref = FirebasePaths.Database.summary(userId, dateStr)

        ref.runTransaction(object : Transaction.Handler {
            override fun doTransaction(currentData: MutableData): Transaction.Result {
                val current = currentData.getValue(DailySummary::class.java) ?: DailySummary()
                val newSummary = current.copy(
                    consumedCalories = (current.consumedCalories + caloriesDelta).coerceAtLeast(0),
                    consumedWaterMl = (current.consumedWaterMl + waterDelta).coerceAtLeast(0),
                    burnedCalories = (current.burnedCalories + exerciseDelta).coerceAtLeast(0),
                    consumedProtein = (current.consumedProtein + proteinDelta).coerceAtLeast(0.0).round(),
                    consumedCarbs = (current.consumedCarbs + carbsDelta).coerceAtLeast(0.0).round(),
                    consumedFat = (current.consumedFat + fatDelta).coerceAtLeast(0.0).round(),
                    mealsCount = (current.mealsCount + mealsDelta).coerceAtLeast(0),
                    timestamp = System.currentTimeMillis()
                )
                currentData.value = newSummary
                return Transaction.success(currentData)
            }

            override fun onComplete(
                error: DatabaseError?,
                committed: Boolean,
                currentData: DataSnapshot?
            ) {
                cont.resume(Unit)
                if (error != null) {
                    recordException(error.toException())
                }
            }
        })
    }
}
