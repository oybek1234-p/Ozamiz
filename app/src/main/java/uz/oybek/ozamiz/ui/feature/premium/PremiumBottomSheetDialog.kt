package uz.oybek.ozamiz.ui.feature.premium

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.fragment.app.setFragmentResult
import androidx.fragment.app.viewModels
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import uz.oybek.ozamiz.R
import uz.oybek.ozamiz.data.user.UserRepository
import uz.oybek.ozamiz.databinding.PremiumSheetBinding

class PremiumBottomSheetDialog : BottomSheetDialogFragment() {

    private var _binding: PremiumSheetBinding? = null
    private val binding get() = _binding!!

    private val viewModel: PremiumSheetViewModel by viewModels()

    companion object {
        const val TAG = "PremiumBottomSheetDialog"
        const val REQUEST_KEY = "PremiumBottomSheetRequestKey"
        const val RESULT_ACTION = "PremiumResultAction"
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = PremiumSheetBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        if (UserRepository.user.paymentChecking) {
            binding.layoutBenefits.visibility = View.GONE
            binding.layoutVerificationPending.visibility = View.VISIBLE
            binding.textView17.visibility = View.GONE
        } else {
            binding.layoutBenefits.visibility = View.VISIBLE
            binding.layoutVerificationPending.visibility = View.GONE
            binding.textView17.visibility = View.VISIBLE
            binding.textView17.text = getString(R.string.premium)
        }

        viewModel.uiState.observe(viewLifecycleOwner) { state ->
            if (state.isLoading) return@observe
            state.premiumPrice?.let {
                binding.priceView.text = "$it so'm / oyiga"
            }

            if (state.paymentChecking) {
                binding.layoutBenefits.visibility = View.GONE
                binding.layoutVerificationPending.visibility = View.VISIBLE
                binding.textView17.visibility = View.GONE
            } else {
                binding.layoutBenefits.visibility = View.VISIBLE
                binding.layoutVerificationPending.visibility = View.GONE
                binding.textView17.visibility = View.VISIBLE
                binding.textView17.text = getString(R.string.premium)
            }
        }
        binding.button.setOnClickListener {
            setFragmentResult(
                REQUEST_KEY, bundleOf(RESULT_ACTION to "go_to_payment")
            )
            dismiss()
        }

        binding.buttonClose.setOnClickListener {
            dismiss()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}