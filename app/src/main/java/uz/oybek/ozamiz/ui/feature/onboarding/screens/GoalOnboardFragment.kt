package uz.oybek.ozamiz.ui.feature.onboarding.screens

import android.view.LayoutInflater
import android.view.ViewGroup
import uz.oybek.ozamiz.data.user.local.LocalUser
import uz.oybek.ozamiz.databinding.FragmentGoalOnboardBinding
import uz.oybek.ozamiz.ui.feature.onboarding.OnBoardingChildFragment

class GoalOnboardFragment : OnBoardingChildFragment<FragmentGoalOnboardBinding>() {

    override fun viewCreated(bind: FragmentGoalOnboardBinding) {
        bind.apply {
            cardLoseWeight.setOnClickListener {
                selected("lose_weight")
            }
            cardGainWeight.setOnClickListener {
                selected("gain_weight")
            }
        }
    }

    private fun selected(goal: String) {
        LocalUser.update {
            this.goal = goal
        }
        moveToNextScreen()
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
