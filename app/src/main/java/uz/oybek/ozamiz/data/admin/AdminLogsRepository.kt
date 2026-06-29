package uz.oybek.ozamiz.data.admin

import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import uz.oybek.ozamiz.data.firebase.FirebasePaths
import uz.oybek.ozamiz.data.logs.model.ExerciseLog
import uz.oybek.ozamiz.data.logs.model.FoodLog
import uz.oybek.ozamiz.data.logs.model.WaterLog
import uz.oybek.ozamiz.ui.feature.admin.users.AdminGenericLog

object AdminLogsRepository {

    fun streamLatestLogsForAdmin(userId: String): Flow<List<AdminGenericLog>> = callbackFlow {
        val userLogsRef = FirebasePaths.userLogsReference(userId)
        
        val logsListener = object : com.google.firebase.database.ValueEventListener {
            override fun onDataChange(snapshot: com.google.firebase.database.DataSnapshot) {
                val allLogs = mutableListOf<AdminGenericLog>()
                if (snapshot.exists()) {
                    for (dateSnapshot in snapshot.children) {
                        // Parse Food Logs
                        dateSnapshot.child(FirebasePaths.Database.FOOD_LOGS).children.forEach { foodSnap ->
                            val food = foodSnap.getValue(FoodLog::class.java)
                            if (food != null) {
                                val ts = (food.timestamp as? Long) ?: ((food.timestamp as? Double)?.toLong()) ?: 0L
                                allLogs.add(
                                    AdminGenericLog(
                                        id = food.logId.ifEmpty { foodSnap.key ?: "" },
                                        title = food.description,
                                        subtitle = "${food.calories} kkal",
                                        extraInfo = "Oqsil: ${food.protein ?: 0}g • Yog': ${food.fat ?: 0}g • Uglevod: ${food.carbs ?: 0}g",
                                        photoUrl = food.photoUrl,
                                        timestamp = ts,
                                        type = "food"
                                    )
                                )
                            }
                        }
                        // Parse Water Logs
                        dateSnapshot.child(FirebasePaths.Database.WATER_LOGS).children.forEach { waterSnap ->
                            val water = waterSnap.getValue(WaterLog::class.java)
                            if (water != null) {
                                val ts = (water.timestamp as? Long) ?: ((water.timestamp as? Double)?.toLong()) ?: 0L
                                allLogs.add(AdminGenericLog(water.logId.ifEmpty { waterSnap.key ?: "" }, "Suv", "${water.amountMl} ml", "", null, ts, "water"))
                            }
                        }
                        // Parse Exercise Logs
                        dateSnapshot.child(FirebasePaths.Database.EXERCISE_LOGS).children.forEach { exSnap ->
                            val ex = exSnap.getValue(ExerciseLog::class.java)
                            if (ex != null) {
                                val ts = (ex.timestamp as? Long) ?: ((ex.timestamp as? Double)?.toLong()) ?: 0L
                                allLogs.add(AdminGenericLog(ex.logId.ifEmpty { exSnap.key ?: "" }, ex.exerciseType.ifEmpty { "Mashg'ulot" }, "${ex.caloriesBurned} kkal yoqildi", "${ex.durationMin} daqiqa", null, ts, "exercise"))
                            }
                        }
                    }
                    allLogs.sortByDescending { it.timestamp }
                    trySend(allLogs)
                } else {
                    trySend(emptyList())
                }
            }

            override fun onCancelled(error: com.google.firebase.database.DatabaseError) {
                trySend(emptyList())
            }
        }
        val query = userLogsRef.orderByKey().limitToLast(14)
        query.addValueEventListener(logsListener)

        awaitClose { query.removeEventListener(logsListener) }
    }
}
