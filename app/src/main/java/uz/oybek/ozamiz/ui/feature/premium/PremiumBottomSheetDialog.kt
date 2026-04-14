package uz.oybek.ozamiz.ui.feature.premium

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.fragment.app.setFragmentResult
import androidx.fragment.app.viewModels
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import uz.oybek.ozamiz.data.user.UserRepository
import uz.oybek.ozamiz.databinding.PremiumSheetBinding
import uz.oybek.ozamiz.ui.utils.NumberFormattingUtils

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
        val sheet = dialog.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet) ?: return
        val behavior = BottomSheetBehavior.from(sheet)
        
        val isPending = UserRepository.user.paymentChecking
        
        if (isPending) {
            // For pending state, allow natural wrap_content height
            sheet.layoutParams.height = ViewGroup.LayoutParams.WRAP_CONTENT
            behavior.state = BottomSheetBehavior.STATE_EXPANDED
        } else {
            // For benefits list, force full height to support pinned footer button
            sheet.layoutParams.height = ViewGroup.LayoutParams.MATCH_PARENT
            behavior.state = BottomSheetBehavior.STATE_EXPANDED
        }
        sheet.requestLayout()
        
        behavior.skipCollapsed = true
        behavior.isDraggable = true
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

        showState(UserRepository.user.paymentChecking)

        viewModel.uiState.observe(viewLifecycleOwner) { state ->
            if (state.isLoading) return@observe
            state.premiumPrice?.let {
                val formattedPrice = NumberFormattingUtils.formatPrice(it)
                binding.priceView.text = "$formattedPrice so'm / oyiga"
            }
            showState(state.paymentChecking)
            
            // Re-run height update if state changes while open
            (dialog as? BottomSheetDialog)?.let { updateSheetHeight(it) }
        }

        binding.button.setOnClickListener {
            setFragmentResult(REQUEST_KEY, bundleOf(RESULT_ACTION to "go_to_payment"))
            dismiss()
        }

        binding.buttonClose.setOnClickListener {
            dismiss()
        }
    }

    private fun showState(isPending: Boolean) {
        if (isPending) {
            binding.headerPremium.visibility = View.GONE // HIDDEN per user request for pending state
            binding.layoutBenefits.visibility = View.GONE
            binding.layoutPinnedBottom.visibility = View.GONE
            binding.layoutVerificationPending.visibility = View.VISIBLE
            binding.layoutPinnedClose.visibility = View.VISIBLE
        } else {
            binding.headerPremium.visibility = View.VISIBLE
            binding.layoutBenefits.visibility = View.VISIBLE
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