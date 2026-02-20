package uz.oybek.ozamiz.ui.feature.premium

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

data class PremiumSheetUiState(
    val isLoading: Boolean = false,
    val premiumPrice: Long? = null,
    val errorMessage: String? = null,
    val paymentChecking: Boolean = false
)

class PremiumSheetViewModel : ViewModel() {

    private val _uiState = MutableLiveData(PremiumSheetUiState())
    val uiState: LiveData<PremiumSheetUiState> = _uiState

    init {
        loadPremiumPrice()
    }

    private fun loadPremiumPrice() {
        _uiState.value = _uiState.value?.copy(isLoading = true)
        val user = uz.oybek.ozamiz.data.user.UserRepository.user
        val isPaymentChecking = user.paymentChecking

        PremiumUtil.loadPremiumPrice { price ->
            _uiState.postValue(
                _uiState.value?.copy(
                    isLoading = false,
                    premiumPrice = price,
                    paymentChecking = isPaymentChecking
                )
            )
        }
    }
}