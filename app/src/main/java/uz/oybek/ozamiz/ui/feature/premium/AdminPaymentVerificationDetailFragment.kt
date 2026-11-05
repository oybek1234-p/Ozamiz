package uz.oybek.ozamiz.ui.feature.premium

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.bumptech.glide.Glide
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import uz.oybek.ozamiz.R
import uz.oybek.ozamiz.data.profile.model.User
import uz.oybek.ozamiz.databinding.FragmentAdminPaymentVerificationDetailBinding
import uz.oybek.ozamiz.ui.base.BaseFragment
import uz.oybek.ozamiz.ui.utils.getArgs
import uz.oybek.ozamiz.ui.utils.showToast
import java.text.SimpleDateFormat
import java.util.*

@AndroidEntryPoint
class AdminPaymentVerificationDetailFragment :
    BaseFragment<FragmentAdminPaymentVerificationDetailBinding>() {

    private val viewModel: AdminPaymentVerificationDetailViewModel by viewModels()

    override fun inflateBinding(
        inflater: LayoutInflater, container: ViewGroup?
    ): FragmentAdminPaymentVerificationDetailBinding {
        return FragmentAdminPaymentVerificationDetailBinding.inflate(inflater, container, false)
    }

    override fun viewCreated(bind: FragmentAdminPaymentVerificationDetailBinding) {
        val userId = arguments.getArgs<AdminPaymentVerificationDetailArgs>()?.userId
            ?: arguments?.getString("userId").orEmpty()
        viewModel.loadUserDetails(userId)

        bind.toolbarAdminPaymentDetail.setNavigationOnClickListener {
            popBackStack()
        }

        bind.buttonApprovePayment.setOnClickListener {
            showConfirmationDialog(true)
        }

        bind.buttonRejectPayment.setOnClickListener {
            showConfirmationDialog(false)
        }

        observeViewModel(bind)
    }

    private fun observeViewModel(bind: FragmentAdminPaymentVerificationDetailBinding) {
        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    bind.progressBarAdminPaymentDetail.isVisible = state.isLoading
                    bind.scrollViewAdminPaymentDetail.isVisible =
                        !state.isLoading && state.user != null
                    bind.textViewErrorAdminPaymentDetail.isVisible =
                        !state.isLoading && state.user == null && state.errorMessageResId != null

                    state.user?.let { displayUserDetails(bind, it) }
                    state.errorMessageResId?.let { resId ->
                        val message = getString(resId)
                        bind.textViewErrorAdminPaymentDetail.text = message
                        showToast(message)
                        viewModel.clearError()
                    }

                    bind.buttonApprovePayment.isEnabled = !state.isProcessing
                    bind.buttonRejectPayment.isEnabled = !state.isProcessing
                    if (state.isProcessing) {
                        bind.buttonApprovePayment.text = ""
                        bind.buttonRejectPayment.text = ""
                        bind.progressButtonsAdminPaymentDetail.isVisible = true
                    } else {
                        bind.buttonApprovePayment.text = getString(R.string.approve_payment)
                        bind.buttonRejectPayment.text = getString(R.string.reject_payment)
                        bind.progressButtonsAdminPaymentDetail.isVisible = false
                    }

                    if (state.verificationComplete) {
                        showToast(
                            if (state.user?.premium == true) {
                                getString(R.string.premium_payment_approved)
                            } else {
                                getString(R.string.premium_payment_rejected)
                            }
                        )
                        popBackStack()
                        viewModel.resetVerificationCompleteFlag()
                    }
                }
            }
        }
    }

    private fun displayUserDetails(
        bind: FragmentAdminPaymentVerificationDetailBinding, user: User
    ) {
        bind.toolbarAdminPaymentDetail.title =
            user.name.ifEmpty { "Foydalanuvchi ${user.uid.take(6)}" }
        bind.textViewUserNameDetailAdmin.text = "Ismi: ${user.name.ifEmpty { "-" }}"
        bind.textViewUserPhoneDetailAdmin.text = "Telefoni: ${user.phoneNumber.ifEmpty { "-" }}"
        bind.textViewUserEmailDetailAdmin.text = "Emaili: ${user.gmail.ifEmpty { "-" }}"
        val dateFormat = SimpleDateFormat("dd-MM-yyyy HH:mm", Locale.getDefault())
        bind.textViewSubmissionTimeDetailAdmin.text =
            "Yuborilgan vaqti: ${if (user.premiumDate > 0) dateFormat.format(Date(user.premiumDate)) else "Noma'lum"}"

        Glide.with(this).load(user.paymentCheckPhotoUrl).placeholder(R.drawable.placeholder_food)
            .into(bind.imageViewPaymentCheckAdmin)
    }

    private fun showConfirmationDialog(approve: Boolean) {
        val title = if (approve) {
            getString(R.string.premium_confirm_approve_title)
        } else {
            getString(R.string.premium_confirm_reject_title)
        }
        val message = if (approve) {
            getString(R.string.premium_confirm_approve_message)
        } else {
            getString(R.string.premium_confirm_reject_message)
        }
        val positiveButtonText = if (approve) {
            getString(R.string.approve_payment)
        } else {
            getString(R.string.reject_payment)
        }

        AlertDialog.Builder(requireContext())
            .setTitle(title)
            .setMessage(message)
            .setPositiveButton(positiveButtonText) { dialog, _ ->
                viewModel.processPaymentVerification(approve)
                dialog.dismiss()
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }
}