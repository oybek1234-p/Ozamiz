package uz.oybek.ozamiz.ui.feature.onboarding.screens

import android.view.LayoutInflater
import android.view.ViewGroup
import uz.oybek.ozamiz.databinding.FragmentGenderBinding
import uz.oybek.ozamiz.ui.feature.onboarding.OnBoardingChildFragment

class GenderFragment : OnBoardingChildFragment<FragmentGenderBinding>() {

    override fun viewCreated(bind: FragmentGenderBinding) {
        bind.apply {
            maleCard.setOnClickListener {
                viewModel.setGender(isMale = true)
                moveToNextScreen()
            }
            femaleCard.setOnClickListener {
                viewModel.setGender(isMale = false)
                moveToNextScreen()
            }
        }
    }

    override fun inflateBinding(
        inflater: LayoutInflater,
        container: ViewGroup?
    ): FragmentGenderBinding {
        return FragmentGenderBinding.inflate(
            inflater,
            container,
            false
        )
    }
}