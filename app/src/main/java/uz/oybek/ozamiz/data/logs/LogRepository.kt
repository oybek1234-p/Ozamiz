package uz.oybek.ozamiz.data.logs

import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.MutableData
import com.google.firebase.database.Transaction
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.tasks.await
import uz.oybek.ozamiz.data.firebase.FirebasePaths
import uz.oybek.ozamiz.data.firebase.currentUserId
import uz.oybek.ozamiz.data.firebase.getValueSafe
import uz.oybek.ozamiz.data.logs.model.AdminGenericLog
import uz.oybek.ozamiz.data.logs.model.DailyLogs
import uz.oybek.ozamiz.data.logs.model.DailySummary
import uz.oybek.ozamiz.common.result.BaseResult
import uz.oybek.ozamiz.common.util.dateApiFormat
import uz.oybek.ozamiz.common.util.orZero
import uz.oybek.ozamiz.common.util.round
import uz.oybek.ozamiz.util.recordException
import java.util.Date
import kotlin.coroutines.resume

object LogRepository {

    fun observeDailySummary(date: Date): Flow<BaseResult<DailySummary, Unit>> =
        callbackFlow {
            val userId = currentUserId
            if (userId.isBlank()) {
                trySend(BaseResult.Failure(Unit))
                close()
                return@callbackFlow
            }

            val formattedDate = date.dateApiFormat()
            val reference = FirebasePaths.Database.dailyLogs(userId, formattedDate)

            val listener = object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val dailyLogs = snapshot.getValueSafe(DailyLogs::class.java)
                    val summary = calculateSummary(dailyLogs)
                    trySend(BaseResult.Success(summary))
                }

                override fun onCancelled(error: DatabaseError) {
                    trySend(BaseResult.Failure(Unit))
                }
            }
            reference.addValueEventListener(listener)
            awaitClose {
                reference.removeEventListener(listener)
            }
        }

    fun observeDailyLogs(date: Date): Flow<BaseResult<DailyLogs, Unit>> =
        callbackFlow {
            val userId = currentUserId
            if (userId.isBlank()) {
                trySend(BaseResult.Failure(Unit))
                close()
                return@callbackFlow
            }

            val formattedDate = date.dateApiFormat()
            val reference = FirebasePaths.Database.dailyLogs(userId, formattedDate)

            val listener = object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val dailyLogs = snapshot.getValueSafe(DailyLogs::class.java) ?: DailyLogs()
                    trySend(BaseResult.Success(dailyLogs))
                }

                override fun onCancelled(error: DatabaseError) {
                    trySend(BaseResult.Failure(Unit))
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
        val reference = FirebasePaths.Database.logs(userId).limitToLast(10)
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
                                timestamp = (log.timestamp as? Long).orZero(),
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

    fun calculateSummary(logs: DailyLogs?): DailySummary {
        if (logs == null) {
            return DailySummary()
        }
        val foodLogs = logs.foodLogs?.values ?: emptyList()
        val waterLogs = logs.waterLogs?.values ?: emptyList()
        val exerciseLogs = logs.exerciseLogs?.values ?: emptyList()

        val consumedCalories = foodLogs.filter { it.calories > 0 }.sumOf { it.calories }
        val consumedWaterMl = waterLogs.sumOf { it.amountMl }
        val burnedCalories = exerciseLogs.sumOf { it.caloriesBurned }
        val consumedProtein = foodLogs.sumOf { it.protein.orZero() }.round()
        val consumedCarbs = foodLogs.sumOf { it.carbs.orZero() }.round()
        val consumedFat = foodLogs.sumOf { it.fat.orZero() }.round()

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
            val snapshot = FirebasePaths.Database.dailyLogs(userId, formattedDate).get().await()
            snapshot.getValueSafe(DailyLogs::class.java)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private val localSummaryCache = java.util.concurrent.ConcurrentHashMap<String, DailySummary>()

    fun clearCache() {
        localSummaryCache.clear()
    }

    fun cacheSummary(dateStr: String, summary: DailySummary) {
        localSummaryCache[dateStr] = summary
    }

    suspend fun get7DaySummariesFast(dates: List<Date>): Map<String, DailySummary> {
        val userId = currentUserId
        if (userId.isBlank()) return emptyMap()

        val resultMap = mutableMapOf<String, DailySummary>()
        val missingDates = mutableListOf<String>()

        for (d in dates) {
            val key = d.dateApiFormat()
            val cached = localSummaryCache[key]
            if (cached != null) {
                resultMap[key] = cached
            } else {
                missingDates.add(key)
            }
        }

        if (missingDates.isEmpty()) {
            return resultMap
        }

        try {
            val minDate = missingDates.minOrNull() ?: dates.last().dateApiFormat()
            val maxDate = missingDates.maxOrNull() ?: dates.first().dateApiFormat()

            val snapshot = FirebasePaths.Database.summaries(userId)
                .orderByKey()
                .startAt(minDate)
                .endAt(maxDate)
                .get()
                .await()

            for (child in snapshot.children) {
                val key = child.key ?: continue
                val summary = child.getValue(DailySummary::class.java)
                if (summary != null) {
                    localSummaryCache[key] = summary
                    resultMap[key] = summary
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return resultMap
    }

    suspend fun getDailySummarySync(date: Date, forceRefresh: Boolean = false): DailySummary {
        val userId = currentUserId
        if (userId.isBlank()) return DailySummary()
        val formattedDate = date.dateApiFormat()
        if (!forceRefresh) {
            val cached = localSummaryCache[formattedDate]
            if (cached != null) return cached
        }

        return try {
            val summarySnapshot = FirebasePaths.Database.summary(userId, formattedDate).get().await()
            val summaryFromDb = summarySnapshot.getValue(DailySummary::class.java)
            if (summaryFromDb != null) {
                localSummaryCache[formattedDate] = summaryFromDb
                return summaryFromDb
            }
            val snapshot = FirebasePaths.Database.dailyLogs(userId, formattedDate).get().await()
            val dailyLogs = snapshot.getValueSafe(DailyLogs::class.java)
            val summary = calculateSummary(dailyLogs)
            localSummaryCache[formattedDate] = summary
            summary
        } catch (e: Exception) {
            e.printStackTrace()
            DailySummary()
        }
    }

    fun observeSummaries(startDate: Date, endDate: Date): Flow<Map<String, DailySummary>> =
        callbackFlow {
            val userId = currentUserId
            if (userId.isBlank()) {
                trySend(emptyMap())
                close()
                return@callbackFlow
            }

            val startStr = startDate.dateApiFormat()
            val endStr = endDate.dateApiFormat()

            val query = FirebasePaths.Database.summaries(userId)
                .orderByKey()
                .startAt(startStr)
                .endAt(endStr)

            val listener = object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val map = mutableMapOf<String, DailySummary>()
                    for (child in snapshot.children) {
                        val summary = child.getValue(DailySummary::class.java)
                        if (summary != null) {
                            val key = child.key ?: ""
                            map[key] = summary
                            if (key.isNotBlank()) {
                                localSummaryCache[key] = summary
                            }
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

    suspend fun updateSummary(
        date: Date,
        targetUid: String? = null,
        caloriesDelta: Int = 0,
        waterDelta: Int = 0,
        exerciseDelta: Int = 0,
        proteinDelta: Double = 0.0,
        carbsDelta: Double = 0.0,
        fatDelta: Double = 0.0,
        mealsDelta: Int = 0
    ) = suspendCancellableCoroutine {
        val userId = targetUid ?: currentUserId
        if (userId.isBlank()) {
            it.resume(Unit)
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
                if (committed && currentData != null) {
                    val updated = currentData.getValue(DailySummary::class.java)
                    if (updated != null) {
                        localSummaryCache[dateStr] = updated
                    }
                }
                it.resume(Unit)
                if (error != null) {
                    recordException(error.toException())
                }
            }
        })
    }
}
