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
import java.text.SimpleDateFormat
import java.util.Date
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
            navigate(R.id.photoUploadInfoFragment)
        }
        
        bind.btnAddWeightBottom.setOnClickListener {
            navigate(R.id.photoUploadInfoFragment)
        }
        
        // Open History Fragment
        bind.cardHistory.setOnClickListener {
            navigate(R.id.action_natijaFragment_to_weightHistoryFragment)
        }
    }

    private fun observeState(bind: FragmentNatijaBinding) {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    bind.swipeRefreshNatija.isRefreshing = state.isLoading

                    // Add Weight Banner visibility based on 7-day logic
                    bind.cardAddWeight.isVisible = state.showTopBanner
                    bind.cardAddWeightBottom.isVisible = state.showBottomBanner

                    // Prediction
                    bind.cardPrediction.isVisible = state.estimatedGoalDate != null
                    if (state.estimatedGoalDate != null) {
                        val targetDate = java.time.Instant.ofEpochMilli(state.estimatedGoalDate)
                            .atZone(java.time.ZoneId.systemDefault())
                            .toLocalDate()
                        val today = java.time.LocalDate.now()
                        val period = java.time.Period.between(today, targetDate)
                        
                        val months = period.months + (period.years * 12)
                        val days = period.days
                        
                        val durationStr = StringBuilder()
                        if (months > 0) durationStr.append("$months oy")
                        if (days > 0) {
                            if (durationStr.isNotEmpty()) durationStr.append(", ")
                            durationStr.append("$days kun")
                        }
                        if (durationStr.isEmpty()) durationStr.append("Bugun")
                        else durationStr.append("da")

                        bind.tvPredictionDate.text = durationStr.toString()
                        bind.tvPredictionWeight.text = "${(state.goalWeight ?: 0.0).toInt()} kg"
                    }

                    // Photos
                    val hasAnyPhoto = state.photoBefore != null || state.photoAfter != null
                    bind.layoutPhotos.isVisible = hasAnyPhoto
                    if (hasAnyPhoto) {
                        val hasBefore = state.photoBefore != null
                        val hasAfter = state.photoAfter != null
                        val onlyOnePhoto = hasBefore xor hasAfter
                        
                        // Before
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

                        // After
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

    private fun updateStats(
        bind: FragmentNatijaBinding,
        state: NatijaUiState
    ) {
        val currentW = state.currentWeight
        val startW = state.startWeight ?: currentW
        val goalW = state.goalWeight ?: 0.0

        bind.tvStartWeight.text = if (startW != null && startW > 0) String.format("%.1f kg", startW) else "-- kg"
        bind.tvCurrentWeight.text = if (currentW != null && currentW > 0) String.format("%.1f kg", currentW) else "-- kg"
        bind.tvGoalWeight.text = "${goalW.toInt()} kg"

        // Progress Calculation
        if (startW != null && currentW != null && goalW > 0 && startW != goalW) {
            val totalToLose = startW - goalW
            val lost = startW - currentW
            // Clamp progress between 0 and 100
            val progress = ((lost / totalToLose) * 100).toInt().coerceIn(0, 100)
            bind.progressWeight.progress = progress
        } else {
             bind.progressWeight.progress = 0
        }

        // Weight Left (Remaining)
        if (currentW != null && goalW > 0) {
             val left = currentW - goalW
             // If left is positive, show "X kg", else "0 kg" (reached)
             val leftText = if (left > 0) String.format("%.1f", left) else "0.0"
             bind.tvWeightLeft.text = "$leftText kg"
        } else {
             bind.tvWeightLeft.text = "-- kg"
        }

        val diff = state.weightDiff
        val diffText = if (diff > 0) "+${String.format("%.1f", diff)}" else String.format("%.1f", diff)
        bind.tvWeightDiff.text = "$diffText kg"
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
        
        val statusText = when (category) {
            BmiCategory.UNDERWEIGHT -> "Kam vazn"
            BmiCategory.HEALTHY -> "Sog'lom"
            BmiCategory.OVERWEIGHT -> "Ortiqcha vazn"
            BmiCategory.OBESE -> "Semizlik"
            else -> ""
        }
        
        val color = when (category) {
            BmiCategory.UNDERWEIGHT -> Color.parseColor("#2196F3")
            BmiCategory.HEALTHY -> Color.parseColor("#4CAF50")
            BmiCategory.OVERWEIGHT -> Color.parseColor("#FFC107")
            BmiCategory.OBESE -> Color.parseColor("#F44336")
            else -> Color.GRAY
        }
        
        bind.btnBmiInfo.setOnClickListener {
             if (bmi > 0) {
                 val sheet = BmiInfoBottomSheetFragment.newInstance(bmi, category)
                 sheet.show(parentFragmentManager, "bmi_info")
             }
        }

        bind.tvBmiStatus.text = statusText
        bind.tvBmiStatus.backgroundTintList = android.content.res.ColorStateList.valueOf(color)

        // Marker Position
        bind.layoutBmiScale.post {
            val width = bind.layoutBmiScale.width
            
            val percentage = when {
                bmi < 18.5 -> {
                    // Underweight: 0.0 -> 0.33
                    val ratio = ((bmi - 10.0) / (18.5 - 10.0)).coerceIn(0.0, 1.0)
                    0.0 + ratio * 0.33
                }
                bmi < 25.0 -> {
                    // Healthy: 0.33 -> 0.66
                    val ratio = ((bmi - 18.5) / (25.0 - 18.5)).coerceIn(0.0, 1.0)
                    0.33 + ratio * 0.33
                }
                bmi < 30.0 -> {
                    // Overweight: 0.66 -> 0.83 (Yellow Zone)
                    val ratio = ((bmi - 25.0) / (30.0 - 25.0)).coerceIn(0.0, 1.0)
                    0.66 + ratio * 0.17
                }
                else -> {
                    // Obese: 0.83 -> 1.0
                    val ratio = ((bmi - 30.0) / (45.0 - 30.0)).coerceIn(0.0, 1.0)
                    0.83 + ratio * 0.17
                }
            }
            
            val x = (width * percentage).toFloat()
            bind.viewBmiMarker.translationX = x
        }
    }
}
