package uz.oybek.ozamiz.ui.feature.onboarding.screens

import android.view.LayoutInflater
import android.view.ViewGroup
import uz.oybek.ozamiz.R
import uz.oybek.ozamiz.databinding.FragmentCalorieOnboardBinding
import uz.oybek.ozamiz.ui.feature.onboarding.OnBoardingChildFragment

class CalorieInfoFragment : OnBoardingChildFragment<FragmentCalorieOnboardBinding>() {
    override fun viewCreated(bind: FragmentCalorieOnboardBinding) {
        bind.apply {
            if (viewModel.uiState.value.isMale) {
                photoView.setImageResource(R.drawable.male_photo)
            } else {
                photoView.setImageResource(R.drawable.female_photo)
            }
            button.setOnClickListener {
                moveToNextScreen()
            }
        }
    }

    override fun inflateBinding(
        inflater: LayoutInflater,
        container: ViewGroup?
    ): FragmentCalorieOnboardBinding {
        return FragmentCalorieOnboardBinding.inflate(
            inflater, container, false
        )
    }


}