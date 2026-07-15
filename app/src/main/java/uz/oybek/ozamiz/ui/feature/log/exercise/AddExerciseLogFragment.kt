package uz.oybek.ozamiz.ui.feature.log.exercise

import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import kotlinx.coroutines.launch
import androidx.fragment.app.setFragmentResult
import uz.oybek.ozamiz.R
import uz.oybek.ozamiz.databinding.FragmentAddExerciseLogBinding
import uz.oybek.ozamiz.ui.base.BaseFragment
import uz.oybek.ozamiz.ui.utils.hideSoftInput
import uz.oybek.ozamiz.ui.utils.showKeyboard
import uz.oybek.ozamiz.ui.utils.showToast

class AddExerciseLogFragment
    : BaseFragment<FragmentAddExerciseLogBinding>() {

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
        binding.textInputLayoutDuration.editText.showKeyboard()
    }

    private fun setupListeners(bind: FragmentAddExerciseLogBinding) {
        bind.toolbarAddExercise.setNavigationOnClickListener {
            findNavController().popBackStack()
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
                    bind.buttonSaveExerciseLog.text = if(isProcessing) "" else getString(R.string.save)

                    when (state) {
                        is AddExerciseUiState.Input -> {
                            bind.toolbarAddExercise.title = state.selectedExercise.exerciseName
                            bind.textSelectedExerciseEmoji.text = state.selectedExercise.emoji
                            bind.textBurnedCalories.text = getString(R.string.unit_kcal_burned,
                                state.caloriesBurned)
                        }
                        is AddExerciseUiState.Success -> {
                            setFragmentResult("exercise_added_request", android.os.Bundle().apply {
                                putString("exerciseId", state.exerciseId)
                                putInt("caloriesBurned", state.caloriesBurned)
                            })
                            findNavController().popBackStack(R.id.home, false)
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