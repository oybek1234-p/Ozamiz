package uz.oybek.ozamiz.ui.feature.premium

import uz.oybek.ozamiz.common.util.orZero
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.fragment.app.setFragmentResult
import androidx.fragment.app.viewModels
import com.google.android.material.color.MaterialColors
import uz.oybek.ozamiz.R
import uz.oybek.ozamiz.data.analytics.AnalyticsManager
import uz.oybek.ozamiz.databinding.FragmentPremiumBinding
import uz.oybek.ozamiz.ui.base.BaseFragment
import uz.oybek.ozamiz.ui.utils.NumberFormattingUtils
import uz.oybek.ozamiz.ui.utils.showToast

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
        bind.btnClose.setOnClickListener {
            popBackStack()
        }

        viewModel.uiState.observe(viewLifecycleOwner) { state ->
            if (state.paymentChecking) {
                bind.progressLoader.isVisible = false
                bind.layoutPlans.alpha = 0f
                showState(bind, true)
                return@observe
            }

            val loaded = state.monthlyPrice > 0L
            bind.progressLoader.isVisible = !loaded
            bind.layoutPlans.alpha = if (loaded) 1f else 0f

            if (!loaded) return@observe

            val monthlyPrice = state.monthlyPrice
            val yearlyPrice = state.yearlyPrice
            val isMonthly = state.selectedPrice == monthlyPrice
            val context = requireContext()

            val primaryColor = MaterialColors.getColor(context, androidx.appcompat.R.attr.colorPrimary, android.graphics.Color.BLACK)
            val outlineColor = MaterialColors.getColor(context, com.google.android.material.R.attr.colorOutlineVariant, android.graphics.Color.LTGRAY)
            val surfaceColor = MaterialColors.getColor(context, com.google.android.material.R.attr.colorSurfaceContainerLowest, android.graphics.Color.WHITE)

            bind.cardMonthly.strokeWidth = if (isMonthly) 2.dpToPx() else 1.dpToPx()
            bind.cardMonthly.strokeColor = if (isMonthly) primaryColor else outlineColor
            bind.cardMonthly.setCardBackgroundColor(surfaceColor)

            bind.cardYearly.strokeWidth = if (!isMonthly) 2.dpToPx() else 1.dpToPx()
            bind.cardYearly.strokeColor = if (!isMonthly) primaryColor else outlineColor
            bind.cardYearly.setCardBackgroundColor(surfaceColor)

            bind.textMonthlyPrice.text = "${NumberFormattingUtils.formatPrice(monthlyPrice)} sum / oy"
            bind.textYearlyPrice.text = "${NumberFormattingUtils.formatPrice(yearlyPrice)} sum / yil"

            val yearlyMonthlyEquivalent = yearlyPrice / 12
            bind.textYearlyMonthlyEquivalent.text = "${NumberFormattingUtils.formatPrice(yearlyMonthlyEquivalent)} sum / oy"

            showState(bind, state.paymentChecking)
        }

        bind.cardMonthly.setOnClickListener {
            val price = viewModel.uiState.value?.monthlyPrice.orZero()
            if (price > 0) viewModel.updateSelectedPrice(price)
        }

        bind.cardYearly.setOnClickListener {
            val price = viewModel.uiState.value?.yearlyPrice.orZero()
            if (price > 0) viewModel.updateSelectedPrice(price)
        }

        bind.button.setOnClickListener {
            val selectedPrice = viewModel.uiState.value?.selectedPrice.orZero()
            if (selectedPrice <= 0) {
                showToast("Ma'lumot yuklanmoqda...")
                return@setOnClickListener
            }

            val isYearly = selectedPrice == viewModel.uiState.value?.yearlyPrice
            AnalyticsManager.logFeatureUsed(
                "premium_purchase_started",
                mapOf("price" to selectedPrice, "is_yearly" to isYearly)
            )

            val args = Bundle().apply {
                putLong(PaymentGetCheckFragment.ARG_PRICE, selectedPrice)
                putBoolean(PaymentGetCheckFragment.ARG_IS_YEARLY, isYearly)
            }

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
            navigate(R.id.paymentGetCheckFragment, args, navOptions)
        }
    }

    private fun Int.dpToPx(): Int = (this * resources.displayMetrics.density).toInt()

    private fun showState(bind: FragmentPremiumBinding, isPending: Boolean) {
        if (isPending) {
            bind.layoutBenefits.visibility = View.GONE
            bind.layoutSelection.visibility = View.GONE
            bind.layoutPinnedBottom.visibility = View.GONE
            bind.layoutVerificationPending.visibility = View.VISIBLE
        } else {
            bind.layoutBenefits.visibility = View.VISIBLE
            bind.layoutSelection.visibility = View.VISIBLE
            bind.layoutPinnedBottom.visibility = View.VISIBLE
            bind.layoutVerificationPending.visibility = View.GONE
        }
    }
}
