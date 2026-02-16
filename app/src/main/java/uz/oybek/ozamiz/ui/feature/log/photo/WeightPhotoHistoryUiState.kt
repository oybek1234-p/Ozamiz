package uz.oybek.ozamiz.ui.feature.log.photo

import androidx.lifecycle.ViewModel
import uz.oybek.ozamiz.data.logs.model.WeightLog
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import uz.oybek.ozamiz.data.logs.WeightLogRepository
import uz.oybek.ozamiz.data.user.local.LocalUser
import uz.oybek.ozamiz.data.utils.DataWrapper
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class WeightPhotoHistoryUiState(
    val isLoading: Boolean = true,
    val logs: List<WeightLog> = emptyList(),
    val error: String? = null
)

class WeightPhotoHistoryViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(WeightPhotoHistoryUiState())
    val uiState: StateFlow<WeightPhotoHistoryUiState> = _uiState.asStateFlow()

    fun loadHistory() {
        val userId = LocalUser.user.uid
        if (userId.isBlank()) {
            _uiState.update { it.copy(isLoading = false, error = "Foydalanuvchi topilmadi.") }
            return
        }
//
        _uiState.update { it.copy(isLoading = true, error = null) }
        viewModelScope.launch {
            WeightLogRepository.getLogsFlow(userId)
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
                        is DataWrapper.Success -> {
                            _uiState.update {
                                it.copy(isLoading = false, logs = result.data, error = null)
                            }
                        }

                        is DataWrapper.Error -> {
                            _uiState.update {
                                it.copy(isLoading = false, error = result.error, logs = emptyList())
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