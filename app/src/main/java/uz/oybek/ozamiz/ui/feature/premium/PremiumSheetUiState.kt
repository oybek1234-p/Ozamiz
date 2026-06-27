package uz.oybek.ozamiz.ui.feature.premium

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch

data class PremiumSheetUiState(
    val isLoading: Boolean = false,
    val monthlyPrice: Long = 0L,
    val yearlyPrice: Long = 0L,
    val selectedPrice: Long = 0L,
    val errorMessage: String? = null,
    val paymentChecking: Boolean = false
)

class PremiumSheetViewModel : ViewModel() {

    private val _uiState = MutableLiveData(PremiumSheetUiState())
    val uiState: LiveData<PremiumSheetUiState> = _uiState

    init {
        loadPremiumPrices()
    }

    fun updateSelectedPrice(price: Long) {
        _uiState.value = _uiState.value?.copy(selectedPrice = price)
    }

    private fun loadPremiumPrices() {
        _uiState.value = _uiState.value?.copy(isLoading = true)
        val user = uz.oybek.ozamiz.data.user.UserRepository.user
        val isPaymentChecking = user.paymentChecking

        viewModelScope.launch {
            val result = uz.oybek.ozamiz.data.config.ConfigRepository.getPremiumPrices()
            if (result is uz.oybek.ozamiz.data.utils.DataWrapper.Success) {
                val (monthly, yearly) = result.data
                _uiState.postValue(
                    _uiState.value?.copy(
                        isLoading = false,
                        monthlyPrice = monthly,
                        yearlyPrice = yearly,
                        selectedPrice = monthly,
                        paymentChecking = isPaymentChecking
                    )
                )
            } else {
                _uiState.postValue(
                    _uiState.value?.copy(
                        isLoading = false,
                        errorMessage = ""
                    )
                )
            }
        }
    }
}