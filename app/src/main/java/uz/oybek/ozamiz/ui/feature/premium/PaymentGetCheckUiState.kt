package uz.oybek.ozamiz.ui.feature.premium

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import androidx.annotation.Keep
import uz.oybek.ozamiz.R
import uz.oybek.ozamiz.common.result.BaseResult
import uz.oybek.ozamiz.data.premium.PaymentInfo
import uz.oybek.ozamiz.data.user.UserProfileRepository
import uz.oybek.ozamiz.data.user.local.LocalUser

@Keep
data class PaymentGetCheckUiState(
    val isLoading: Boolean = true,
    val premiumPrice: Long? = null,
    val paymentCardNumber: String? = null,
    val errorMessage: String? = null,
    val successMessage: String? = null,
    val isYearly: Boolean = false
)

class PaymentGetCheckViewModel(application: Application) : AndroidViewModel(application) {

    private val _uiState = MutableLiveData(PaymentGetCheckUiState())
    val uiState: LiveData<PaymentGetCheckUiState> = _uiState

    init {
        loadCardNumber()
    }

    fun initData(price: Long, isYearly: Boolean) {
        val current = _uiState.value ?: PaymentGetCheckUiState()
        _uiState.value = current.copy(
            premiumPrice = price,
            isYearly = isYearly,
            isLoading = current.paymentCardNumber == null
        )
    }

    private fun loadCardNumber() {
        PaymentInfo.loadPaymentCard { card ->
            viewModelScope.launch {
                val current = _uiState.value ?: PaymentGetCheckUiState()
                _uiState.postValue(current.copy(isLoading = false, paymentCardNumber = card))
            }
        }
    }

    fun submitPaymentCheck(checkImageUri: Uri) {
        val userId = LocalUser.user.uid
        if (userId.isBlank()) {
            _uiState.value =
                _uiState.value?.copy(errorMessage = getApplication<Application>().getString(R.string.error_user_not_found))
            return
        }

        _uiState.value =
            _uiState.value?.copy(isLoading = true, errorMessage = null, successMessage = null)
        viewModelScope.launch {
            val result = UserProfileRepository.uploadPremiumCheckData(
                userId,
                checkImageUri,
                _uiState.value?.isYearly ?: false
            )
            when (result) {
                is BaseResult.Success -> {
                    _uiState.postValue(
                        _uiState.value?.copy(
                            isLoading = false,
                            successMessage = "Chek muvaffaqiyatli yuborildi. Admin tekshiruvini kuting."
                        )
                    )
                }

                is BaseResult.Failure -> {
                    _uiState.postValue(
                        _uiState.value?.copy(
                            isLoading = false,
                            errorMessage = result.error
                        )
                    )
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