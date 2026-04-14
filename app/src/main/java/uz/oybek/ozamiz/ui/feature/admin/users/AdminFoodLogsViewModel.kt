package uz.oybek.ozamiz.ui.feature.admin.users

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import uz.oybek.ozamiz.data.firebase.FirebasePaths
import uz.oybek.ozamiz.data.logs.model.FoodLog
import uz.oybek.ozamiz.data.logs.model.WaterLog
import uz.oybek.ozamiz.data.logs.model.ExerciseLog

data class AdminGenericLog(
    val id: String,
    val title: String,
    val subtitle: String,
    val extraInfo: String,
    val photoUrl: String?,
    val timestamp: Long,
    val type: String
)

class AdminFoodLogsViewModel : ViewModel() {

    private val _foodLogs = MutableStateFlow<List<AdminGenericLog>>(emptyList())
    val foodLogs: StateFlow<List<AdminGenericLog>> = _foodLogs.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private var logsListener: com.google.firebase.database.ValueEventListener? = null
    private var currentPath: com.google.firebase.database.DatabaseReference? = null

    fun loadLatestFoodLogs(userId: String) {
        currentPath?.removeEventListener(logsListener ?: return)
        
        viewModelScope.launch {
            _isLoading.value = true
            val userLogsRef = FirebasePaths.userLogsReference(userId)
            currentPath = userLogsRef

            logsListener = object : com.google.firebase.database.ValueEventListener {
                override fun onDataChange(snapshot: com.google.firebase.database.DataSnapshot) {
                    val allLogs = mutableListOf<AdminGenericLog>()
                    if (snapshot.exists()) {
                        for (dateSnapshot in snapshot.children) {
                            // Parse Food Logs
                            dateSnapshot.child("foodLogs").children.forEach { foodSnap ->
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
                            // ... Water and Exercise parsing ...
                            dateSnapshot.child("waterLogs").children.forEach { waterSnap ->
                                val water = waterSnap.getValue(WaterLog::class.java)
                                if (water != null) {
                                    val ts = (water.timestamp as? Long) ?: ((water.timestamp as? Double)?.toLong()) ?: 0L
                                    allLogs.add(AdminGenericLog(water.logId.ifEmpty { waterSnap.key ?: "" }, "Suv", "${water.amountMl} ml", "", null, ts, "water"))
                                }
                            }
                            dateSnapshot.child("exerciseLogs").children.forEach { exSnap ->
                                val ex = exSnap.getValue(ExerciseLog::class.java)
                                if (ex != null) {
                                    val ts = (ex.timestamp as? Long) ?: ((ex.timestamp as? Double)?.toLong()) ?: 0L
                                    allLogs.add(AdminGenericLog(ex.logId.ifEmpty { exSnap.key ?: "" }, ex.exerciseType.ifEmpty { "Mashg'ulot" }, "${ex.caloriesBurned} kkal yoqildi", "${ex.durationMin} daqiqa", null, ts, "exercise"))
                                }
                            }
                        }
                        allLogs.sortByDescending { it.timestamp }
                        _foodLogs.value = allLogs
                    } else {
                        _foodLogs.value = emptyList()
                    }
                    _isLoading.value = false
                }

                override fun onCancelled(error: com.google.firebase.database.DatabaseError) {
                    _isLoading.value = false
                }
            }
            userLogsRef.orderByKey().limitToLast(14).addValueEventListener(logsListener!!)
        }
    }

    override fun onCleared() {
        super.onCleared()
        currentPath?.removeEventListener(logsListener ?: return)
    }
}
