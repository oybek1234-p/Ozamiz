package uz.oybek.ozamiz.ui.feature.admin.users

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.Job
import uz.oybek.ozamiz.data.logs.LogRepository
import uz.oybek.ozamiz.data.logs.model.AdminGenericLog

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
            LogRepository.streamLatestLogsForAdmin(userId).collect { allLogs ->
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
