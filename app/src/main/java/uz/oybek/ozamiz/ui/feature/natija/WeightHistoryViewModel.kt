package uz.oybek.ozamiz.ui.feature.natija

import androidx.annotation.Keep
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import uz.oybek.ozamiz.common.result.BaseResult
import uz.oybek.ozamiz.data.profile.api.ProfileRepo
import uz.oybek.ozamiz.data.tracker.weight.api.WeightTrackerRepo
import uz.oybek.ozamiz.data.tracker.weight.model.WeightLog
import javax.inject.Inject

@Keep
data class WeightHistoryUiState(
    val isLoading: Boolean = false,
    val logs: List<WeightLog> = emptyList(),
    val error: String? = null,
    val needShowWeightLog: Boolean = false
)

@HiltViewModel
class WeightHistoryViewModel @Inject constructor(
    private val weightTrackerRepo: WeightTrackerRepo,
    private val profileRepo: ProfileRepo
) : ViewModel() {

    private val _uiState = MutableStateFlow(WeightHistoryUiState())
    val uiState = _uiState.asStateFlow()

    fun loadData() {
        val user = profileRepo.currentUser
        if (!user.valid) return

        _uiState.update { it.copy(isLoading = true, error = null) }

        viewModelScope.launch {
            weightTrackerRepo.getWeightLogsFlow(limit = 100).collect { result ->
                when (result) {
                    is BaseResult.Success -> {
                        val sortedLogs = result.data
                            .filter { it.weightKg > 0 }
                            .sortedByDescending { it.timestampLong }
                        _uiState.update {
                            it.copy(isLoading = false, logs = sortedLogs)
                        }
                    }
                    is BaseResult.Failure -> {
                        _uiState.update { it.copy(isLoading = false, error = result.error?.message ?: "Xatolik") }
                    }
                }
            }
        }

        viewModelScope.launch {
            weightTrackerRepo.getWeightLogs(limit = 1).let { result ->
                if (result is BaseResult.Success) {
                    val logs = result.data
                    val needShow = if (logs.isNotEmpty()) {
                        val lastLog = logs.first()
                        val oneWeekMs = 7L * 24 * 60 * 60 * 1000
                        System.currentTimeMillis() - lastLog.timestampLong > oneWeekMs
                    } else {
                        true
                    }
                    _uiState.update { it.copy(needShowWeightLog = needShow) }
                }
            }
        }
    }

    fun deleteLog(log: WeightLog) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            when (val result = weightTrackerRepo.deleteWeightLog(log)) {
                is BaseResult.Success -> {
                    _uiState.update { it.copy(isLoading = false) }
                }
                is BaseResult.Failure -> {
                    _uiState.update { it.copy(isLoading = false, error = result.error?.message ?: "Xatolik") }
                }
            }
        }
    }
}
