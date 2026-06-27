package uz.oybek.ozamiz.data.logs

import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import uz.oybek.ozamiz.data.firebase.FirebasePaths
import uz.oybek.ozamiz.data.firebase.currentUserId
import uz.oybek.ozamiz.data.firebase.getValueSafe
import uz.oybek.ozamiz.data.logs.model.DailyLogs
import uz.oybek.ozamiz.data.logs.model.DailySummary
import uz.oybek.ozamiz.data.utils.DataWrapper
import uz.oybek.ozamiz.data.utils.dateApiFormat
import java.util.Date

object DailyLogRepository {

    fun observeDailySummary(date: Date):
            Flow<DataWrapper<DailySummary>> =
        callbackFlow {
            val userId = currentUserId
            if (userId == null) {
                trySend(DataWrapper.Error())
                close()
                return@callbackFlow
            }

            val formattedDate = date.dateApiFormat()
            val reference = FirebasePaths
                .userLogsReference(userId)
                .child(formattedDate)

            val listener = object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val dailyLogs = snapshot
                        .getValueSafe(DailyLogs::class.java)
                    val summary = calculateSummary(dailyLogs)
                    trySend(DataWrapper.Success(summary))
                }

                override fun onCancelled(error: DatabaseError) {
                    trySend(DataWrapper.Error())
                }
            }
            reference.addValueEventListener(listener)
            awaitClose {
                reference.removeEventListener(listener)
            }
        }

    fun observeDailyLogs(date: Date): Flow<DataWrapper<DailyLogs>> =
        callbackFlow {
            val userId = currentUserId
            if (userId == null) {
                trySend(DataWrapper.Error())
                close()
                return@callbackFlow
            }

            val formattedDate = date.dateApiFormat()
            val reference = FirebasePaths
                .userLogsReference(userId)
                .child(formattedDate)

            val listener = object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val dailyLogs = snapshot.getValueSafe(DailyLogs::class.java) ?: DailyLogs()
                    trySend(DataWrapper.Success(dailyLogs))
                }

                override fun onCancelled(error: DatabaseError) {
                    trySend(DataWrapper.Error())
                }
            }
            reference.addValueEventListener(listener)
            awaitClose { reference.removeEventListener(listener) }
        }

    private fun calculateSummary(logs: DailyLogs?): DailySummary {
        if (logs == null) {
            return DailySummary()
        }
        val foodLogs = logs.foodLogs?.values ?: emptyList()
        val waterLogs = logs.waterLogs?.values ?: emptyList()
        val exerciseLogs = logs.exerciseLogs?.values ?: emptyList()

        val consumedCalories = foodLogs.filter { it.calories > 0 }.sumOf { it.calories }
        val consumedWaterMl = waterLogs.sumOf { it.amountMl }
        val burnedCalories = exerciseLogs.sumOf { it.caloriesBurned }
        val consumedProtein = foodLogs.sumOf { it.protein ?: 0.0 }
        val consumedCarbs = foodLogs.sumOf { it.carbs ?: 0.0 }
        val consumedFat = foodLogs.sumOf { it.fat ?: 0.0 }

        return DailySummary(
            consumedCalories = consumedCalories,
            consumedWaterMl = consumedWaterMl,
            burnedCalories = burnedCalories,
            consumedProtein = consumedProtein,
            consumedCarbs = consumedCarbs,
            consumedFat = consumedFat
        )
    }
}