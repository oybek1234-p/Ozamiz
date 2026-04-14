package uz.oybek.ozamiz.data.logs

import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.MutableData
import com.google.firebase.database.Transaction
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import uz.oybek.ozamiz.data.firebase.FirebasePaths
import uz.oybek.ozamiz.data.firebase.currentUserId
import uz.oybek.ozamiz.data.logs.model.DailySummary
import uz.oybek.ozamiz.data.utils.dateApiFormat
import java.util.Date

object DailySummaryRepository {

    fun observeSummaries(startDate: Date, endDate: Date): Flow<Map<String, DailySummary>> = callbackFlow {
        val userId = currentUserId
        if (userId == null) {
            trySend(emptyMap())
            close()
            return@callbackFlow
        }

        val startStr = startDate.dateApiFormat()
        val endStr = endDate.dateApiFormat()

        val query = FirebasePaths.userDailySummariesReference(userId)
            .orderByKey()
            .startAt(startStr)
            .endAt(endStr)

        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val map = mutableMapOf<String, DailySummary>()
                for (child in snapshot.children) {
                    val summary = child.getValue(DailySummary::class.java)
                    if (summary != null) {
                        map[child.key ?: ""] = summary
                    }
                }
                trySend(map)
            }

            override fun onCancelled(error: DatabaseError) {
                trySend(emptyMap())
            }
        }
        query.addValueEventListener(listener)
        awaitClose { query.removeEventListener(listener) }
    }

    /**
     * Updates the summary transactionally.
     * @param date Date of the log
     * @param caloriesDelta Change in calories (positive to add, negative to remove)
     * @param waterDelta Change in water (ml)
     * @param exerciseDelta Change in burned calories
     * @param proteinDelta Change in protein
     * @param carbsDelta Change in carbs
     * @param fatDelta Change in fat
     */
    fun updateSummary(
        date: Date,
        targetUid: String? = null,
        caloriesDelta: Int = 0,
        waterDelta: Int = 0,
        exerciseDelta: Int = 0,
        proteinDelta: Double = 0.0,
        carbsDelta: Double = 0.0,
        fatDelta: Double = 0.0
    ) {
        val userId = targetUid ?: currentUserId ?: return
        val dateStr = date.dateApiFormat()
        val ref = FirebasePaths.dailySummaryReference(userId, dateStr)

        ref.runTransaction(object : Transaction.Handler {
            override fun doTransaction(currentData: MutableData): Transaction.Result {
                val current = currentData.getValue(DailySummary::class.java) ?: DailySummary()
                
                val newSummary = current.copy(
                    consumedCalories = (current.consumedCalories + caloriesDelta).coerceAtLeast(0),
                    consumedWaterMl = (current.consumedWaterMl + waterDelta).coerceAtLeast(0),
                    burnedCalories = (current.burnedCalories + exerciseDelta).coerceAtLeast(0),
                    consumedProtein = (current.consumedProtein + proteinDelta).coerceAtLeast(0.0),
                    consumedCarbs = (current.consumedCarbs + carbsDelta).coerceAtLeast(0.0),
                    consumedFat = (current.consumedFat + fatDelta).coerceAtLeast(0.0),
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
                // Log error if needed
            }
        })
    }
}
