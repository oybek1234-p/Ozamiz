package uz.oybek.ozamiz.ui.feature.premium

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
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
        const val KEY_SELECTED_PRICE = "selected_price"
        const val KEY_IS_YEARLY = "is_yearly"
        const val ACTION_GO_TO_PAYMENT = "go_to_payment"
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return (super.onCreateDialog(savedInstanceState) as BottomSheetDialog).also { dialog ->
            dialog.setOnShowListener {
                val sheet =
                    dialog.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)
                        ?: return@setOnShowListener
                val behavior = BottomSheetBehavior.from(sheet)
                behavior.skipCollapsed = true
                behavior.state = BottomSheetBehavior.STATE_EXPANDED
            }
        }
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
            if (state.paymentChecking) {
                binding.progressLoader.isVisible = false
                binding.layoutPlans.alpha = 0f
                showState(true)
                replacePremiumText(binding.root)
                return@observe
            }

            val loaded = state.monthlyPrice > 0L
            binding.progressLoader.isVisible = !loaded
            binding.layoutPlans.alpha = if (loaded) 1f else 0f

            if (!loaded) return@observe

            val monthlyPrice = state.monthlyPrice
            val yearlyPrice = state.yearlyPrice
            val isMonthly = state.selectedPrice == monthlyPrice
            val context = requireContext()

            val primaryColor = MaterialColors.getColor(
                context,
                androidx.appcompat.R.attr.colorPrimary,
                android.graphics.Color.GREEN
            )
            val outlineColor = MaterialColors.getColor(
                context,
                com.google.android.material.R.attr.colorOutlineVariant,
                android.graphics.Color.LTGRAY
            )
            val surfaceColor = MaterialColors.getColor(
                context,
                com.google.android.material.R.attr.colorSurfaceContainerLowest,
                android.graphics.Color.WHITE
            )

            binding.cardMonthly.strokeWidth = if (isMonthly) 2.dpToPx() else 1.dpToPx()
            binding.cardMonthly.strokeColor = if (isMonthly) primaryColor else outlineColor
            binding.cardMonthly.setCardBackgroundColor(surfaceColor)
            binding.radioMonthly.isChecked = isMonthly

            binding.cardYearly.strokeWidth = if (!isMonthly) 2.dpToPx() else 1.dpToPx()
            binding.cardYearly.strokeColor = if (!isMonthly) primaryColor else outlineColor
            binding.cardYearly.setCardBackgroundColor(surfaceColor)
            binding.radioYearly.isChecked = !isMonthly

            binding.textMonthlyPrice.text = "${NumberFormattingUtils.formatPrice(monthlyPrice)} sum"
            binding.textYearlyPrice.text = "${NumberFormattingUtils.formatPrice(yearlyPrice)} sum"

            val yearlyMonthlyEquivalent = yearlyPrice / 12
            binding.textYearlyMonthlyEquivalent.text =
                "Atigi ${NumberFormattingUtils.formatPrice(yearlyMonthlyEquivalent)} sum / oy 🚀"

            showState(state.paymentChecking)
            replacePremiumText(binding.root)
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
                showToast("Ma'lumot yuklanmoqda...")
                return@setOnClickListener
            }
            setFragmentResult(
                REQUEST_KEY, bundleOf(
                    RESULT_ACTION to ACTION_GO_TO_PAYMENT,
                    KEY_SELECTED_PRICE to selectedPrice,
                    KEY_IS_YEARLY to (selectedPrice == viewModel.uiState.value?.yearlyPrice)
                )
            )
            dismiss()
        }
        replacePremiumText(binding.root)
    }

    private fun replacePremiumText(view: View) {
        if (view is ViewGroup) {
            for (i in 0 until view.childCount) {
                replacePremiumText(view.getChildAt(i))
            }
        } else if (view is TextView) {
            val currentText = view.text.toString()
            if (currentText.contains("Premium", ignoreCase = true)) {
                view.text = currentText
                    .replace("Premium obuna", "Shaxsiy reja ✨")
                    .replace("Premium", "Shaxsiy ✨")
                    .replace("PREMIUM", "SHAXSIY ✨")
            }
            if (currentText.contains("AI dietolog")) view.text = "$currentText 🤖"
            if (currentText.contains("receptlar")) view.text = "$currentText 🍳"
            if (currentText.contains("tahlil")) view.text = "$currentText 📊"
        }
    }

    private fun showRefundTerms() {
        AlertDialog.Builder(requireContext())
            .setTitle("Pulni qaytarish shartlari 💰")
            .setMessage("Shart: 1 oyda 24 kun davomida har kuni kamida 2 mahal ovqat qoshsangiz, oy oxirida to'lovingiz 100% qaytarib beriladi! Bu faqat oylik obuna uchun amal qiladi.")
            .setPositiveButton("Tushunarli ✅", null)
            .show()
    }

    private fun Int.dpToPx(): Int = (this * resources.displayMetrics.density).toInt()

    private fun showState(isPending: Boolean) {
        if (isPending) {
            binding.headerPremium.visibility = View.GONE
            binding.layoutBenefits.visibility = View.GONE
            binding.layoutSelection.visibility = View.GONE
            binding.layoutPinnedBottom.visibility = View.GONE
            binding.layoutVerificationPending.visibility = View.VISIBLE
        } else {
            binding.headerPremium.visibility = View.VISIBLE
            binding.layoutBenefits.visibility = View.VISIBLE
            binding.layoutSelection.visibility = View.VISIBLE
            binding.layoutPinnedBottom.visibility = View.VISIBLE
            binding.layoutVerificationPending.visibility = View.GONE
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}