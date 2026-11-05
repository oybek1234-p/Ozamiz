package uz.oybek.ozamiz.ui.feature.log.photo

import androidx.annotation.Keep
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import uz.oybek.ozamiz.common.result.BaseResult
import uz.oybek.ozamiz.data.tracker.weight.api.WeightTrackerRepo
import uz.oybek.ozamiz.data.tracker.weight.model.WeightLog
import javax.inject.Inject

@Keep
data class WeightPhotoHistoryUiState(
    val isLoading: Boolean = true,
    val logs: List<WeightLog> = emptyList(),
    val error: String? = null
)

@HiltViewModel
class WeightPhotoHistoryViewModel @Inject constructor(
    private val weightTrackerRepo: WeightTrackerRepo
) : ViewModel() {

    private val _uiState = MutableStateFlow(WeightPhotoHistoryUiState())
    val uiState: StateFlow<WeightPhotoHistoryUiState> = _uiState.asStateFlow()

    fun loadHistory(uid: String? = null) {
        _uiState.update { it.copy(isLoading = true, error = null) }
        viewModelScope.launch {
            weightTrackerRepo.getWeightLogsFlow(limit = 100)
                .catch { e ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = "Tarixni yuklashda xatolik: ${e.localizedMessage}"
                        )
                    }
                }
                .collectLatest { result ->
                    when (result) {
                        is BaseResult.Success -> {
                            _uiState.update {
                                it.copy(isLoading = false, logs = result.data, error = null)
                            }
                        }

                        is BaseResult.Failure -> {
                            _uiState.update {
                                it.copy(isLoading = false, error = result.error?.message ?: "Xatolik", logs = emptyList())
                            }
                        }
                    }
                }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
}
