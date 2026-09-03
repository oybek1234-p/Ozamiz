package uz.oybek.ozamiz.ui.feature.natija

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.bumptech.glide.Glide
import kotlinx.coroutines.launch
import uz.oybek.ozamiz.R
import uz.oybek.ozamiz.databinding.FragmentNatijaBinding
import uz.oybek.ozamiz.ui.base.BaseFragment
import java.util.Locale

class NatijaFragment : BaseFragment<FragmentNatijaBinding>() {

    private val viewModel: NatijaViewModel by viewModels()

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
            viewModel.loadData()
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.loadData()
    }

    private fun setupClickListeners(bind: FragmentNatijaBinding) {
        bind.btnAddWeight.setOnClickListener {
            navigate(R.id.addWeightPhotoFragment)
        }
        bind.btnWeightHistory.setOnClickListener {
            navigate(R.id.action_natijaFragment_to_weightHistoryFragment)
        }
        val openEditGoal = View.OnClickListener {
            val sheet = EditWeightGoalsBottomSheet.newInstance()
            sheet.onSavedListener = {
                viewModel.loadData()
            }
            sheet.show(childFragmentManager, EditWeightGoalsBottomSheet.TAG)
        }
        bind.btnEditGoalWeight.setOnClickListener(openEditGoal)
        bind.layoutGoalWeight.setOnClickListener(openEditGoal)
    }

    private fun observeState(bind: FragmentNatijaBinding) {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    bind.swipeRefreshNatija.isRefreshing = state.isLoading
                    bind.cardAddWeight.isVisible = true

                    // Prediction Summary with Target Completion Duration
                    if (state.isPredictionLoading) {
                        bind.tvPredictionHighlight.text = "Hisoblanmoqda..."
                        bind.tvPredictionSummary.text = "Kunlik reja tahlil qilinmoqda"
                    } else {
                        val rawWeekly = state.weeklyPredictedLoss.replace("-", "").replace("+", "").trim()
                        val rawMonthly = state.monthlyPredictedLoss.replace("-", "").replace("+", "").trim()
                        val rawDuration = state.goalDurationText.replace("qoldi", "").trim()

                        val weeklyText = if (rawWeekly.isNotEmpty() && rawWeekly != "...") rawWeekly else "0.5 kg"
                        val monthlyText = if (rawMonthly.isNotEmpty() && rawMonthly != "...") rawMonthly else "2.0 kg"
                        val currentW = state.currentWeight
                        val goalW = state.goalWeight ?: 0.0
                        val isGoalReached = currentW != null && goalW > 0 && if (state.isGainGoal) currentW >= goalW else currentW <= goalW

                        if (isGoalReached) {
                            bind.tvPredictionHighlight.text = "Maqsadga erishildi"
                            bind.tvPredictionSummary.text = "Tabriklaymiz! Siz belgilangan maqsadli vazningizga muvaffaqiyatli erishdingiz 🎉"
                        } else {
                            val durationText = if (rawDuration.isNotEmpty() && rawDuration != "Hisoblanmoqda...") rawDuration else "1 oy 16 kun"
                            val formattedDuration = if (durationText.endsWith("da")) durationText else "${durationText}da"

                            bind.tvPredictionHighlight.text = formattedDuration

                            val summaryText = if (state.isGainGoal) {
                                "To'g'ri ovqatlansangiz har hafta $weeklyText, oyiga $monthlyText semirasiz."
                            } else {
                                "To'g'ri ovqatlansangiz har hafta $weeklyText, oyiga $monthlyText ozasiz."
                            }
                            bind.tvPredictionSummary.text = summaryText
                        }
                    }

                    // Photos
                    val hasAnyPhoto = state.photoBefore != null || state.photoAfter != null
                    bind.cardPhotos.isVisible = hasAnyPhoto
                    if (hasAnyPhoto) {
                        val hasBefore = state.photoBefore != null
                        val hasAfter = state.photoAfter != null
                        val onlyOnePhoto = hasBefore xor hasAfter

                        if (hasBefore) {
                            bind.layoutPhotoBefore.isVisible = true
                            bind.tvPhotoBeforeLabel.text = "Oldin"

                            Glide.with(this@NatijaFragment)
                                .load(state.photoBefore)
                                .placeholder(R.color.grey_line)
                                .into(bind.imgPhotoBefore)

                            bind.imgPhotoBefore.setOnClickListener {
                                state.photoBefore?.let { url ->
                                    uz.oybek.ozamiz.ui.common.dialog.FullScreenImageDialog.newInstance(url)
                                        .show(parentFragmentManager, "full_image")
                                }
                            }
                        } else {
                            bind.layoutPhotoBefore.isVisible = false
                        }

                        if (hasAfter) {
                            bind.layoutPhotoAfter.isVisible = true
                            bind.tvPhotoAfterLabel.text = "Hozir"

                            Glide.with(this@NatijaFragment)
                                .load(state.photoAfter)
                                .placeholder(R.color.grey_line)
                                .into(bind.imgPhotoAfter)

                            bind.imgPhotoAfter.setOnClickListener {
                                state.photoAfter?.let { url ->
                                    uz.oybek.ozamiz.ui.common.dialog.FullScreenImageDialog.newInstance(url)
                                        .show(parentFragmentManager, "full_image")
                                }
                            }
                        } else {
                            bind.layoutPhotoAfter.isVisible = false
                        }

                        bind.spacerPhotoBalance.isVisible = onlyOnePhoto
                    }

                    updateStats(bind, state)
                }
            }
        }
    }

    private fun formatWeightValue(value: Double): String {
        return if (value % 1.0 == 0.0) {
            value.toInt().toString()
        } else {
            String.format(Locale.US, "%.1f", value)
        }
    }

    private fun updateStats(
        bind: FragmentNatijaBinding,
        state: NatijaUiState
    ) {
        val currentW = state.currentWeight
        val startW = state.startWeight ?: currentW
        val goalW = state.goalWeight ?: 0.0

        // 1. Weight Metrics (Boshlang'ich -> Hozirgi -> Maqsad)
        bind.tvStartWeight.text =
            if (startW != null && startW > 0) "${formatWeightValue(startW)} kg" else "-- kg"
        bind.tvCurrentWeight.text =
            if (currentW != null && currentW > 0) "${formatWeightValue(currentW)} kg" else "-- kg"
        bind.tvGoalWeight.text =
            if (goalW > 0) "${formatWeightValue(goalW)} kg" else "-- kg"

        // Prominent Result Text - ONLY show if there is an actual weight difference!
        val totalDiff = if (startW != null && currentW != null && startW > 0) {
            currentW - startW
        } else {
            state.weightDiff
        }
        val absDiff = Math.abs(totalDiff)
        val hasResult = startW != null && currentW != null && startW > 0 && absDiff >= 0.05

        if (hasResult) {
            bind.tvTotalChangeLabel.visibility = View.VISIBLE
            bind.tvWeightDiff.visibility = View.VISIBLE
            (bind.layoutMetricsRow.layoutParams as? ViewGroup.MarginLayoutParams)?.topMargin = (12 * resources.displayMetrics.density).toInt()
            val diffFormatted = formatWeightValue(absDiff)
            bind.tvTotalChangeLabel.text = "Vazn"

            val isProgressGood = if (state.isGainGoal) totalDiff > 0 else totalDiff < 0
            val statusColor = requireContext().getColor(if (isProgressGood) R.color.green else R.color.orange)

            when {
                totalDiff < 0 -> {
                    bind.tvWeightDiff.text = "$diffFormatted kg kamaydi"
                    bind.tvWeightDiff.setTextColor(statusColor)
                }
                totalDiff > 0 -> {
                    bind.tvWeightDiff.text = "$diffFormatted kg qo'shildi"
                    bind.tvWeightDiff.setTextColor(statusColor)
                }
                else -> {
                }
            }
        } else {
            bind.tvTotalChangeLabel.visibility = View.GONE
            bind.tvWeightDiff.visibility = View.GONE
            (bind.layoutMetricsRow.layoutParams as? ViewGroup.MarginLayoutParams)?.topMargin = 0
        }



        updateIdealWeight(bind, state)
    }

    private fun updateIdealWeight(bind: FragmentNatijaBinding, state: NatijaUiState) {
        val rangeText = state.idealWeightText
        val heightCm = state.userHeightCm

        if (rangeText.isBlank()) {
            bind.cardIdealWeight.isVisible = false
            return
        }

        bind.cardIdealWeight.isVisible = true
        bind.tvIdealWeightRange.text = rangeText
        bind.tvIdealWeightSubtitle.text = if (heightCm > 0) {
            "Bo'yingiz ($heightCm cm) uchun eng maqbul va sog'lom vazn oralig'i."
        } else {
            "Bo'yingiz uchun eng maqbul va sog'lom vazn oralig'i."
        }
    }
}
