package uz.oybek.ozamiz.ui.feature.onboarding.screens

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.google.android.material.card.MaterialCardView
import kotlinx.coroutines.launch
import uz.oybek.ozamiz.R
import uz.oybek.ozamiz.data.diet.model.DietPlanSpeed
import uz.oybek.ozamiz.databinding.FragmentGoalPaceOnboardBinding
import uz.oybek.ozamiz.ui.feature.onboarding.OnBoardingChildFragment
import uz.oybek.ozamiz.ui.utils.appColors
import java.util.Locale

class GoalPaceOnboardFragment : OnBoardingChildFragment<FragmentGoalPaceOnboardBinding>() {

    override fun inflateBinding(
        inflater: LayoutInflater,
        container: ViewGroup?
    ): FragmentGoalPaceOnboardBinding {
        return FragmentGoalPaceOnboardBinding.inflate(inflater, container, false)
    }

    override fun viewCreated(bind: FragmentGoalPaceOnboardBinding) {
        viewModel.initDietPlan()

        bind.cardPaceNormal.setOnClickListener {
            viewModel.setPlanSpeed(DietPlanSpeed.NORMAL)
        }

        bind.cardPaceFast.setOnClickListener {
            viewModel.setPlanSpeed(DietPlanSpeed.FAST)
        }

        bind.buttonContinue.setOnClickListener {
            moveToNextScreen()
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    renderUI(bind, state.dietPlanState)
                }
            }
        }
    }

    private fun renderUI(bind: FragmentGoalPaceOnboardBinding, state: DietPlanUiState) =
        with(bind) {
            val plan = state.selectedPlan ?: return@with
            val isNormal = state.selectedSpeed == DietPlanSpeed.NORMAL
            val isGain = plan.goal.isGain

            textSpeedTitle.setText(
                if (isGain) R.string.onboarding_goalpace_title_gain
                else R.string.onboarding_goalpace_title_loss
            )

            val rateRes = if (isGain) R.string.onboarding_goalpace_rate_format_gain
            else R.string.onboarding_goalpace_rate_format_loss

            state.normalPlan?.let {
                textPaceNormalRate.text = getString(rateRes, it.monthlyRateKg)
            }
            state.fastPlan?.let {
                textPaceFastRate.text = getString(rateRes, it.monthlyRateKg)
            }

            // Cards UI selection
            bindPaceCard(
                cardPaceNormal,
                framePaceNormalIcon,
                iconPaceNormal,
                textPaceNormalLabel,
                textPaceNormalRate,
                isNormal
            )
            bindPaceCard(
                cardPaceFast,
                framePaceFastIcon,
                iconPaceFast,
                textPaceFastLabel,
                textPaceFastRate,
                !isNormal
            )

            // Inputs
            editTargetCalories.setText(plan.targetCalories.toString())
            editTargetProtein.setText(plan.macros.protein.toString())
            editTargetCarbs.setText(plan.macros.carbs.toString())
            editTargetFat.setText(plan.macros.fat.toString())

            // Duration string
            val duration = plan.duration
            val durationStr = when {
                duration.isTargetReached -> getString(R.string.onboarding_goalpace_duration_reached)
                duration.months > 0 && duration.days > 0 ->
                    getString(
                        R.string.onboarding_goalpace_duration_months_days,
                        duration.months,
                        duration.days
                    )

                duration.months > 0 ->
                    getString(R.string.onboarding_goalpace_duration_months, duration.months)

                else ->
                    getString(
                        R.string.onboarding_goalpace_duration_days,
                        duration.totalDays
                    )
            }

            val weeklyStr = String.format(Locale.US, "%.1f", plan.weeklyRateKg)
            val monthlyStr = String.format(Locale.US, "%.1f", plan.monthlyRateKg)

            textEnergySummary.text = if (isGain) {
                getString(
                    R.string.onboarding_goalpace_summary_gain,
                    plan.tdee,
                    plan.deltaCalories,
                    weeklyStr,
                    monthlyStr,
                    durationStr
                )
            } else {
                val defVal = if (plan.tdee - plan.targetCalories > 0) {
                    plan.tdee - plan.targetCalories
                } else {
                    plan.deltaCalories
                }
                getString(
                    R.string.onboarding_goalpace_summary_loss,
                    plan.tdee,
                    defVal,
                    weeklyStr,
                    monthlyStr,
                    durationStr
                )
            }
        }

    private fun bindPaceCard(
        card: MaterialCardView,
        frameIcon: View,
        icon: ImageView,
        label: TextView,
        rate: TextView,
        isSelected: Boolean
    ) {
        val colors = appColors
        val bg =
            if (isSelected) colors.colorSurfaceContainerHigh else colors.colorSurfaceContainerLowest
        val stroke = if (isSelected) colors.colorPrimary else colors.colorOutlineVariant
        val textColor = if (isSelected) colors.colorOnSurface else colors.colorOnSurfaceVariant
        val iconBg =
            if (isSelected) colors.colorSurfaceContainerHighest else colors.colorSurfaceContainerLowest

        card.setCardBackgroundColor(colors.colorStateList(bg))
        card.strokeWidth = if (isSelected) 2.dpToPx() else 1.dpToPx()
        card.strokeColor = stroke
        card.cardElevation = if (isSelected) 2.dpToPx().toFloat() else 0f
        frameIcon.backgroundTintList = colors.colorStateList(iconBg)
        icon.imageTintList = colors.colorStateList(textColor)
        label.setTextColor(textColor)
        rate.setTextColor(textColor)
    }

    private fun Int.dpToPx(): Int = (this * resources.displayMetrics.density).toInt()
}
