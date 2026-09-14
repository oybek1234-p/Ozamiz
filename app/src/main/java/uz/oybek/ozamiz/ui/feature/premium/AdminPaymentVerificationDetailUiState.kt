package uz.oybek.ozamiz.ui.feature.premium

import androidx.annotation.Keep
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import uz.oybek.ozamiz.data.firebase.uploader.ImageUploader
import uz.oybek.ozamiz.data.user.AdminRepository
import uz.oybek.ozamiz.data.user.model.User

@Keep
data class AdminPaymentVerificationDetailUiState(
    val isLoading: Boolean = true,
    val user: User? = null,
    val errorMessage: String? = null,
    val isProcessing: Boolean = false,
    val verificationComplete: Boolean = false
)

class AdminPaymentVerificationDetailViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(AdminPaymentVerificationDetailUiState())
    val uiState: StateFlow<AdminPaymentVerificationDetailUiState> = _uiState.asStateFlow()

    private var currentUserData: User? = null

    fun loadUserDetails(userId: String) {
        _uiState.update { it.copy(isLoading = true, errorMessage = null) }
        viewModelScope.launch {
            val foundUser = AdminRepository.getAdminUser(userId)
            if (foundUser != null && foundUser.paymentChecking) {
                currentUserData = foundUser
                _uiState.update {
                    it.copy(isLoading = false, user = foundUser)
                }
            } else {
                _uiState.update {
                    it.copy(isLoading = false, errorMessage = "Foydalanuvchi topilmadi yoki tekshiruvda emas.")
                }
            }
        }
    }

    fun processPaymentVerification(approve: Boolean) {
        val userToVerify = currentUserData ?: return
        _uiState.update { it.copy(isProcessing = true, errorMessage = null) }

        viewModelScope.launch {
            // Approve/Reject Logic
            AdminRepository.setPremium(userToVerify.uid, approve, approve, userToVerify.isPremiumYearly)

            if (!approve && !userToVerify.paymentCheckPhotoUrl.isNullOrEmpty()) {
                ImageUploader.deleteImage(userToVerify.paymentCheckPhotoUrl)
            }

            _uiState.update {
                it.copy(
                    isProcessing = false,
                    verificationComplete = true,
                    user = userToVerify.copy(
                        premium = approve,
                        paymentChecking = false,
                        paymentCheckPhotoUrl = if(approve) userToVerify.paymentCheckPhotoUrl else null
                    )
                )
            }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    fun resetVerificationCompleteFlag() {
        _uiState.update { it.copy(verificationComplete = false) }
    }
}
