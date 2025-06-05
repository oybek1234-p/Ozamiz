package uz.oybek.ozamiz.ui.premium

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import uz.oybek.ozamiz.R
import uz.oybek.ozamiz.data.DataWrapper
import uz.oybek.ozamiz.data.LocalUser
import uz.oybek.ozamiz.data.UserRepository
import uz.oybek.ozamiz.data.premium.PaymentInfo

data class PaymentGetCheckUiState(
    val isLoading: Boolean = false,
    val premiumPrice: Long? = null,
    val paymentCardNumber: String? = null,
    val errorMessage: String? = null,
    val successMessage: String? = null
)

class PaymentGetCheckViewModel(application: Application) : AndroidViewModel(application) {

    private val _uiState = MutableLiveData(PaymentGetCheckUiState())
    val uiState: LiveData<PaymentGetCheckUiState> = _uiState

    init {
        loadInitialData()
    }

    private fun loadInitialData() {
        _uiState.value = _uiState.value?.copy(isLoading = true)
        PremiumUtil.loadPremiumPrice { price ->
            PaymentInfo.loadPaymentCard { card ->
                _uiState.postValue(
                    _uiState.value?.copy(
                        isLoading = false,
                        premiumPrice = price,
                        paymentCardNumber = card
                    )
                )
            }
        }
    }

    fun submitPaymentCheck(checkImageUri: Uri) {
        val userId = LocalUser.user.uid
        if (userId.isBlank()) {
            _uiState.value = _uiState.value?.copy(errorMessage = getApplication<Application>().getString(R.string.error_user_not_found))
            return
        }

        _uiState.value = _uiState.value?.copy(isLoading = true, errorMessage = null, successMessage = null)
        viewModelScope.launch {
            val result = UserRepository.uploadPremiumCheckData(userId, checkImageUri)
            when (result) {
                is DataWrapper.Success -> {
                    _uiState.postValue(
                        _uiState.value?.copy(
                            isLoading = false,
                            successMessage = "Chek muvaffaqiyatli yuborildi. Admin tekshiruvini kuting."
                        )
                    )
                }
                is DataWrapper.Error -> {
                    _uiState.postValue(
                        _uiState.value?.copy(
                            isLoading = false,
                            errorMessage = result.error
                        )
                    )
                }
                is DataWrapper.Loading -> {
                    // Already handled
                }
            }
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value?.copy(errorMessage = null)
    }
    fun clearSuccess() {
        _uiState.value = _uiState.value?.copy(successMessage = null)
    }
}