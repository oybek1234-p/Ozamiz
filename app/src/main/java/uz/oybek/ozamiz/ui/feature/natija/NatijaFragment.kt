package uz.oybek.ozamiz.ui.feature.natija

import android.graphics.Color
import android.view.LayoutInflater
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
        bind.btnEditGoalWeight.setOnClickListener {
            val sheet = EditWeightGoalsBottomSheet.newInstance()
            sheet.onSavedListener = {
                viewModel.loadData()
            }
            sheet.show(childFragmentManager, EditWeightGoalsBottomSheet.TAG)
        }
        bind.btnPredictionInfo.setOnClickListener {
            com.google.android.material.dialog.MaterialAlertDialogBuilder(requireContext())
                .setTitle("Tavsiya va taxmin")
                .setMessage("Ushbu ko'rsatkichlar sizning kunlik kaloriya va faolligingiz asosida hisoblangan taxminiy natijalardir.")
                .setPositiveButton("Tushunarli", null)
                .show()
        }
    }

    private fun observeState(bind: FragmentNatijaBinding) {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    bind.swipeRefreshNatija.isRefreshing = state.isLoading
                    bind.cardAddWeight.isVisible = true

                    // Prediction Summary with Target Completion Date
                    if (state.isPredictionLoading) {
                        bind.tvPredictionSummary.text = "Hisoblanmoqda..."
                    } else {
                        val rawWeekly = state.weeklyPredictedLoss.replace("-", "").replace("+", "").trim()
                        val rawMonthly = state.monthlyPredictedLoss.replace("-", "").replace("+", "").trim()
                        val rawDuration = state.goalDurationText.replace("qoldi", "").trim()
                        val goalDate = state.goalDateFormatted

                        val weeklyText = if (rawWeekly.isNotEmpty() && rawWeekly != "...") rawWeekly else "0.5 kg"
                        val monthlyText = if (rawMonthly.isNotEmpty() && rawMonthly != "...") rawMonthly else "1.8 kg"
                        val durationText = if (rawDuration.isNotEmpty() && rawDuration != "Hisoblanmoqda...") rawDuration else "2 oy 5 kunda"

                        val summaryText = if (state.isGainGoal) {
                            "Tog'ri ovqatlansangiz har hafta $weeklyText semirasiz, oyiga $monthlyText semirasiz. Maqsadingizga $durationText erishasiz."
                        } else {
                            "Tog'ri ovqatlansangiz har hafta $weeklyText ozasiz, oyiga $monthlyText ozasiz. Maqsadingizga $durationText erishasiz."
                        }
                        bind.tvPredictionSummary.text = summaryText
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
            String.format(java.util.Locale.US, "%.1f", value)
        }
    }

    private fun updateStats(
        bind: FragmentNatijaBinding,
        state: NatijaUiState
    ) {
        val currentW = state.currentWeight
        val startW = state.startWeight ?: currentW
        val goalW = state.goalWeight ?: 0.0

        bind.tvCurrentWeight.text =
            if (currentW != null && currentW > 0) "${formatWeightValue(currentW)} kg" else "-- kg"
        bind.tvGoalWeight.text =
            if (goalW > 0) "${formatWeightValue(goalW)} kg" else "-- kg"
        bind.tvStartWeight.text =
            if (startW != null && startW > 0) "${formatWeightValue(startW)} kg" else "-- kg"

        val totalDiff = if (startW != null && currentW != null && startW > 0) {
            currentW - startW
        } else {
            state.weightDiff
        }
        val absDiff = Math.abs(totalDiff)
        val diffFormatted = formatWeightValue(absDiff)

        bind.tvTotalChangeLabel.text = "Natija"
        when {
            totalDiff < 0 -> {
                bind.tvWeightDiff.text = "$diffFormatted kg kamaydi"
            }

            totalDiff > 0 -> {
                bind.tvWeightDiff.text = "$diffFormatted kg qo'shildi"
            }

            else -> {
                bind.tvWeightDiff.text = "$diffFormatted kg"
            }
        }
        updateBmi(bind, state)
    }

    private fun updateBmi(bind: FragmentNatijaBinding, state: NatijaUiState) {
        val bmi = state.bmi
        val category = state.bmiCategory

        if (bmi == null || bmi <= 0) {
            bind.cardBmi.isVisible = false
            return
        }

        bind.cardBmi.isVisible = true
        bind.tvBmiValue.text = String.format("%.2f", bmi)

        bind.btnBmiInfo.setOnClickListener {
            if (bmi > 0) {
                val sheet = BmiInfoBottomSheetFragment.newInstance(bmi, category)
                sheet.show(parentFragmentManager, "bmi_info")
            }
        }
    }
}
