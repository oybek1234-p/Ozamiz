package uz.oybek.ozamiz.ui.feature.premium

import androidx.annotation.Keep
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import uz.oybek.ozamiz.R
import uz.oybek.ozamiz.common.result.BaseResult
import uz.oybek.ozamiz.data.premium.api.AdminPremiumRepo
import uz.oybek.ozamiz.data.profile.model.User
import javax.inject.Inject

@Keep
data class AdminPaymentVerificationListUiState(
    val isLoading: Boolean = true,
    val pendingUsers: List<User> = emptyList(),
    val errorMessageResId: Int? = null
)

@HiltViewModel
class AdminPaymentVerificationListViewModel @Inject constructor(
    private val adminPremiumRepo: AdminPremiumRepo
) : ViewModel() {

    private val _uiState = MutableStateFlow(AdminPaymentVerificationListUiState())
    val uiState: StateFlow<AdminPaymentVerificationListUiState> = _uiState.asStateFlow()

    init {
        loadPendingVerifications()
    }

    fun loadPendingVerifications() {
        _uiState.update { it.copy(isLoading = true, errorMessageResId = null) }
        viewModelScope.launch {
            when (val result = adminPremiumRepo.getPendingVerifications()) {
                is BaseResult.Success -> {
                    _uiState.update {
                        it.copy(isLoading = false, pendingUsers = result.data)
                    }
                }
                is BaseResult.Failure -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessageResId = R.string.no_pending_verifications,
                            pendingUsers = emptyList()
                        )
                    }
                }
            }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessageResId = null) }
    }
}
