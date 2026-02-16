package uz.oybek.ozamiz.ui.feature.natija

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import uz.oybek.ozamiz.data.logs.WeightLogRepository
import uz.oybek.ozamiz.data.logs.model.WeightLog
import uz.oybek.ozamiz.data.user.local.LocalUser
import uz.oybek.ozamiz.data.utils.DataWrapper

data class WeightHistoryUiState(
    val isLoading: Boolean = false,
    val logs: List<WeightLog> = emptyList(),
    val error: String? = null
)

class WeightHistoryViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(WeightHistoryUiState())
    val uiState = _uiState.asStateFlow()

    fun loadData() {
        val user = LocalUser.user
        if (!user.valid) return

        _uiState.update { it.copy(isLoading = true, error = null) }

        viewModelScope.launch {
            WeightLogRepository.getLogsFlow(user.uid).collect { result ->
                when (result) {
                    is DataWrapper.Success -> {
                        val logs = result.data
                            .filter { it.weightKg > 0 }
                            .sortedByDescending { it.timestampLong } // Newest first
                        
                        _uiState.update { 
                            it.copy(isLoading = false, logs = logs) 
                        }
                    }
                    is DataWrapper.Error -> {
                        _uiState.update { it.copy(isLoading = false, error = result.error) }
                    }
                    else -> {}
                }
            }
        }
    }
    fun deleteLog(log: WeightLog) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            when (val result = WeightLogRepository.deleteLog(log)) {
                is DataWrapper.Success -> {
                    // Refresh data is automatic via Flow, but we can update state if needed
                    // For now, let loading be handled by flow collection
                     _uiState.update { it.copy(isLoading = false) }
                }
                is DataWrapper.Error -> {
                    _uiState.update { it.copy(isLoading = false, error = result.error) }
                }

                else -> {}
            }
        }
    }
}
