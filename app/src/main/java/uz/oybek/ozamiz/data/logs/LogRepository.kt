package uz.oybek.ozamiz.data.logs

import com.google.firebase.crashlytics.crashlytics
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.MutableData
import com.google.firebase.database.Transaction
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import uz.oybek.ozamiz.data.firebase.FirebasePaths
import uz.oybek.ozamiz.data.firebase.currentUserId
import uz.oybek.ozamiz.data.firebase.getValueSafe
import uz.oybek.ozamiz.data.logs.model.AdminGenericLog
import uz.oybek.ozamiz.data.logs.model.DailyLogs
import uz.oybek.ozamiz.data.logs.model.DailySummary
import uz.oybek.ozamiz.data.utils.DataWrapper
import uz.oybek.ozamiz.data.utils.dateApiFormat
import java.util.Date

object LogRepository {

    fun observeDailySummary(date: Date): Flow<DataWrapper<DailySummary>> =
        callbackFlow {
            val userId = currentUserId
            if (userId.isBlank()) {
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
                    val dailyLogs = snapshot.getValueSafe(DailyLogs::class.java)
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
            if (userId.isBlank()) {
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

    fun streamLatestLogsForAdmin(userId: String): Flow<List<AdminGenericLog>> = callbackFlow {
        if (userId.isBlank()) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }
        val reference = FirebasePaths.userLogsReference(userId).limitToLast(10)
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val allLogs = mutableListOf<AdminGenericLog>()
                for (dateSnapshot in snapshot.children) {
                    val dailyLogs = dateSnapshot.getValueSafe(DailyLogs::class.java) ?: continue

                    dailyLogs.foodLogs?.forEach { (id, log) ->
                        allLogs.add(
                            AdminGenericLog(
                                id = id,
                                title = log.description,
                                subtitle = "${log.calories} kcal",
                                extraInfo = listOfNotNull(
                                    log.protein?.let { "P: ${it}g" },
                                    log.carbs?.let { "C: ${it}g" },
                                    log.fat?.let { "F: ${it}g" }
                                ).joinToString(", "),
                                photoUrl = log.photoUrl,
                                timestamp = (log.timestamp as? Long) ?: 0L,
                                type = "food"
                            )
                        )
                    }

                    dailyLogs.waterLogs?.forEach { (id, log) ->
                        allLogs.add(
                            AdminGenericLog(
                                id = id,
                                title = "Suv",
                                subtitle = "${log.amountMl} ml",
                                extraInfo = "",
                                photoUrl = null,
                                timestamp = log.timestampLong,
                                type = "water"
                            )
                        )
                    }

                    dailyLogs.exerciseLogs?.forEach { (id, log) ->
                        allLogs.add(
                            AdminGenericLog(
                                id = id,
                                title = log.exerciseType,
                                subtitle = "${log.durationMin} min • ${log.caloriesBurned} kcal",
                                extraInfo = "",
                                photoUrl = null,
                                timestamp = log.timestampLong,
                                type = "exercise"
                            )
                        )
                    }
                }
                trySend(allLogs.sortedByDescending { it.timestamp })
            }

            override fun onCancelled(error: DatabaseError) {
                trySend(emptyList())
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

    suspend fun getDailyLogsSync(date: Date): DailyLogs? {
        val userId = currentUserId
        if (userId.isBlank()) return null
        return try {
            val formattedDate = date.dateApiFormat()
            val snapshot = FirebasePaths.userLogsReference(userId).child(formattedDate).get().await()
            snapshot.getValueSafe(DailyLogs::class.java)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    suspend fun getDailySummarySync(date: Date): DailySummary {
        val userId = currentUserId
        if (userId.isBlank()) return DailySummary()
        return try {
            val formattedDate = date.dateApiFormat()
            val snapshot = FirebasePaths.userLogsReference(userId).child(formattedDate).get().await()
            val dailyLogs = snapshot.getValueSafe(DailyLogs::class.java)
            calculateSummary(dailyLogs)
        } catch (e: Exception) {
            e.printStackTrace()
            DailySummary()
        }
    }

    fun observeSummaries(startDate: Date, endDate: Date): Flow<Map<String, DailySummary>> = callbackFlow {
        val userId = currentUserId
        if (userId.isBlank()) {
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

    fun updateSummary(
        date: Date,
        targetUid: String? = null,
        caloriesDelta: Int = 0,
        waterDelta: Int = 0,
        exerciseDelta: Int = 0,
        proteinDelta: Double = 0.0,
        carbsDelta: Double = 0.0,
        fatDelta: Double = 0.0,
        mealsDelta: Int = 0
    ) {
        val userId = targetUid ?: currentUserId
        if (userId.isBlank()) return
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
                if (error != null) {
                    com.google.firebase.Firebase.crashlytics.recordException(error.toException())
                }
            }
        })
    }
}
