package uz.oybek.ozamiz.ui.feature.premium

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.fragment.app.setFragmentResult
import androidx.fragment.app.viewModels
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.color.MaterialColors
import uz.oybek.ozamiz.databinding.PremiumSheetBinding
import uz.oybek.ozamiz.ui.utils.NumberFormattingUtils
import uz.oybek.ozamiz.ui.utils.showToast

class PremiumBottomSheetDialog : BottomSheetDialogFragment() {

    private var _binding: PremiumSheetBinding? = null
    private val binding get() = _binding!!

    private val viewModel: PremiumSheetViewModel by viewModels()

    companion object {
        const val TAG = "PremiumBottomSheetDialog"
        const val REQUEST_KEY = "PremiumBottomSheetRequestKey"
        const val RESULT_ACTION = "PremiumResultAction"
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return (super.onCreateDialog(savedInstanceState) as BottomSheetDialog).also { dialog ->
            dialog.setOnShowListener {
                updateSheetHeight(dialog)
            }
        }
    }

    private fun updateSheetHeight(dialog: BottomSheetDialog) {
        val sheet = dialog.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)
            ?: return
        val behavior = BottomSheetBehavior.from(sheet)
        behavior.skipCollapsed = true
        behavior.state = BottomSheetBehavior.STATE_EXPANDED
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = PremiumSheetBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        (view.parent as? View)?.setBackgroundColor(android.graphics.Color.TRANSPARENT)

        viewModel.uiState.observe(viewLifecycleOwner) { state ->
            val loaded = state.monthlyPrice > 0L
            binding.progressLoader.isVisible = !loaded
            binding.layoutPlans.alpha = if (loaded) 1f else 0f

            if (!loaded) return@observe
            
            val monthlyPrice = state.monthlyPrice
            val yearlyPrice = state.yearlyPrice
            val isMonthly = state.selectedPrice == monthlyPrice
            val context = requireContext()
            
            val primaryColor = MaterialColors.getColor(context, androidx.appcompat.R.attr.colorPrimary, android.graphics.Color.GREEN)
            val outlineColor = MaterialColors.getColor(context, com.google.android.material.R.attr.colorOutlineVariant, android.graphics.Color.LTGRAY)
            val surfaceColor = MaterialColors.getColor(context, com.google.android.material.R.attr.colorSurfaceContainerLowest, android.graphics.Color.WHITE)
            
            // Monthly Card
            binding.cardMonthly.strokeWidth = if (isMonthly) 2.dpToPx() else 1.dpToPx()
            binding.cardMonthly.strokeColor = if (isMonthly) primaryColor else outlineColor
            binding.cardMonthly.setCardBackgroundColor(surfaceColor)
            binding.radioMonthly.isChecked = isMonthly

            // Yearly Card
            binding.cardYearly.strokeWidth = if (!isMonthly) 2.dpToPx() else 1.dpToPx()
            binding.cardYearly.strokeColor = if (!isMonthly) primaryColor else outlineColor
            binding.cardYearly.setCardBackgroundColor(surfaceColor)
            binding.radioYearly.isChecked = !isMonthly
            binding.textMonthlyPrice.text = "${NumberFormattingUtils.formatPrice(monthlyPrice)} sum"
            binding.textYearlyPrice.text = "${NumberFormattingUtils.formatPrice(yearlyPrice)} sum"
            
            val yearlyMonthlyEquivalent = yearlyPrice / 12
            binding.textYearlyMonthlyEquivalent.text = "${NumberFormattingUtils.formatPrice(yearlyMonthlyEquivalent)} sum / oy"

            showState(state.paymentChecking)
        }

        binding.cardMonthly.setOnClickListener {
            val price = viewModel.uiState.value?.monthlyPrice ?: 0L
            if (price > 0) viewModel.updateSelectedPrice(price)
        }

        binding.cardYearly.setOnClickListener {
            val price = viewModel.uiState.value?.yearlyPrice ?: 0L
            if (price > 0) viewModel.updateSelectedPrice(price)
        }

        binding.button.setOnClickListener {
            val selectedPrice = viewModel.uiState.value?.selectedPrice ?: 0L
            if (selectedPrice <= 0) {
                showToast("Narx yuklanmadi")
                return@setOnClickListener
            }
            setFragmentResult(REQUEST_KEY, bundleOf(
                RESULT_ACTION to "go_to_payment",
                "selected_price" to selectedPrice,
                "is_yearly" to (selectedPrice == viewModel.uiState.value?.yearlyPrice)
            ))
            dismiss()
        }

        binding.buttonClose.setOnClickListener {
            dismiss()
        }
    }



    private fun Int.dpToPx(): Int = (this * resources.displayMetrics.density).toInt()

    private fun showState(isPending: Boolean) {
        if (isPending) {
            binding.headerPremium.visibility = View.GONE
            binding.layoutBenefits.visibility = View.GONE
            binding.layoutSelection.visibility = View.GONE
            binding.layoutPinnedBottom.visibility = View.GONE
            binding.layoutVerificationPending.visibility = View.VISIBLE
            binding.layoutPinnedClose.visibility = View.VISIBLE
        } else {
            binding.headerPremium.visibility = View.VISIBLE
            binding.layoutBenefits.visibility = View.VISIBLE
            binding.layoutSelection.visibility = View.VISIBLE
            binding.layoutPinnedBottom.visibility = View.VISIBLE
            binding.layoutVerificationPending.visibility = View.GONE
            binding.layoutPinnedClose.visibility = View.GONE
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}