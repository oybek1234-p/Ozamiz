package uz.oybek.ozamiz.ui.feature.onboarding.screens.plan

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.launch
import uz.oybek.ozamiz.R
import uz.oybek.ozamiz.databinding.FragmentPlanBinding
import uz.oybek.ozamiz.ui.feature.onboarding.OnBoardingChildFragment

class PlanFragment : OnBoardingChildFragment<FragmentPlanBinding>() {

    override fun viewCreated(bind: FragmentPlanBinding) {
        val goalDuration = viewModel.uiState.value.goalDuration
        val durationText = when {
            goalDuration.isTargetReached -> getString(R.string.onboarding_goalpace_duration_reached)
            goalDuration.months > 0 && goalDuration.days > 0 -> getString(
                R.string.onboarding_goalpace_duration_months_days,
                goalDuration.months,
                goalDuration.days
            )

            goalDuration.months > 0 -> getString(
                R.string.onboarding_goalpace_duration_months,
                goalDuration.months
            )

            else -> getString(
                R.string.onboarding_goalpace_duration_days,
                goalDuration.totalDays
            )
        }
        bind.tvPlanTitle.text = getString(R.string.plan_estimated_title, durationText)

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    bind.buttonDone.isEnabled = !state.isSaving
                    bind.progressBarDone.isVisible = state.isSaving
                    bind.buttonDone.text = if (state.isSaving) "" else getString(R.string.done)
                }
            }
        }

        bind.buttonDone.setOnClickListener {
            viewModel.saveOnboardingInfo()
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