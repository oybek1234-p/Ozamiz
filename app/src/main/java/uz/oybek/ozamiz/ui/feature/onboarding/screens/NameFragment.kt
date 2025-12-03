package uz.oybek.ozamiz.ui.feature.onboarding.screens

import android.view.LayoutInflater
import android.view.ViewGroup
import uz.oybek.ozamiz.R
import uz.oybek.ozamiz.data.user.local.LocalUser
import uz.oybek.ozamiz.databinding.FragmentNameBinding
import uz.oybek.ozamiz.ui.feature.onboarding.OnBoardingChildFragment
import uz.oybek.ozamiz.ui.utils.showKeyboard

class NameFragment
    : OnBoardingChildFragment<FragmentNameBinding>() {
    override fun viewCreated(bind: FragmentNameBinding) {
        bind.apply {
            button.setOnClickListener {
                val inputName =
                    textInput.editText?.text.toString().trim()
                if (inputName.isBlank()) {
                    textInput.error = getString(R.string.name_error)
                    return@setOnClickListener
                } else {
                    LocalUser.update { this.name = inputName }
                    moveToNextScreen()
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        binding.textInput.editText.showKeyboard()
    }

    override fun inflateBinding(
        inflater: LayoutInflater,
        container: ViewGroup?
    ): FragmentNameBinding {
        return FragmentNameBinding.inflate(
            inflater,
            container,
            false
        )
    }
}