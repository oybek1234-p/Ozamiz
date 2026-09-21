package uz.oybek.ozamiz.ui.feature.onboarding.screens

import android.view.LayoutInflater
import android.view.ViewGroup
import uz.oybek.ozamiz.databinding.FragmentGoalOnboardBinding
import uz.oybek.ozamiz.ui.feature.onboarding.OnBoardingChildFragment

class GoalOnboardFragment : OnBoardingChildFragment<FragmentGoalOnboardBinding>() {

    override fun viewCreated(bind: FragmentGoalOnboardBinding) {
        bind.apply {
            cardLoseWeight.setOnClickListener {
                viewModel.setGoal(isLoseWeight = true)
                moveToNextScreen()
            }
            cardGainWeight.setOnClickListener {
                viewModel.setGoal(isLoseWeight = false)
                moveToNextScreen()
            }
        }
    }

    override fun inflateBinding(
        inflater: LayoutInflater,
        container: ViewGroup?
    ): FragmentGoalOnboardBinding {
        return FragmentGoalOnboardBinding.inflate(
            inflater,
            container,
            false
        )
    }
}
