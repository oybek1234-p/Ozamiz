package uz.oybek.ozamiz.ui.feature.premium

import androidx.annotation.Keep
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import uz.oybek.ozamiz.data.utils.DataWrapper
import uz.oybek.ozamiz.data.user.AdminRepository
import uz.oybek.ozamiz.data.user.model.User

@Keep
data class AdminPaymentVerificationListUiState(
    val isLoading: Boolean = true,
    val pendingUsers: List<User> = emptyList(),
    val errorMessage: String? = null
)

class AdminPaymentVerificationListViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(AdminPaymentVerificationListUiState())
    val uiState: StateFlow<AdminPaymentVerificationListUiState> = _uiState.asStateFlow()

    init {
        loadPendingVerifications()
    }

    fun loadPendingVerifications() {
        _uiState.update { it.copy(isLoading = true, errorMessage = null) }
        viewModelScope.launch {
            when (val result = AdminRepository.getPendingPremiumUsers()) {
                is DataWrapper.Success -> {
                    _uiState.update {
                        it.copy(isLoading = false, pendingUsers = result.data)
                    }
                }
                is DataWrapper.Error -> {
                    _uiState.update {
                        it.copy(isLoading = false, errorMessage = result.error, pendingUsers = emptyList())
                    }
                }
            }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }
}
