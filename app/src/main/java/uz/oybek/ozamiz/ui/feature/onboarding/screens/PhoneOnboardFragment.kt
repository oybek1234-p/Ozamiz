package uz.oybek.ozamiz.ui.feature.onboarding.screens

import android.view.LayoutInflater
import android.view.ViewGroup
import uz.oybek.ozamiz.data.user.local.LocalUser
import uz.oybek.ozamiz.databinding.FragmentPhoneOnboardBinding
import uz.oybek.ozamiz.ui.feature.onboarding.OnBoardingChildFragment
import uz.oybek.ozamiz.ui.utils.showKeyboard

class PhoneOnboardFragment : OnBoardingChildFragment<FragmentPhoneOnboardBinding>() {
    override fun viewCreated(bind: FragmentPhoneOnboardBinding) {
        bind.apply {
            buttonSave.setOnClickListener {
                val inputPhone = editTextPhone.text.toString().trim()
                if (inputPhone.isNotEmpty()) {
                    LocalUser.update { this.phoneNumber = inputPhone }
                }
                moveToNextScreen()
            }
            buttonSkip.setOnClickListener {
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
