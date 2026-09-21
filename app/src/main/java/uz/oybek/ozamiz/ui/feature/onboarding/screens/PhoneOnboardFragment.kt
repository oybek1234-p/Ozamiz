package uz.oybek.ozamiz.ui.feature.onboarding.screens

import android.view.LayoutInflater
import android.view.ViewGroup
import uz.oybek.ozamiz.databinding.FragmentPhoneOnboardBinding
import uz.oybek.ozamiz.ui.feature.onboarding.OnBoardingChildFragment
import uz.oybek.ozamiz.ui.utils.showKeyboard

class PhoneOnboardFragment : OnBoardingChildFragment<FragmentPhoneOnboardBinding>() {
    override fun viewCreated(bind: FragmentPhoneOnboardBinding) {
        val currentPhone = viewModel.uiState.value.user.phoneNumber
        if (!currentPhone.isNullOrEmpty()) {
            bind.editTextPhone.setText(currentPhone)
            bind.editTextPhone.setSelection(currentPhone.length)
        }

        bind.apply {
            buttonSave.setOnClickListener {
                val inputPhone = editTextPhone.text.toString().trim()
                if (inputPhone.isNotEmpty()) {
                    viewModel.setPhoneNumber(inputPhone)
                }
                moveToNextScreen()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        binding.editTextPhone.showKeyboard()
    }

    override fun inflateBinding(
        inflater: LayoutInflater,
        container: ViewGroup?
    ): FragmentPhoneOnboardBinding {
        return FragmentPhoneOnboardBinding.inflate(
            inflater,
            container,
            false
        )
    }
}
