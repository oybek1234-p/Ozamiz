package uz.oybek.ozamiz.ui.feature.premium

import android.net.Uri
import androidx.annotation.Keep
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import uz.oybek.ozamiz.R
import uz.oybek.ozamiz.common.result.BaseResult
import uz.oybek.ozamiz.data.premium.api.PremiumRepo
import uz.oybek.ozamiz.data.premium.model.PremiumError
import uz.oybek.ozamiz.ui.utils.getArgs
import javax.inject.Inject

@Keep
data class PaymentGetCheckUiState(
    val isLoading: Boolean = true,
    val isSubmitting: Boolean = false,
    val premiumPrice: Long? = null,
    val paymentCardNumber: String? = null,
    val errorMessageResId: Int? = null,
    val isSuccess: Boolean = false,
    val isYearly: Boolean = false
)

sealed interface PaymentGetCheckUiEffect {
    data object PaymentCheckSubmitted : PaymentGetCheckUiEffect
    data class ShowToastRes(val messageResId: Int) : PaymentGetCheckUiEffect
}

@HiltViewModel
class PaymentGetCheckViewModel @Inject constructor(
    private val premiumRepo: PremiumRepo,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val navArgs: PaymentGetCheckArgs? = savedStateHandle.getArgs<PaymentGetCheckArgs>()

    private val _uiState = MutableStateFlow(
        PaymentGetCheckUiState(
            premiumPrice = navArgs?.price,
            isYearly = navArgs?.isYearly ?: false
        )
    )
    val uiState: StateFlow<PaymentGetCheckUiState> = _uiState.asStateFlow()

    private val _effects = Channel<PaymentGetCheckUiEffect>(Channel.BUFFERED)
    val effects = _effects.receiveAsFlow()

    init {
        loadCardNumber()
    }

    fun initData(price: Long, isYearly: Boolean) {
        _uiState.update { current ->
            current.copy(
                premiumPrice = price,
                isYearly = isYearly,
                isLoading = current.paymentCardNumber == null
            )
        }
    }

    private fun loadCardNumber() {
        viewModelScope.launch {
            when (val result = premiumRepo.getPremiumInfo()) {
                is BaseResult.Success -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            paymentCardNumber = result.data.card
                        )
                    }
                }

                is BaseResult.Failure -> {
                    _uiState.update { it.copy(isLoading = false) }
                }
            }
        }
    }

    fun submitPaymentCheck(checkImageUri: Uri) {
        _uiState.update { it.copy(isSubmitting = true, errorMessageResId = null) }
        viewModelScope.launch {
            val isYearly = _uiState.value.isYearly
            when (val result = premiumRepo.uploadPaymentCheck(checkImageUri, isYearly)) {
                is BaseResult.Success -> {
                    _uiState.update {
                        it.copy(
                            isSubmitting = false,
                            isSuccess = true
                        )
                    }
                    _effects.send(PaymentGetCheckUiEffect.PaymentCheckSubmitted)
                }

                is BaseResult.Failure -> {
                    val errorRes = when (result.error) {
                        is PremiumError.UserNotFound -> R.string.error_user_not_found
                        is PremiumError.ImageUploadFailed -> R.string.error_upload_check_photo
                        else -> R.string.error_upload_check_photo
                    }
                    _uiState.update {
                        it.copy(
                            isSubmitting = false,
                            errorMessageResId = errorRes
                        )
                    }
                    _effects.send(PaymentGetCheckUiEffect.ShowToastRes(errorRes))
                }
            }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessageResId = null) }
    }
}
