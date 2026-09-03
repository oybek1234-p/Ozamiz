package uz.oybek.ozamiz.ui.feature.log.exercise

import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.setFragmentResult
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.launch
import uz.oybek.ozamiz.R
import uz.oybek.ozamiz.databinding.FragmentAddExerciseLogBinding
import uz.oybek.ozamiz.ui.base.BaseFragment
import uz.oybek.ozamiz.ui.utils.hideSoftInput
import uz.oybek.ozamiz.ui.utils.showKeyboard
import uz.oybek.ozamiz.ui.utils.showToast
import java.util.Locale

class AddExerciseLogFragment : BaseFragment<FragmentAddExerciseLogBinding>() {

    private val viewModel: AddExerciseLogViewModel by viewModels()

    override fun inflateBinding(
        inflater: LayoutInflater, container: ViewGroup?
    ): FragmentAddExerciseLogBinding {
        return FragmentAddExerciseLogBinding.inflate(
            inflater,
            container,
            false
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel.loadArguments(arguments)
    }

    override fun viewCreated(bind: FragmentAddExerciseLogBinding) {
        setupListeners(bind)
        observeViewModel(bind)
    }

    override fun onResume() {
        super.onResume()
        binding.editTextDuration.showKeyboard()
    }

    private fun setupListeners(bind: FragmentAddExerciseLogBinding) {
        bind.toolbarAddExercise.setNavigationOnClickListener {
            popBackStack()
        }
        bind.buttonSaveExerciseLog.setOnClickListener {
            val durationStr = bind.editTextDuration.text.toString()
            viewModel.saveExerciseLog(durationStr)
            hideSoftInput(requireActivity())
        }
        bind.editTextDuration.doAfterTextChanged {
            viewModel.onDurationChanged(it.toString())
        }
    }

    private fun observeViewModel(bind: FragmentAddExerciseLogBinding) {
        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    val isProcessing = state is AddExerciseUiState.Saving
                    bind.progressBarExercise.isVisible = isProcessing
                    bind.buttonSaveExerciseLog.isEnabled = !isProcessing
                    bind.editTextDuration.isEnabled = !isProcessing
                    bind.buttonSaveExerciseLog.text = if (isProcessing) "" else "Saqlash"

                    when (state) {
                        is AddExerciseUiState.Input -> {
                            bind.tvExerciseName.text = state.selectedExercise.exerciseName
                            bind.textSelectedExerciseEmoji.text = state.selectedExercise.emoji
                            bind.tvExerciseBenefit.text = state.selectedExercise.benefit

                            val calories = state.caloriesBurned
                            if (calories > 0) {
                                val fatGrams = String.format(Locale.US, "%.1f", calories / 7.7)
                                bind.cardExerciseResult.isVisible = true
                                bind.tvBurnedCalories.text = "🔥 $calories kkal • $fatGrams g yog' yoqildi"
                                bind.tvPraiseMessage.text = "Barakalla! Ajoyib natija 👏"
                            } else {
                                bind.cardExerciseResult.isVisible = false
                            }
                        }
                        is AddExerciseUiState.Success -> {
                            setFragmentResult("exercise_added_request", Bundle().apply {
                                putString("exerciseId", state.exerciseId)
                                putInt("caloriesBurned", state.caloriesBurned)
                            })
                            popBackStack(R.id.home, false)
                        }
                        is AddExerciseUiState.Error -> {
                            showToast(state.message)
                        }
                        else -> {}
                    }
                }
            }
        }
    }
}