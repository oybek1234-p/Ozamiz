package uz.oybek.ozamiz.ui.log_photo

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import uz.oybek.ozamiz.data.DataWrapper
import uz.oybek.ozamiz.data.LocalUser
import uz.oybek.ozamiz.data.logs.WeightPhotoLogRepository
import uz.oybek.ozamiz.data.model.WeightPhotoLog

data class WeightPhotoHistoryUiState(
    val isLoading: Boolean = true,
    val logs: List<WeightPhotoLog> = emptyList(),
    val error: String? = null
)

class WeightPhotoHistoryViewModel : ViewModel() {

    private val repository = WeightPhotoLogRepository()

    private val _uiState = MutableStateFlow(WeightPhotoHistoryUiState())
    val uiState: StateFlow<WeightPhotoHistoryUiState> = _uiState.asStateFlow()

    fun loadHistory() {
        val userId = LocalUser.user.uid
        if (userId.isBlank()) {
            _uiState.update { it.copy(isLoading = false, error = "Foydalanuvchi topilmadi.") }
            return
        }

        _uiState.update { it.copy(isLoading = true, error = null) }
        viewModelScope.launch {
            repository.getLogsFlow(userId)
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
                        is DataWrapper.Loading -> {
                            // isLoading is already true
                        }
                    }
                }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
}