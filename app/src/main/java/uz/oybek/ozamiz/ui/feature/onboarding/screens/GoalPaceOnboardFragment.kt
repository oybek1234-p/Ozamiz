package uz.oybek.ozamiz.ui.feature.onboarding.screens

import android.content.res.ColorStateList
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.text.HtmlCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.google.android.material.card.MaterialCardView
import kotlinx.coroutines.launch
import uz.oybek.ozamiz.R
import uz.oybek.ozamiz.common.util.readableFormat
import uz.oybek.ozamiz.data.diet.model.DietPlanSpeed
import uz.oybek.ozamiz.databinding.FragmentGoalPaceOnboardBinding
import uz.oybek.ozamiz.ui.feature.onboarding.OnBoardingChildFragment
import uz.oybek.ozamiz.ui.feature.profile.goal.DietPlanTypeAdapter
import uz.oybek.ozamiz.ui.modelUtils.getFormattedDuration
import uz.oybek.ozamiz.ui.utils.appColors

class GoalPaceOnboardFragment : OnBoardingChildFragment<FragmentGoalPaceOnboardBinding>() {

    private lateinit var planTypeAdapter: DietPlanTypeAdapter

    override fun inflateBinding(
        inflater: LayoutInflater,
        container: ViewGroup?
    ): FragmentGoalPaceOnboardBinding {
        return FragmentGoalPaceOnboardBinding.inflate(inflater, container, false)
    }

    override fun viewCreated(bind: FragmentGoalPaceOnboardBinding) {
        viewModel.initDietPlan()

        planTypeAdapter = DietPlanTypeAdapter { selectedPlanType ->
            viewModel.setPlanType(selectedPlanType)
        }
        bind.recyclerDietPlanTypes.adapter = planTypeAdapter

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

            planTypeAdapter.submitList(state.availablePlanTypes)
            planTypeAdapter.setSelectedPlanType(state.selectedPlanType)

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
                iconPaceNormal,
                textPaceNormalLabel,
                textPaceNormalRate,
                isNormal
            )
            bindPaceCard(
                cardPaceFast,
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

            val durationStr = plan.duration.getFormattedDuration(requireContext())
            val weeklyRateStr = plan.weeklyRateKg.readableFormat()
            val monthlyRateStr = plan.monthlyRateKg.readableFormat()

            val deltaVal = if (!isGain && plan.tdee - plan.targetCalories > 0) {
                plan.tdee - plan.targetCalories
            } else {
                plan.deltaCalories
            }

            val summaryFormatRes = if (isGain) {
                R.string.goal_summary_gain_format
            } else {
                R.string.goal_summary_loss_format
            }

            val summaryHtml = getString(
                summaryFormatRes,
                plan.tdee,
                deltaVal,
                plan.targetCalories,
                weeklyRateStr,
                monthlyRateStr,
                durationStr
            )
            textEnergySummary.text = HtmlCompat.fromHtml(summaryHtml, HtmlCompat.FROM_HTML_MODE_COMPACT)
        }

    private fun bindPaceCard(
        card: MaterialCardView,
        icon: ImageView,
        label: TextView,
        rate: TextView,
        isSelected: Boolean
    ) {
        val colors = appColors
        if (isSelected) {
            card.setCardBackgroundColor(colors.colorSelectedCard)
            card.strokeWidth = 2.dpToPx()
            card.strokeColor = colors.colorPrimary
            card.cardElevation = 0f

            icon.imageTintList = ColorStateList.valueOf(colors.colorPrimary)
            label.setTextColor(colors.colorPrimary)
            rate.setTextColor(colors.colorPrimary)
        } else {
            card.setCardBackgroundColor(colors.colorSurfaceContainerLowest)
            card.strokeWidth = 1.dpToPx()
            card.strokeColor = colors.colorOutlineVariant
            card.cardElevation = 0f

            icon.imageTintList = ColorStateList.valueOf(colors.colorOnSurfaceVariant)
            label.setTextColor(colors.colorOnSurface)
            rate.setTextColor(colors.colorOnSurfaceVariant)
        }
    }

    private fun Int.dpToPx(): Int = (this * resources.displayMetrics.density).toInt()
}
