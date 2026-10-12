package uz.oybek.ozamiz.ui.feature.onboarding.screens

import android.view.LayoutInflater
import android.view.ViewGroup
import uz.oybek.ozamiz.R
import uz.oybek.ozamiz.data.profile.model.UserMetricsLimits
import uz.oybek.ozamiz.databinding.FragmentAgeBinding
import uz.oybek.ozamiz.ui.feature.onboarding.OnBoardingChildFragment
import uz.oybek.ozamiz.ui.utils.intOrNull
import uz.oybek.ozamiz.ui.utils.showKeyboard

class AgeFragment : OnBoardingChildFragment<FragmentAgeBinding>() {
    override fun viewCreated(bind: FragmentAgeBinding) {
        val currentAge = viewModel.uiState.value.user.age
        if (currentAge > 0) {
            val ageStr = currentAge.toString()
            bind.textInput.editText?.setText(ageStr)
            bind.textInput.editText?.setSelection(ageStr.length)
        }

        bind.apply {
            button.setOnClickListener {
                val age = textInput.intOrNull()
                if (UserMetricsLimits.isValidAge(age)) {
                    viewModel.setAge(age!!)
                    moveToNextScreen()
                } else {
                    textInput.error = getString(R.string.error_invalid_age)
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
    ): FragmentAgeBinding {
        return FragmentAgeBinding.inflate(
            inflater,
            container,
            false
        )
    }
}