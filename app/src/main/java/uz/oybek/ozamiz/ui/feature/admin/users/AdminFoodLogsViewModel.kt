package uz.oybek.ozamiz.ui.feature.admin.users

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import uz.oybek.ozamiz.data.admin.AdminLogsRepository
import kotlinx.coroutines.Job

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

    private var job: Job? = null

    fun loadLatestFoodLogs(userId: String) {
        job?.cancel()
        
        job = viewModelScope.launch {
            _isLoading.value = true
            AdminLogsRepository.streamLatestLogsForAdmin(userId).collect { allLogs ->
                _foodLogs.value = allLogs
                _isLoading.value = false
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        job?.cancel()
    }
}
