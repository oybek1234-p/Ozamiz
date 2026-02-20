package uz.oybek.ozamiz.ui.feature.premium

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.net.Uri
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.isVisible
import androidx.fragment.app.setFragmentResult
import androidx.fragment.app.viewModels
import com.bumptech.glide.Glide
import uz.oybek.ozamiz.databinding.PaymentCheckFragmentBinding
import uz.oybek.ozamiz.ui.base.BaseFragment
import uz.oybek.ozamiz.ui.utils.showToast

class PaymentGetCheckFragment : BaseFragment<PaymentCheckFragmentBinding>() {

    private var selectedCheckUri: Uri? = null
    private val viewModel: PaymentGetCheckViewModel by viewModels()

    private val pickImageLauncher =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
            uri?.let {
                selectedCheckUri = it
                binding.checkImage.isVisible = true
                Glide.with(this).load(it).into(binding.checkImage)
                binding.nextButton.isEnabled = true
            }
        }

    override fun viewCreated(bind: PaymentCheckFragmentBinding) {
        bind.apply {
            toolbar.setUpBackButton(this@PaymentGetCheckFragment)

            viewModel.uiState.observe(viewLifecycleOwner) { state ->
                progressBar.isVisible = state.isLoading
                nextButton.isEnabled = !state.isLoading && selectedCheckUri != null
                checkButton.isEnabled = !state.isLoading

                state.paymentCardNumber?.let { cardNumber.text = it }
                state.premiumPrice?.let { textView10.text = "$it so'm" }

                state.errorMessage?.let {
                    showToast(it)
                    viewModel.clearError()
                }
                state.successMessage?.let {
                    showToast(it)
                    setFragmentResult("payment_check_submitted", androidx.core.os.bundleOf("submitted" to true))
                    closeFragment()
                    viewModel.clearSuccess()
                }
            }

            checkButton.setOnClickListener {
                pickImageLauncher.launch("image/*")
            }

            copyButton.setOnClickListener {
                val clipboard: ClipboardManager? =
                    requireActivity().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager?
                val text = cardNumber.text.toString().replace(" ", "")
                val clip = ClipData.newPlainText("Karta raqami", text)
                clipboard?.setPrimaryClip(clip)
                showToast("Karta raqami nusxalandi")
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