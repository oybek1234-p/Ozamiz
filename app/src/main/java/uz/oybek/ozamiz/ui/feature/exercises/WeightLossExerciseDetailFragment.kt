package uz.oybek.ozamiz.ui.feature.exercises

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import uz.oybek.ozamiz.R
import uz.oybek.ozamiz.databinding.FragmentWeightLossExerciseDetailBinding
import uz.oybek.ozamiz.databinding.ItemBenefitBulletBinding
import uz.oybek.ozamiz.databinding.ItemInstructionStepBinding
import uz.oybek.ozamiz.ui.base.BaseFragment
import uz.oybek.ozamiz.ui.utils.getArgs
import uz.oybek.ozamiz.ui.utils.loadPhoto

class WeightLossExerciseDetailFragment : BaseFragment<FragmentWeightLossExerciseDetailBinding>() {

    private val viewModel: WeightLossExerciseDetailViewModel by viewModels()

    init {
        showBottomSheet = false
    }

    override fun inflateBinding(inflater: LayoutInflater, container: ViewGroup?): FragmentWeightLossExerciseDetailBinding {
        return FragmentWeightLossExerciseDetailBinding.inflate(inflater, container, false)
    }

    override fun viewCreated(bind: FragmentWeightLossExerciseDetailBinding) {
        val exerciseId = arguments.getArgs<WeightLossExerciseArgs>()?.exerciseId
            ?: arguments?.getString("exerciseId").orEmpty()
        viewModel.loadExercise(exerciseId)

        bind.toolbar.setNavigationOnClickListener {
            navigateUp()
        }

        bind.btnPlayPause.setOnClickListener {
            viewModel.toggleStartPause()
        }

        bind.btnRestart.setOnClickListener {
            viewModel.restartTimer()
        }

        viewModel.uiState.observe(viewLifecycleOwner) { state ->
            val exercise = state.exercise ?: return@observe
            bind.toolbar.title = exercise.name

            bind.imageExerciseMedia.setImageResource(exercise.imageRes)

            val minutes = state.timeLeftSeconds / 60
            val seconds = state.timeLeftSeconds % 60
            bind.textTimerDigits.text = getString(R.string.exerc_timer_format, minutes, seconds)
            bind.progressTimer.progress = state.progress

            if (state.isRunning) {
                bind.imagePlayPauseIcon.setImageResource(R.drawable.pause_ic)
                bind.textPlayPauseLabel.text = getString(R.string.exerc_pauza)
            } else {
                bind.imagePlayPauseIcon.setImageResource(R.drawable.play_ic)
                bind.textPlayPauseLabel.text = getString(R.string.exerc_boshlash)
            }

            bind.textBurnedCaloriesValue.text = exercise.calories.toString()
            bind.textDurationValue.text = getString(R.string.exerc_sec_format, exercise.durationSeconds)

            bind.layoutBenefitsList.removeAllViews()
            exercise.benefits.forEach { benefit ->
                val bulletBinding = ItemBenefitBulletBinding.inflate(
                    LayoutInflater.from(requireContext()),
                    bind.layoutBenefitsList,
                    false
                )
                bulletBinding.textBenefitContent.text = benefit
                bind.layoutBenefitsList.addView(bulletBinding.root)
            }

            bind.layoutInstructionsList.removeAllViews()
            exercise.instructions.forEachIndexed { index, instruction ->
                val stepBinding = ItemInstructionStepBinding.inflate(
                    LayoutInflater.from(requireContext()),
                    bind.layoutInstructionsList,
                    false
                )
                stepBinding.textStepNumber.text = (index + 1).toString()
                stepBinding.textStepContent.text = instruction
                bind.layoutInstructionsList.addView(stepBinding.root)
            }
        }
    }
}
