package uz.oybek.ozamiz.ui.feature.log.exercise

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import androidx.core.view.isVisible
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.setFragmentResult
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import uz.oybek.ozamiz.R
import uz.oybek.ozamiz.databinding.FragmentAddExerciseLogBinding
import uz.oybek.ozamiz.ui.base.BaseFragment
import uz.oybek.ozamiz.ui.utils.getBenefit
import uz.oybek.ozamiz.ui.utils.getTitle
import uz.oybek.ozamiz.ui.utils.hideSoftInput
import uz.oybek.ozamiz.ui.utils.setOnSafeClickListener
import uz.oybek.ozamiz.ui.utils.showKeyboard
import uz.oybek.ozamiz.ui.utils.showToast

@AndroidEntryPoint
class AddExerciseLogFragment : BaseFragment<FragmentAddExerciseLogBinding>() {

    private val viewModel: AddExerciseLogViewModel by viewModels()

    override fun inflateBinding(
        inflater: LayoutInflater,
        container: ViewGroup?
    ): FragmentAddExerciseLogBinding =
        FragmentAddExerciseLogBinding.inflate(inflater, container, false)

    override fun viewCreated(bind: FragmentAddExerciseLogBinding) {
        setupListeners()
        observeState()
    }

    override fun onResume() {
        super.onResume()
        binding.editTextDuration.showKeyboard()
    }

    private fun setupListeners() = with(binding) {
        toolbarAddExercise.setNavigationOnClickListener {
            popBackStack()
        }

        buttonSaveExerciseLog.setOnSafeClickListener {
            hideSoftInput(requireActivity())
            viewModel.saveExerciseLog()
        }

        editTextDuration.doAfterTextChanged { text ->
            viewModel.onDurationChanged(text?.toString().orEmpty())
        }

        editTextDuration.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                if (viewModel.uiState.value.isSaveButtonVisible) {
                    hideSoftInput(requireActivity())
                    viewModel.saveExerciseLog()
                    true
                } else false
            } else false
        }
    }

    private fun observeState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.uiState.collect { state ->
                        renderUi(state)
                    }
                }
                launch {
                    viewModel.effects.collect { effect ->
                        handleEffect(effect)
                    }
                }
            }
        }
    }

    private fun renderUi(state: AddExerciseUiState) = with(binding) {
        progressBarExercise.isVisible = state.isSaving
        buttonSaveExerciseLog.isEnabled = !state.isSaving
        editTextDuration.isEnabled = !state.isSaving
        buttonSaveExerciseLog.text = if (state.isSaving) "" else getString(R.string.save)

        val exercise = state.selectedExercise
        if (exercise != null) {
            tvExerciseName.text = exercise.getTitle(requireContext())
            textSelectedExerciseEmoji.text = exercise.emoji
            tvExerciseBenefit.text = exercise.getBenefit(requireContext())
        }

        val showResult = state.durationMinutes > 0 && state.caloriesBurned > 0
        if (showResult) {
            tvBurnedCalories.text = getString(
                R.string.exercise_add_burned_result_format,
                state.caloriesBurned,
                state.fatBurnedGrams
            )
        }

        setViewVisibleAnimated(tvBurnedCalories, showResult)
        setViewVisibleAnimated(saveButtonContainer, state.isSaveButtonVisible)
    }

    private fun setViewVisibleAnimated(view: View, isTargetVisible: Boolean) {
        if (isTargetVisible) {
            if (!view.isVisible || view.alpha < 1f) {
                view.isVisible = true
                view.animate().cancel()
                if (view.alpha == 0f) {
                    view.translationY = 24f
                }
                view.animate()
                    .alpha(1f)
                    .translationY(0f)
                    .setDuration(220L)
                    .start()
            }
        } else {
            if (view.isVisible && view.alpha > 0f) {
                view.animate().cancel()
                view.animate()
                    .alpha(0f)
                    .translationY(16f)
                    .setDuration(180L)
                    .withEndAction {
                        view.isVisible = false
                    }
                    .start()
            }
        }
    }

    private fun handleEffect(effect: AddExerciseUiEffect) {
        when (effect) {
            is AddExerciseUiEffect.ExerciseSaved -> {
                popBackStack()
            }

            is AddExerciseUiEffect.ShowToastRes -> {
                context?.let { ctx -> showToast(ctx.getString(effect.messageResId)) }
            }

            is AddExerciseUiEffect.NavigateBack -> {
                popBackStack()
            }
        }
    }
}