package uz.oybek.ozamiz.ui.feature.log.water

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import uz.oybek.ozamiz.R
import uz.oybek.ozamiz.databinding.DialogLogWaterBinding
import uz.oybek.ozamiz.ui.modelUtils.NutritionFormatter
import uz.oybek.ozamiz.ui.utils.triggerVibration

@AndroidEntryPoint
class LogWaterDialog : BottomSheetDialogFragment() {

    private var _binding: DialogLogWaterBinding? = null
    private val binding get() = _binding!!

    private val viewModel: LogWaterViewModel by viewModels()

    private val waterTipResIds = listOf(
        R.string.log_water_tip_1,
        R.string.log_water_tip_2,
        R.string.log_water_tip_3,
        R.string.log_water_tip_4,
        R.string.log_water_tip_5,
        R.string.log_water_tip_6,
        R.string.log_water_tip_7,
        R.string.log_water_tip_8
    )

    private var currentTipIndex = (0..7).random()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DialogLogWaterBinding.inflate(inflater, container, false)
        dialog?.window?.setBackgroundDrawableResource(android.R.color.transparent)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupClickListeners()
        updateTipText()
    }

    private fun updateTipText() {
        if (_binding != null) {
            val tipResId = waterTipResIds[currentTipIndex % waterTipResIds.size]
            currentTipIndex++
            binding.tvWaterTip.setText(tipResId)
        }
    }

    private fun animateButton(view: View, onEnd: () -> Unit) {
        view.animate()
            .scaleX(0.92f)
            .scaleY(0.92f)
            .setDuration(70)
            .withEndAction {
                view.animate()
                    .scaleX(1.0f)
                    .scaleY(1.0f)
                    .setDuration(90)
                    .withEndAction { onEnd() }
                    .start()
            }
            .start()
    }

    private fun setupClickListeners() {
        binding.apply {
            cardGlass250.setOnClickListener { animateButton(it) { addWater(250) } }
            cardBottle500.setOnClickListener { animateButton(it) { addWater(500) } }
            cardBottle1000.setOnClickListener { animateButton(it) { addWater(1000) } }
            btnCloseWater.setOnClickListener { dismiss() }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    val target = state.waterTargetMl
                    binding.progressWaterCircular.max = target
                    binding.progressWaterCircular.setProgressCompat(
                        state.waterConsumedMl.coerceAtMost(target),
                        true
                    )
                    binding.tvWaterProgress.text = NutritionFormatter.waterMl(state.waterConsumedMl)
                    binding.tvWaterTarget.text = getString(
                        R.string.log_water_target_format,
                        target
                    )
                    binding.cardWaterGoalReached.isVisible = state.isGoalReached
                }
            }
        }
    }

    private fun addWater(amountMl: Int) {
        viewModel.addWater(amountMl)
        triggerVibration()
        updateTipText()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}