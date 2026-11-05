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
import uz.oybek.ozamiz.data.premium.api.PremiumRepo
import uz.oybek.ozamiz.data.profile.api.ProfileRepo
import javax.inject.Inject

enum class PlanType {
    MONTHLY,
    YEARLY
}

@Keep
data class PremiumSheetUiState(
    val isLoading: Boolean = false,
    val monthlyPrice: Long = 0L,
    val yearlyPrice: Long = 0L,
    val selectedPrice: Long = 0L,
    val selectedPlan: PlanType = PlanType.YEARLY,
    val errorMessageResId: Int? = null,
    val paymentChecking: Boolean = false
) {
    val yearlyMonthlyEquivalent: Long
        get() = if (yearlyPrice > 0L) yearlyPrice / 12L else 0L

    val isYearlySelected: Boolean
        get() = selectedPlan == PlanType.YEARLY

    val discountPercent: Int
        get() {
            if (monthlyPrice <= 0L || yearlyPrice <= 0L) return 0
            val monthlyPerYear = monthlyPrice * 12.0
            val savings = ((monthlyPerYear - yearlyPrice) / monthlyPerYear) * 100.0
            return savings.toInt().coerceAtLeast(0)
        }
}

@HiltViewModel
class PremiumSheetViewModel @Inject constructor(
    private val premiumRepo: PremiumRepo,
    private val profileRepo: ProfileRepo
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        PremiumSheetUiState(
            paymentChecking = profileRepo.currentUser.paymentChecking
        )
    )
    val uiState: StateFlow<PremiumSheetUiState> = _uiState.asStateFlow()

    init {
        loadPremiumPrices()
    }

    fun selectPlan(planType: PlanType) {
        _uiState.update { current ->
            val price = if (planType == PlanType.YEARLY) current.yearlyPrice else current.monthlyPrice
            current.copy(
                selectedPlan = planType,
                selectedPrice = price
            )
        }
    }

    fun updateSelectedPrice(price: Long) {
        _uiState.update { current ->
            val plan = if (price == current.yearlyPrice) PlanType.YEARLY else PlanType.MONTHLY
            current.copy(
                selectedPrice = price,
                selectedPlan = plan
            )
        }
    }

    private fun loadPremiumPrices() {
        _uiState.update {
            it.copy(
                isLoading = true,
                paymentChecking = profileRepo.currentUser.paymentChecking
            )
        }

        viewModelScope.launch {
            when (val result = premiumRepo.getPremiumInfo()) {
                is BaseResult.Success -> {
                    val info = result.data
                    _uiState.update { current ->
                        val initialPlan = PlanType.YEARLY
                        val initialPrice = if (info.yearlyPrice > 0L) info.yearlyPrice else info.monthlyPrice
                        current.copy(
                            isLoading = false,
                            monthlyPrice = info.monthlyPrice,
                            yearlyPrice = info.yearlyPrice,
                            selectedPlan = initialPlan,
                            selectedPrice = initialPrice,
                            paymentChecking = profileRepo.currentUser.paymentChecking
                        )
                    }
                }
                is BaseResult.Failure -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessageResId = R.string.premium_price_not_found
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
