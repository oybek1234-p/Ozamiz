package uz.oybek.ozamiz.ui.feature.onboarding.screens.plan

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import uz.oybek.ozamiz.R
import uz.oybek.ozamiz.data.food.DietController
import uz.oybek.ozamiz.databinding.FragmentPlanBinding
import uz.oybek.ozamiz.ui.feature.onboarding.OnBoardingChildFragment
import uz.oybek.ozamiz.ui.utils.showToast

class PlanFragment : OnBoardingChildFragment<FragmentPlanBinding>() {

    override fun viewCreated(bind: FragmentPlanBinding) {
        val durationText = DietController.getGoalDurationText()
        bind.tvPlanTitle.text = getString(R.string.plan_estimated_title, durationText)

        viewModel.isLoading.observe(viewLifecycleOwner) { loading ->
            bind.buttonLoveIt.isEnabled = !loading
            bind.progressBarLoveIt.isVisible = loading
            bind.buttonLoveIt.text = if (loading) "" else getString(R.string.love_it)
        }

        bind.buttonLoveIt.setOnClickListener {
            viewModel.completeOnboarding(
                onSuccess = {
                    mainActivity()?.recreateUi()
                },
                onError = {
                    showToast(getString(R.string.food_add_error))
                }
            )
        }
    }

    override fun inflateBinding(
        inflater: LayoutInflater, container: ViewGroup?
    ): FragmentPlanBinding {
        return FragmentPlanBinding.inflate(
            inflater, container, false
        )
    }
}