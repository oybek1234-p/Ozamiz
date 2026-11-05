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
data class AdminPaymentVerificationDetailUiState(
    val isLoading: Boolean = true,
    val user: User? = null,
    val errorMessageResId: Int? = null,
    val isProcessing: Boolean = false,
    val verificationComplete: Boolean = false
)

@HiltViewModel
class AdminPaymentVerificationDetailViewModel @Inject constructor(
    private val adminPremiumRepo: AdminPremiumRepo
) : ViewModel() {

    private val _uiState = MutableStateFlow(AdminPaymentVerificationDetailUiState())
    val uiState: StateFlow<AdminPaymentVerificationDetailUiState> = _uiState.asStateFlow()

    private var currentUserData: User? = null

    fun loadUserDetails(userId: String) {
        _uiState.update { it.copy(isLoading = true, errorMessageResId = null) }
        viewModelScope.launch {
            when (val result = adminPremiumRepo.getAdminUser(userId)) {
                is BaseResult.Success -> {
                    val foundUser = result.data
                    if (foundUser.paymentChecking) {
                        currentUserData = foundUser
                        _uiState.update {
                            it.copy(isLoading = false, user = foundUser)
                        }
                    } else {
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                errorMessageResId = R.string.premium_user_not_pending
                            )
                        }
                    }
                }
                is BaseResult.Failure -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessageResId = R.string.premium_user_not_pending
                        )
                    }
                }
            }
        }
    }

    fun processPaymentVerification(approve: Boolean) {
        val userToVerify = currentUserData ?: return
        _uiState.update { it.copy(isProcessing = true, errorMessageResId = null) }

        viewModelScope.launch {
            val result = adminPremiumRepo.verifyPayment(
                userId = userToVerify.uid,
                approve = approve,
                isYearly = userToVerify.isPremiumYearly,
                checkPhotoUrl = userToVerify.paymentCheckPhotoUrl
            )

            when (result) {
                is BaseResult.Success -> {
                    _uiState.update {
                        it.copy(
                            isProcessing = false,
                            verificationComplete = true,
                            user = userToVerify.copy(
                                premium = approve,
                                paymentChecking = false,
                                paymentCheckPhotoUrl = if (approve) userToVerify.paymentCheckPhotoUrl else null
                            )
                        )
                    }
                }
                is BaseResult.Failure -> {
                    _uiState.update {
                        it.copy(
                            isProcessing = false,
                            errorMessageResId = R.string.error_upload_check_photo
                        )
                    }
                }
            }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessageResId = null) }
    }

    fun resetVerificationCompleteFlag() {
        _uiState.update { it.copy(verificationComplete = false) }
    }
}
