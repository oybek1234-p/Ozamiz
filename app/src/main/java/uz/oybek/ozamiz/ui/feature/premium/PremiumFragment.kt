package uz.oybek.ozamiz.ui.feature.premium

import android.content.res.ColorStateList
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.fragment.app.setFragmentResult
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.google.android.material.color.MaterialColors
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import androidx.navigation.NavOptions
import uz.oybek.ozamiz.R
import uz.oybek.ozamiz.databinding.FragmentPremiumBinding
import uz.oybek.ozamiz.ui.base.BaseFragment
import uz.oybek.ozamiz.ui.utils.NumberFormattingUtils
import uz.oybek.ozamiz.ui.utils.appColors
import uz.oybek.ozamiz.ui.utils.showToast
import uz.oybek.ozamiz.ui.utils.toBundle

@AndroidEntryPoint
class PremiumFragment : BaseFragment<FragmentPremiumBinding>() {

    private val viewModel: PremiumSheetViewModel by viewModels()

    init {
        showBottomSheet = false
    }

    override fun inflateBinding(
        inflater: LayoutInflater,
        container: ViewGroup?
    ): FragmentPremiumBinding {
        return FragmentPremiumBinding.inflate(inflater, container, false)
    }

    override fun viewCreated(bind: FragmentPremiumBinding) {
        requireActivity().onBackPressedDispatcher.addCallback(
            viewLifecycleOwner,
            object : androidx.activity.OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    handleBackOrClose()
                }
            }
        )

        bind.btnClose.setOnClickListener {
            handleBackOrClose()
        }

        val mealType = arguments?.getString("mealType") ?: "breakfast"
        bind.btnFoodList.setOnClickListener {
            val navOptions = NavOptions.Builder()
                .setPopUpTo(R.id.home, false)
                .build()
            val args = uz.oybek.ozamiz.ui.feature.log.food.SearchFoodArgs(mealType = mealType)
            navigate(R.id.searchFoodFragment, args.toBundle(), navOptions)
        }

        bind.cardMonthly.setOnClickListener {
            viewModel.selectPlan(PlanType.MONTHLY)
        }

        bind.cardYearly.setOnClickListener {
            viewModel.selectPlan(PlanType.YEARLY)
        }

        bind.button.setOnClickListener {
            val state = viewModel.uiState.value
            val selectedPrice = state.selectedPrice
            if (selectedPrice <= 0L) {
                showToast(getString(R.string.premium_payment_loading))
                return@setOnClickListener
            }

            val isYearly = state.isYearlySelected
            val argsBundle = PaymentGetCheckArgs(price = selectedPrice, isYearly = isYearly).toBundle()

            setFragmentResult(
                PremiumBottomSheetDialog.REQUEST_KEY,
                bundleOf(
                    PremiumBottomSheetDialog.RESULT_ACTION to PremiumBottomSheetDialog.ACTION_GO_TO_PAYMENT,
                    PremiumBottomSheetDialog.KEY_SELECTED_PRICE to selectedPrice,
                    PremiumBottomSheetDialog.KEY_IS_YEARLY to isYearly
                )
            )

            val navOptions = androidx.navigation.NavOptions.Builder()
                .setPopUpTo(R.id.premiumFragment, true)
                .build()
            navigate(R.id.paymentGetCheckFragment, argsBundle, navOptions)
        }

        observeUiState(bind)
    }

    private fun observeUiState(bind: FragmentPremiumBinding) {
        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    renderState(bind, state)
                }
            }
        }
    }

    private fun renderState(bind: FragmentPremiumBinding, state: PremiumSheetUiState) {
        if (state.paymentChecking) {
            popBackStack()
            PaymentCheckSubmittedBottomSheet().show(
                parentFragmentManager,
                "PaymentCheckSubmittedBottomSheet"
            )
            return
        }

        val isLoaded = state.monthlyPrice > 0L
        bind.progressLoader.isVisible = state.isLoading || !isLoaded
        bind.layoutPlans.alpha = if (isLoaded) 1f else 0f

        if (!isLoaded) return

        val selectedCardBg = appColors.colorSelectedCard
        val unselectedCardBg = android.graphics.Color.TRANSPARENT
        val primaryColor = appColors.colorPrimary
        val outlineColor = appColors.colorOutlineVariant
        val onSurfaceColor = appColors.colorOnSurface

        val isYearly = state.isYearlySelected

        // Yearly Card Styling (Selected vs Unselected)
        bind.cardYearly.strokeWidth = if (isYearly) 2.dpToPx() else 1.dpToPx()
        bind.cardYearly.strokeColor = if (isYearly) primaryColor else outlineColor
        bind.cardYearly.setCardBackgroundColor(if (isYearly) selectedCardBg else unselectedCardBg)
        bind.imgYearlyRadio.imageTintList = ColorStateList.valueOf(if (isYearly) primaryColor else outlineColor)
        bind.textYearlyTitle.setTextColor(onSurfaceColor)

        // Monthly Card Styling (Selected vs Unselected)
        bind.cardMonthly.strokeWidth = if (!isYearly) 2.dpToPx() else 1.dpToPx()
        bind.cardMonthly.strokeColor = if (!isYearly) primaryColor else outlineColor
        bind.cardMonthly.setCardBackgroundColor(if (!isYearly) selectedCardBg else unselectedCardBg)
        bind.imgMonthlyRadio.imageTintList = ColorStateList.valueOf(if (!isYearly) primaryColor else outlineColor)
        bind.textMonthlyTitle.setTextColor(onSurfaceColor)

        // Text & Prices
        bind.textMonthlyPrice.text = getString(
            R.string.premium_price_per_month,
            NumberFormattingUtils.formatPrice(state.monthlyPrice)
        )
        bind.textYearlyPrice.text = getString(
            R.string.premium_price_per_year,
            NumberFormattingUtils.formatPrice(state.yearlyPrice)
        )
        bind.textYearlyMonthlyEquivalent.text = getString(
            R.string.premium_price_per_month,
            NumberFormattingUtils.formatPrice(state.yearlyMonthlyEquivalent)
        )

        // Dynamic Action Button Text
        if (state.selectedPrice > 0L) {
            bind.button.text = getString(
                R.string.premium_btn_continue_with_price,
                "${NumberFormattingUtils.formatPrice(state.selectedPrice)} sum"
            )
        } else {
            bind.button.text = getString(R.string.premium_btn_continue)
        }

        state.errorMessageResId?.let { resId ->
            showToast(getString(resId))
            viewModel.clearError()
        }
    }

    private fun handleBackOrClose() {
        val mealType = arguments?.getString("mealType")
        if (!mealType.isNullOrBlank()) {
            val navOptions = NavOptions.Builder()
                .setPopUpTo(R.id.home, false)
                .build()
            navigate(R.id.logFoodFragment, uz.oybek.ozamiz.ui.feature.log.food.LogFoodArgs(mealType = mealType).toBundle(), navOptions)
        } else {
            popBackStack()
        }
    }

    private fun Int.dpToPx(): Int = (this * resources.displayMetrics.density).toInt()
}
