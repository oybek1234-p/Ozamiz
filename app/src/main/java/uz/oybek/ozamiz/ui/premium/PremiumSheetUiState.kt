package uz.oybek.ozamiz.ui.premium

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import uz.oybek.ozamiz.data.premium.PaymentInfo

data class PremiumSheetUiState(
    val isLoading: Boolean = false,
    val premiumPrice: Long? = null,
    val errorMessage: String? = null
)

class PremiumSheetViewModel : ViewModel() {

    private val _uiState = MutableLiveData(PremiumSheetUiState())
    val uiState: LiveData<PremiumSheetUiState> = _uiState

    init {
        loadPremiumPrice()
    }

    private fun loadPremiumPrice() {
        _uiState.value = _uiState.value?.copy(isLoading = true)
        PremiumUtil.loadPremiumPrice { price ->
            _uiState.postValue(
                _uiState.value?.copy(
                    isLoading = false,
                    premiumPrice = price
                )
            )
        }
    }
}