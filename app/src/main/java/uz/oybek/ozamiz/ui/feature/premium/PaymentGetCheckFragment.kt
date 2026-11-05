package uz.oybek.ozamiz.ui.feature.premium

import android.content.res.ColorStateList
import android.net.Uri
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.bumptech.glide.Glide
import com.google.android.material.color.MaterialColors
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import uz.oybek.ozamiz.R
import uz.oybek.ozamiz.databinding.PaymentCheckFragmentBinding
import uz.oybek.ozamiz.ui.base.BaseFragment
import uz.oybek.ozamiz.ui.utils.NumberFormattingUtils
import uz.oybek.ozamiz.ui.utils.copyToClipboard
import uz.oybek.ozamiz.ui.utils.getArgs
import uz.oybek.ozamiz.ui.utils.registerImagePicker
import uz.oybek.ozamiz.ui.utils.showToast

@AndroidEntryPoint
class PaymentGetCheckFragment : BaseFragment<PaymentCheckFragmentBinding>() {

    private var selectedCheckUri: Uri? = null
    private var hasShownSuccessSheet = false
    private val viewModel: PaymentGetCheckViewModel by viewModels()

    private val pickImageLauncher = registerImagePicker { uri ->
        selectedCheckUri = uri
        binding.cardCheckImage.isVisible = true
        binding.checkImage.isVisible = true
        updateCheckButtonState(binding)
        Glide.with(this).load(uri).into(binding.checkImage)
        binding.nextButton.isEnabled = true
    }

    override fun viewCreated(bind: PaymentCheckFragmentBinding) {
        val navArgs = arguments.getArgs<PaymentGetCheckArgs>()
        if (navArgs != null && navArgs.price > 0L) {
            viewModel.initData(navArgs.price, navArgs.isYearly)
        }

        bind.apply {
            toolbar.setUpBackButton(this@PaymentGetCheckFragment)
            updateCheckButtonState(this)

            viewLifecycleOwner.lifecycleScope.launch {
                repeatOnLifecycle(Lifecycle.State.STARTED) {
                    launch {
                        viewModel.uiState.collect { state ->
                            progressBar.isVisible = state.isLoading || state.isSubmitting
                            nextButton.isEnabled =
                                !state.isLoading && !state.isSubmitting && selectedCheckUri != null
                            checkButton.isEnabled = !state.isLoading && !state.isSubmitting

                            cardNumber.text =
                                NumberFormattingUtils.formatCardNumber(state.paymentCardNumber)
                            textView10.text = NumberFormattingUtils.formatPrice(state.premiumPrice)
                            textView9.text = if (state.isYearly) {
                                getString(R.string.payment_check_tariff_yearly)
                            } else {
                                getString(R.string.payment_check_tariff_monthly)
                            }

                            if (state.isSuccess && !hasShownSuccessSheet) {
                                hasShownSuccessSheet = true
                                popBackStack()
                                PaymentCheckSubmittedBottomSheet().show(
                                    parentFragmentManager,
                                    "PaymentCheckSubmittedBottomSheet"
                                )
                            }
                        }
                    }

                    launch {
                        viewModel.effects.collect { effect ->
                            when (effect) {
                                is PaymentGetCheckUiEffect.PaymentCheckSubmitted -> Unit

                                is PaymentGetCheckUiEffect.ShowToastRes -> {
                                    showToast(getString(effect.messageResId))
                                }
                            }
                        }
                    }
                }
            }

            checkButton.setOnClickListener {
                pickImageLauncher.launch("image/*")
            }

            copyButton.setOnClickListener {
                val text = cardNumber.text.toString().replace(" ", "")
                copyToClipboard(
                    text,
                    getString(R.string.copy_card_number),
                    getString(R.string.card_number_copied)
                )
            }

            nextButton.setOnClickListener {
                selectedCheckUri?.let { uri ->
                    viewModel.submitPaymentCheck(uri)
                } ?: run {
                    showToast(getString(R.string.premium_payment_check_required))
                }
            }
        }
    }

    private fun updateCheckButtonState(bind: PaymentCheckFragmentBinding) {
        val context = requireContext()
        val hasImage = selectedCheckUri != null

        if (hasImage) {
            val secondaryContainerColor = MaterialColors.getColor(
                context,
                com.google.android.material.R.attr.colorSecondaryContainer,
                android.graphics.Color.LTGRAY
            )
            val onSecondaryContainerColor = MaterialColors.getColor(
                context,
                com.google.android.material.R.attr.colorOnSecondaryContainer,
                android.graphics.Color.BLACK
            )

            bind.checkButton.text = getString(R.string.payment_check_change_photo)
            bind.checkButton.backgroundTintList = ColorStateList.valueOf(secondaryContainerColor)
            bind.checkButton.setTextColor(onSecondaryContainerColor)
            bind.checkButton.iconTint = ColorStateList.valueOf(onSecondaryContainerColor)
        } else {
            val primaryColor = MaterialColors.getColor(
                context,
                androidx.appcompat.R.attr.colorPrimary,
                android.graphics.Color.BLACK
            )
            val onPrimaryColor = MaterialColors.getColor(
                context,
                com.google.android.material.R.attr.colorOnPrimary,
                android.graphics.Color.WHITE
            )

            bind.checkButton.text = getString(R.string.payment_check_attach_photo)
            bind.checkButton.backgroundTintList = ColorStateList.valueOf(primaryColor)
            bind.checkButton.setTextColor(onPrimaryColor)
            bind.checkButton.iconTint = ColorStateList.valueOf(onPrimaryColor)
        }
    }

    override fun inflateBinding(
        inflater: LayoutInflater,
        container: ViewGroup?
    ): PaymentCheckFragmentBinding {
        return PaymentCheckFragmentBinding.inflate(inflater, container, false)
    }
}
