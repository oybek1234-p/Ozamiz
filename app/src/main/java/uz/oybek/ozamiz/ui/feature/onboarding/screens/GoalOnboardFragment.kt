package uz.oybek.ozamiz.ui.feature.onboarding.screens

import android.view.LayoutInflater
import android.view.ViewGroup
import uz.oybek.ozamiz.data.profile.model.UserGoal
import uz.oybek.ozamiz.databinding.FragmentGoalOnboardBinding
import uz.oybek.ozamiz.ui.feature.onboarding.OnBoardingChildFragment

class GoalOnboardFragment : OnBoardingChildFragment<FragmentGoalOnboardBinding>() {

    override fun viewCreated(bind: FragmentGoalOnboardBinding) {
        bind.apply {
            cardLoseWeight.setOnClickListener {
                viewModel.setGoal(UserGoal.LOSE_WEIGHT)
                moveToNextScreen()
            }
            cardGainWeight.setOnClickListener {
                viewModel.setGoal(UserGoal.GAIN_WEIGHT)
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
