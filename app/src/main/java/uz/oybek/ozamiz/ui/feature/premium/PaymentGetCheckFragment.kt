package uz.oybek.ozamiz.ui.feature.premium

import uz.oybek.ozamiz.common.util.orZero
import android.net.Uri
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.fragment.app.setFragmentResult
import androidx.fragment.app.viewModels
import com.bumptech.glide.Glide
import uz.oybek.ozamiz.databinding.PaymentCheckFragmentBinding
import uz.oybek.ozamiz.ui.base.BaseFragment
import uz.oybek.ozamiz.ui.utils.NumberFormattingUtils
import uz.oybek.ozamiz.ui.utils.copyToClipboard
import uz.oybek.ozamiz.ui.utils.registerImagePicker
import uz.oybek.ozamiz.ui.utils.showToast

class PaymentGetCheckFragment : BaseFragment<PaymentCheckFragmentBinding>() {

    companion object {
        const val REQUEST_KEY = "payment_check_submitted"
        const val RESULT_SUBMITTED = "submitted"
        const val ARG_PRICE = "price"
        const val ARG_IS_YEARLY = "is_yearly"
    }

    private var selectedCheckUri: Uri? = null
    private val viewModel: PaymentGetCheckViewModel by viewModels()

    private val pickImageLauncher = registerImagePicker { uri ->
        selectedCheckUri = uri
        binding.checkImage.isVisible = true
        Glide.with(this).load(uri).into(binding.checkImage)
        binding.nextButton.isEnabled = true
    }

    override fun viewCreated(bind: PaymentCheckFragmentBinding) {
        val price = arguments?.getLong(ARG_PRICE, 0L).orZero()
        val isYearly = arguments?.getBoolean(ARG_IS_YEARLY, false) ?: false
        if (price > 0L) {
            viewModel.initData(price, isYearly)
        }
        bind.apply {
            toolbar.setUpBackButton(this@PaymentGetCheckFragment)

            viewModel.uiState.observe(viewLifecycleOwner) { state ->
                progressBar.isVisible = state.isLoading
                nextButton.isEnabled = !state.isLoading && selectedCheckUri != null
                checkButton.isEnabled = !state.isLoading

                cardNumber.text = NumberFormattingUtils.formatCardNumber(state.paymentCardNumber)
                textView10.text = "${NumberFormattingUtils.formatPrice(state.premiumPrice)} so'm"

                state.errorMessage?.let {
                    showToast(it)
                    viewModel.clearError()
                }
                state.successMessage?.let {
                    setFragmentResult(REQUEST_KEY, bundleOf(RESULT_SUBMITTED to true))
                    scrollViewForm.isVisible = false
                    layoutFormButtons.isVisible = false
                    layoutVerificationSubmitted.isVisible = true
                    viewModel.clearSuccess()
                }
            }

            btnSubmittedHome.setOnClickListener {
                popBackStack()
            }

            checkButton.setOnClickListener {
                pickImageLauncher.launch("image/*")
            }
            copyButton.setOnClickListener {
                val text = cardNumber.text.toString().replace(" ", "")
                copyToClipboard(text, "Karta raqami", "Karta raqami nusxalandi")
            }

            nextButton.setOnClickListener {
                selectedCheckUri?.let { uri ->
                    viewModel.submitPaymentCheck(uri)
                } ?: run {
                    showToast("Iltimos, avval chek rasmini yuklang")
                }
            }
        }
    }

    override fun inflateBinding(
        inflater: LayoutInflater,
        container: ViewGroup?
    ): PaymentCheckFragmentBinding {
        return PaymentCheckFragmentBinding.inflate(inflater, container, false)
    }
}