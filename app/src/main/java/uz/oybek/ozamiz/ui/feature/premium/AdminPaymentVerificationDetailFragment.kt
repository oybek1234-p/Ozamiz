package uz.oybek.ozamiz.ui.feature.premium

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.bumptech.glide.Glide
import kotlinx.coroutines.launch
import uz.oybek.ozamiz.R
import uz.oybek.ozamiz.data.user.model.User
import uz.oybek.ozamiz.databinding.FragmentAdminPaymentVerificationDetailBinding
import uz.oybek.ozamiz.ui.base.BaseFragment
import uz.oybek.ozamiz.ui.utils.showToast
import java.text.SimpleDateFormat
import java.util.*

class AdminPaymentVerificationDetailFragment :
    BaseFragment<FragmentAdminPaymentVerificationDetailBinding>() {

    private val viewModel: AdminPaymentVerificationDetailViewModel by viewModels()
    private val args: AdminPaymentVerificationDetailFragmentArgs by navArgs()

    override fun inflateBinding(
        inflater: LayoutInflater, container: ViewGroup?
    ): FragmentAdminPaymentVerificationDetailBinding {
        return FragmentAdminPaymentVerificationDetailBinding.inflate(inflater, container, false)
    }
    
    override fun viewCreated(bind: FragmentAdminPaymentVerificationDetailBinding) {
        viewModel.loadUserDetails(args.userId)

        bind.toolbarAdminPaymentDetail.setNavigationOnClickListener {
            findNavController().popBackStack()
        }

        bind.imageViewPaymentCheckAdmin.setOnClickListener {
            viewModel.uiState.value.user?.paymentCheckPhotoUrl?.let { url ->
                if (url.isNotEmpty()) {
                //    bind.imageViewPaymentCheckAdmin.openImageViewer(listOf(url))
                }
            }
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
                        !state.isLoading && state.user == null && state.errorMessage != null

                    state.user?.let { displayUserDetails(bind, it) }
                    state.errorMessage?.let {
                        bind.textViewErrorAdminPaymentDetail.text = it
                        showToast(it)
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
                        showToast(if (state.user?.premium == true) "To'lov tasdiqlandi" else "To'lov rad etildi")
                        findNavController().popBackStack()
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
        val title = if (approve) "To'lovni Tasdiqlash" else "To'lovni Rad Etish"
        val message =
            if (approve) "Haqiqatan ham bu foydalanuvchining to'lovini tasdiqlamoqchimisiz?"
            else "Haqiqatan ham bu foydalanuvchining to'lovini rad etmoqchimisiz? Bu amalni ortga qaytarib bo'lmaydi."
        val positiveButtonText = if (approve) "Tasdiqlash" else "Rad Etish"

        AlertDialog.Builder(requireContext()).setTitle(title).setMessage(message)
            .setPositiveButton(positiveButtonText) { dialog, _ ->
                viewModel.processPaymentVerification(approve)
                dialog.dismiss()
            }.setNegativeButton(R.string.cancel, null).show()
    }

}