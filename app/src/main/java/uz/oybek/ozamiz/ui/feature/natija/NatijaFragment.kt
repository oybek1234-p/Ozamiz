package uz.oybek.ozamiz.ui.feature.natija

import android.graphics.Typeface
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.style.StyleSpan
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import dagger.hilt.android.AndroidEntryPoint
import uz.oybek.ozamiz.R
import uz.oybek.ozamiz.common.util.DateFormatter
import uz.oybek.ozamiz.common.util.orZero
import uz.oybek.ozamiz.common.util.readableFormat
import uz.oybek.ozamiz.common.util.round
import uz.oybek.ozamiz.databinding.FragmentNatijaBinding
import uz.oybek.ozamiz.ui.base.BaseFragment
import uz.oybek.ozamiz.ui.modelUtils.NutritionFormatter
import uz.oybek.ozamiz.ui.utils.collectFlowOnStart
import uz.oybek.ozamiz.ui.utils.loadPhoto
import uz.oybek.ozamiz.ui.utils.showFullScreenImage
import kotlin.math.abs

@AndroidEntryPoint
class NatijaFragment : BaseFragment<FragmentNatijaBinding>() {

    private val viewModel: NatijaViewModel by viewModels()
    private var isContentAnimatedIn = false

    init {
        showBottomSheet = true
    }

    override fun inflateBinding(
        inflater: LayoutInflater, container: ViewGroup?
    ): FragmentNatijaBinding {
        return FragmentNatijaBinding.inflate(inflater, container, false)
    }

    override fun viewCreated(bind: FragmentNatijaBinding) {
        setupClickListeners(bind)
        observeState(bind)

        bind.swipeRefreshNatija.setOnRefreshListener {
            viewModel.loadData(isRefresh = true)
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.loadData(isRefresh = false)
    }

    private fun setupClickListeners(bind: FragmentNatijaBinding) {
        bind.btnAddWeight.setOnClickListener {
            navigate(R.id.addWeightPhotoFragment)
        }
        bind.cardMonthlyChanges.setOnClickListener {
            navigate(R.id.weightHistoryFragment)
        }
        bind.tvWeightHistory.setOnClickListener {
            navigate(R.id.weightHistoryFragment)
        }
    }

    private fun observeState(bind: FragmentNatijaBinding) {
        collectFlowOnStart(viewModel.uiState) { state ->
            val isLoading = state.isLoading
            val summary = state.weightSummary
            val currentW = state.currentWeight ?: 70.0

            val isInitialLoading = summary == null
            if (isInitialLoading) {
                bind.layoutLoadingNatija.isVisible = true
                bind.scrollViewContentNatija.isVisible = false
                bind.swipeRefreshNatija.isRefreshing = false
                return@collectFlowOnStart
            }

            bind.swipeRefreshNatija.isRefreshing = isLoading
            if (bind.layoutLoadingNatija.isVisible) {
                bind.layoutLoadingNatija.animate()
                    .alpha(0f)
                    .setDuration(200)
                    .withEndAction { bind.layoutLoadingNatija.isVisible = false }
            }

            if (!bind.scrollViewContentNatija.isVisible || !isContentAnimatedIn) {
                isContentAnimatedIn = true
                bind.scrollViewContentNatija.alpha = 0f
                bind.scrollViewContentNatija.isVisible = true
                bind.scrollViewContentNatija.animate()
                    .alpha(1f)
                    .setDuration(300)
                    .start()

                val cards = listOf(
                    bind.cardGoalHeader,
                    bind.btnAddWeight,
                    bind.cardForecast,
                    bind.cardMonthlyChanges,
                    bind.cardPhotos,
                    bind.cardIdealWeight
                )
                cards.forEachIndexed { index, view ->
                    view.translationY = 30f
                    view.alpha = 0f
                    view.animate()
                        .translationY(0f)
                        .alpha(1f)
                        .setStartDelay((index * 40).toLong())
                        .setDuration(280)
                        .start()
                }
            }

            // 1. Predictions & Goal Forecast (Goal reach date, weekly & monthly predictions)
            bind.layoutForecastPredictions.isVisible = true
            bind.layoutForecastAdvisory.isVisible = false

            // Item 1: Goal Target Reach Date
            val targetTimestamp = summary.prediction.targetTimestamp
            if (targetTimestamp != null && targetTimestamp > 0) {
                val targetDateStr = DateFormatter.formatTargetDate(targetTimestamp)
                val daysLeft = DateFormatter.daysBetween(System.currentTimeMillis(), targetTimestamp).coerceAtLeast(1)
                val months = daysLeft / 30
                val days = daysLeft % 30
                val remainingStr = when {
                    months > 0 && days > 0 -> getString(R.string.natija_remaining_months_days_format, months, days)
                    months > 0 -> getString(R.string.natija_remaining_months_format, months)
                    else -> getString(R.string.natija_remaining_days_format, daysLeft)
                }
                val rawText = getString(
                    R.string.natija_target_reach_line_format,
                    targetDateStr,
                    remainingStr
                )
                bind.tvGoalTargetDate.text = formatSpannableLine(rawText, targetDateStr, remainingStr)
                bind.layoutGoalTargetDate.isVisible = true
            } else if ((summary.weight.remainingValue ?: 1.0) <= 0.05) {
                bind.tvGoalTargetDate.text = getString(R.string.natija_goal_reached)
                bind.layoutGoalTargetDate.isVisible = true
            } else {
                bind.layoutGoalTargetDate.isVisible = false
            }

            // Item 2: Weekly Prediction ("Bu hafta taxmin -0.6 kg")
            val weeklyKg = summary.prediction.weeklyLossKg
            val weeklyAbsKg = abs(weeklyKg)
            val weeklyChangeStr = when {
                weeklyKg < -0.01 -> getString(R.string.natija_predicted_decrease_format, weeklyAbsKg.readableFormat())
                weeklyKg > 0.01 -> getString(R.string.natija_predicted_increase_format, weeklyAbsKg.readableFormat())
                else -> getString(R.string.natija_predicted_no_change)
            }
            val rawWeeklyText = getString(R.string.natija_weekly_line_format, weeklyChangeStr)
            bind.tvWeeklyPrediction.text = formatSpannableLine(rawWeeklyText, weeklyChangeStr)
            bind.layoutWeeklyPrediction.isVisible = true

            // Item 3: Monthly Prediction ("1 oyda taxmin -2.5 kg")
            val monthlyKg = summary.prediction.monthlyLossKg
            val monthlyAbsKg = abs(monthlyKg)
            val monthlyChangeStr = when {
                monthlyKg < -0.01 -> getString(R.string.natija_predicted_decrease_format, monthlyAbsKg.readableFormat())
                monthlyKg > 0.01 -> getString(R.string.natija_predicted_increase_format, monthlyAbsKg.readableFormat())
                else -> getString(R.string.natija_predicted_no_change)
            }
            val rawMonthlyText = getString(R.string.natija_monthly_line_format, monthlyChangeStr)
            bind.tvMonthlyPrediction.text = formatSpannableLine(rawMonthlyText, monthlyChangeStr)
            bind.layoutMonthlyPrediction.isVisible = true

            val footnoteRes = if (summary.prediction.isBasedOnActualFoodLogs) {
                R.string.natija_forecast_footnote
            } else {
                R.string.natija_forecast_footnote_planned
            }
            bind.tvForecastFootnote.text = getString(footnoteRes)

            // 2. Metrics (Vazn, Qorin, Bo'ksa, Qo'l) - Format using ONLY NutritionFormatter
            val weightCurrent = summary.weight.currentValue ?: currentW
            val weightChange = summary.weight.changeValue ?: summary.totalWeightChangeKg
            bind.tvMetricVaznVal.text = NutritionFormatter.kg(weightCurrent)
            bind.tvMetricVaznSubVal.text = if (abs(weightChange) >= 0.05) NutritionFormatter.kgChange(weightChange) else ""

            val bellyCurrent = summary.belly.currentValue
            val bellyChange = summary.belly.changeValue
            bind.tvMetricQorinVal.text = if (bellyCurrent != null && bellyCurrent > 0) NutritionFormatter.cm(bellyCurrent) else getString(R.string.change_no_change_sm)
            bind.tvMetricQorinSubVal.text = if (bellyChange != null && abs(bellyChange) >= 0.05) NutritionFormatter.cmChange(bellyChange) else ""

            val hipsCurrent = summary.hips.currentValue
            val hipsChange = summary.hips.changeValue
            bind.tvMetricSonVal.text = if (hipsCurrent != null && hipsCurrent > 0) NutritionFormatter.cm(hipsCurrent) else getString(R.string.change_no_change_sm)
            bind.tvMetricSonSubVal.text = if (hipsChange != null && abs(hipsChange) >= 0.05) NutritionFormatter.cmChange(hipsChange) else ""

            val armCurrent = summary.arm.currentValue
            val armChange = summary.arm.changeValue
            bind.tvMetricQolVal.text = if (armCurrent != null && armCurrent > 0) NutritionFormatter.cm(armCurrent) else getString(R.string.change_no_change_sm)
            bind.tvMetricQolSubVal.text = if (armChange != null && abs(armChange) >= 0.05) NutritionFormatter.cmChange(armChange) else ""

            updateStats(bind, state)

            val hasAnyPhoto = state.photoBefore != null || state.photoAfter != null
            bind.cardPhotos.isVisible = hasAnyPhoto
            if (hasAnyPhoto) {
                val hasBefore = state.photoBefore != null
                val hasAfter = state.photoAfter != null

                if (hasBefore) {
                    bind.layoutPhotoBefore.isVisible = true
                    bind.tvPhotoBeforeLabel.text = getString(R.string.community_photo_oldin)

                    bind.imgPhotoBefore.loadPhoto(state.photoBefore)
                    bind.imgPhotoBefore.setOnClickListener {
                        showFullScreenImage(state.photoBefore)
                    }
                } else {
                    bind.layoutPhotoBefore.isVisible = false
                }

                if (hasAfter) {
                    bind.layoutPhotoAfter.isVisible = true
                    bind.tvPhotoAfterLabel.text = getString(R.string.community_photo_hozir)

                    bind.imgPhotoAfter.loadPhoto(state.photoAfter)
                    bind.imgPhotoAfter.setOnClickListener {
                        showFullScreenImage(state.photoAfter)
                    }
                } else {
                    bind.layoutPhotoAfter.isVisible = false
                }
            }
        }
    }
}

private fun formatSpannableLine(
    fullText: String,
    boldTarget1: String,
    boldTarget2: String? = null
): CharSequence {
    if (fullText.isEmpty()) return ""
    val spannable = SpannableStringBuilder(fullText)

    if (boldTarget1.isNotEmpty()) {
        val start1 = fullText.indexOf(boldTarget1)
        if (start1 >= 0) {
            spannable.setSpan(
                StyleSpan(Typeface.BOLD),
                start1,
                start1 + boldTarget1.length,
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
            )
        }
    }

    if (!boldTarget2.isNullOrEmpty()) {
        val start2 = fullText.indexOf(boldTarget2)
        if (start2 >= 0) {
            spannable.setSpan(
                StyleSpan(Typeface.BOLD),
                start2,
                start2 + boldTarget2.length,
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
            )
        }
    }

    return spannable
}

private fun updateStats(
    bind: FragmentNatijaBinding,
    state: NatijaUiState
) {
    val summary = state.weightSummary
    val currentW = state.currentWeight ?: 70.0
    val startW = state.startWeight ?: currentW
    val goalW = state.goalWeight.orZero()

    val unitSm = bind.root.context.getString(R.string.unit_sm_short)

    // 1. Total Weight & Measurement Changes
    val totalChange = summary?.totalWeightChangeKg ?: (currentW - startW)
    val totalMeasurement = summary?.totalMeasurementChangeCm ?: 0.0

    val hasWeightChange = abs(totalChange) >= 0.05
    val hasMeasurementChange = abs(totalMeasurement) >= 0.05

    bind.layoutWeightStat.isVisible = hasWeightChange
    if (hasWeightChange) {
        val formattedTotalChange = if (totalChange > 0) {
            "+${totalChange.readableFormat()}"
        } else {
            totalChange.readableFormat()
        }
        bind.tvTotalWeightVal.text = formattedTotalChange

        val totalWeightUnitText = when {
            totalChange < -0.01 -> bind.root.context.getString(R.string.natija_total_lost_unit_format)
            totalChange > 0.01 -> bind.root.context.getString(R.string.natija_total_gained_unit_format)
            else -> bind.root.context.getString(R.string.unit_kg_short)
        }
        bind.tvTotalWeightUnit.text = totalWeightUnitText
    }

    bind.layoutMeasurementStat.isVisible = hasMeasurementChange
    if (hasMeasurementChange) {
        val formattedMeasurement = if (totalMeasurement > 0) {
            "+${totalMeasurement.round().toInt()}"
        } else {
            totalMeasurement.round().toInt().toString()
        }
        bind.tvTotalMeasurementVal.text = formattedMeasurement
        bind.tvTotalMeasurementUnit.text = unitSm
    }

    bind.dividerTotalStats.isVisible = hasWeightChange && hasMeasurementChange
    val hasAnyStat = hasWeightChange || hasMeasurementChange
    bind.layoutTotalChangeStatsRow.isVisible = hasAnyStat
    bind.tvTotalChangeLabel.isVisible = hasAnyStat

    // 2. Start, Current, Goal Weight
    bind.tvStartWeightVal.text = NutritionFormatter.kg(startW)
    bind.tvCurrentWeightVal.text = NutritionFormatter.kg(currentW)
    bind.tvGoalWeightVal.text = NutritionFormatter.kg(goalW)

    updateIdealWeight(bind, state)
}

private fun updateIdealWeight(bind: FragmentNatijaBinding, state: NatijaUiState) {
    val summary = state.weightSummary
    val minKg = summary?.idealWeightMin ?: 0
    val maxKg = summary?.idealWeightMax ?: 0
    val heightCm = summary?.userHeightCm ?: 0

    if (minKg <= 0 || maxKg <= 0) {
        bind.cardIdealWeight.isVisible = false
        return
    }

    bind.cardIdealWeight.isVisible = true
    bind.tvIdealWeightRange.text = NutritionFormatter.kgRange(minKg.toDouble(), maxKg.toDouble())
    bind.tvIdealWeightSubtitle.text = if (heightCm > 0) {
        bind.root.context.getString(R.string.natija_ideal_weight_subtitle_with_height, heightCm)
    } else {
        bind.root.context.getString(R.string.natija_ideal_weight_subtitle_default)
    }
}
