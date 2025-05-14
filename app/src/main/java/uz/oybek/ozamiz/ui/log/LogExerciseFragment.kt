package uz.oybek.ozamiz.ui.log

import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.gson.Gson
import kotlinx.coroutines.launch
import uz.oybek.ozamiz.R
import uz.oybek.ozamiz.data.info.ExerciseLog
import uz.oybek.ozamiz.databinding.FragmentLogExerciseBinding
import uz.oybek.ozamiz.ui.base.BaseFragment
import uz.oybek.ozamiz.ui.getinfo.ExerciseType
import uz.oybek.ozamiz.ui.hideSoftInput
import uz.oybek.ozamiz.ui.showToast

class LogExerciseFragment : BaseFragment<FragmentLogExerciseBinding>() {

    private val viewModel: LogExerciseViewModel by viewModels()
    private lateinit var exerciseAdapter: ExerciseTypeSelectableAdapter

    override fun inflateBinding(
        inflater: LayoutInflater, container: ViewGroup?
    ): FragmentLogExerciseBinding {
        return FragmentLogExerciseBinding.inflate(inflater, container, false)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val logJson = arguments?.getString("logToEditJson")
        val preselectedId = arguments?.getString("preselectedExerciseId")
        viewModel.loadInitialData(logJson, preselectedId)
    }

    override fun viewCreated(bind: FragmentLogExerciseBinding) {
        setupToolbar(bind)
        setupRecyclerView(bind)
        setupListeners(bind)
        observeViewModel(bind)
    }

    private fun setupToolbar(bind: FragmentLogExerciseBinding) {
        bind.toolbarLogExercise.setNavigationOnClickListener {
            findNavController().popBackStack()
        }
    }

    private fun setupRecyclerView(bind: FragmentLogExerciseBinding) {
        exerciseAdapter = ExerciseTypeSelectableAdapter { selectedType ->
            viewModel.setSelectedExerciseType(selectedType)
        }
        bind.recyclerViewExerciseTypes.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = exerciseAdapter
            itemAnimator = null // Optional: disable animation
        }
    }

    private fun setupListeners(bind: FragmentLogExerciseBinding) {
        bind.buttonSaveExerciseLog.setOnClickListener {
            val durationStr = bind.editTextDuration.text.toString()
            viewModel.saveExerciseLog(durationStr)
            hideSoftInput(requireActivity())
        }
    }

    private fun observeViewModel(bind: FragmentLogExerciseBinding) {
        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    val isLoading = state is LogExerciseUiState.Loading || state is LogExerciseUiState.Saving
                    bind.progressBarExercise.isVisible = isLoading
                    bind.buttonSaveExerciseLog.isEnabled = !isLoading
                    bind.buttonSaveExerciseLog.text = if (isLoading) "" else getString(R.string.save)
                    bind.editTextDuration.isEnabled = !isLoading
                    bind.recyclerViewExerciseTypes.isEnabled = !isLoading // Consider disabling RV

                    when (state) {
                        is LogExerciseUiState.Loading -> {
                            // Handled above
                        }
                        is LogExerciseUiState.Input -> {
                            bind.toolbarLogExercise.title = getString(R.string.log_exercise_title)
                            exerciseAdapter.submitList(state.availableTypes)
                            bind.editTextDuration.setText("") // Clear duration on initial input
                            // Pre-select if needed
                             state.preselectedTypeId?.let { id ->
                                 val position = state.availableTypes.indexOfFirst { it.id == id }
                                 if (position != -1) {
                                     exerciseAdapter.setSelectedPosition(position)
                                     viewModel.setSelectedExerciseType(state.availableTypes[position]) // Inform ViewModel
                                 }
                             }
                        }
                        is LogExerciseUiState.Editing -> {
                             bind.toolbarLogExercise.title = getString(R.string.edit_exercise_log_title)
                             exerciseAdapter.submitList(state.availableTypes) // Submit all types
                             bind.editTextDuration.setText(state.log.durationMin.toString())

                             // Find and select the existing type
                             val position = state.availableTypes.indexOfFirst { it.id == state.log.activityType }
                             if (position != -1) {
                                 exerciseAdapter.setSelectedPosition(position)
                                 viewModel.setSelectedExerciseType(state.availableTypes[position]) // Ensure ViewModel knows
                             } else {
                                 // Handle case where logged activity type is no longer in default list?
                                 showToast("Original activity type not found, please re-select.")
                             }
                        }

                        is LogExerciseUiState.Saving -> {
                            // Handled above
                        }
                        is LogExerciseUiState.Success -> {
                            showToast(getString(R.string.exercise_log_saved_success))
                            if (findNavController().currentDestination?.id == R.id.logExerciseFragment) {
                                findNavController().popBackStack()
                            }
                        }
                        is LogExerciseUiState.Error -> {
                            showToast(state.message)
                             // Optionally reset state or keep fields as they are
                        }
                    }
                }
            }
        }
    }
}