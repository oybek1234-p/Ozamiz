package uz.oybek.ozamiz.ui.feature.onboarding.screens

import android.view.LayoutInflater
import android.view.ViewGroup
import uz.oybek.ozamiz.R
import uz.oybek.ozamiz.databinding.FragmentAgeBinding
import uz.oybek.ozamiz.ui.feature.onboarding.OnBoardingChildFragment
import uz.oybek.ozamiz.ui.utils.showKeyboard

class AgeFragment : OnBoardingChildFragment<FragmentAgeBinding>() {
    override fun viewCreated(bind: FragmentAgeBinding) {
        bind.apply {
            button.setOnClickListener {
                val age = textInput.editText?.text.toString()
                if (age.length < 2) {
                    textInput.error = getString(R.string.age_error)
                    return@setOnClickListener
                }
                moveToNextScreen()
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