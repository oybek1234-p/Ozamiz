package uz.oybek.ozamiz.ui.feature.admin.users

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch
import uz.oybek.ozamiz.common.util.orZero
import uz.oybek.ozamiz.data.firebase.FirebasePaths
import uz.oybek.ozamiz.data.firebase.getValueSafe
import uz.oybek.ozamiz.data.logs.model.AdminGenericLog
import uz.oybek.ozamiz.data.logs.model.DailyLogs

class AdminFoodLogsViewModel : ViewModel() {

    private val _foodLogs = MutableStateFlow<List<AdminGenericLog>>(emptyList())
    val foodLogs: StateFlow<List<AdminGenericLog>> = _foodLogs.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private var job: Job? = null

    fun loadLatestFoodLogs(userId: String) {
        job?.cancel()

        job = viewModelScope.launch {
            _isLoading.value = true
            streamLatestLogsForAdmin(userId).collect { allLogs ->
                _foodLogs.value = allLogs
                _isLoading.value = false
            }
        }
    }

    private fun streamLatestLogsForAdmin(userId: String): Flow<List<AdminGenericLog>> = callbackFlow {
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
                                title = "${log.amountMl} ml",
                                subtitle = "",
                                extraInfo = "",
                                photoUrl = null,
                                timestamp = (log.timestamp as? Long).orZero(),
                                type = "water"
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

    override fun onCleared() {
        super.onCleared()
        job?.cancel()
    }
}
