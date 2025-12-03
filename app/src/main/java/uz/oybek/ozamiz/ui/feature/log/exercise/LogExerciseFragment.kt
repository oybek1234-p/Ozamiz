package uz.oybek.ozamiz.ui.feature.log.exercise

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
import kotlinx.coroutines.launch
import uz.oybek.ozamiz.R
import uz.oybek.ozamiz.databinding.FragmentLogExerciseBinding
import uz.oybek.ozamiz.ui.base.BaseFragment
import uz.oybek.ozamiz.ui.utils.hideSoftInput
import uz.oybek.ozamiz.ui.utils.showToast

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
        // Tanlangan sanani argumentlardan olish
        val timestamp = arguments?.getLong("selectedDateTimestamp", System.currentTimeMillis()) ?: System.currentTimeMillis()
        viewModel.loadInitialData(logJson, preselectedId, timestamp)
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
            itemAnimator = null
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

                    when (state) {
                        is LogExerciseUiState.Input -> {
                            bind.toolbarLogExercise.title = getString(R.string.log_exercise_title)
                            exerciseAdapter.submitList(state.availableTypes)
                            bind.editTextDuration.setText("")
                            state.preselectedTypeId?.let { id ->
                                val position = state.availableTypes.indexOfFirst { it.id == id }
                                if (position != -1) {
                                    exerciseAdapter.setSelectedPosition(position)
                                    viewModel.setSelectedExerciseType(state.availableTypes[position])
                                }
                            }
                        }
                        is LogExerciseUiState.Editing -> {
                            bind.toolbarLogExercise.title = getString(R.string.edit_exercise_log_title)
                            exerciseAdapter.submitList(state.availableTypes)
                            bind.editTextDuration.setText(state.log.durationMin.toString())
                            val position = state.availableTypes.indexOfFirst { it.id == state.log.exerciseType }
                            if (position != -1) {
                                exerciseAdapter.setSelectedPosition(position)
                                viewModel.setSelectedExerciseType(state.availableTypes[position])
                            }
                        }
                        is LogExerciseUiState.Success -> {
                            showToast(getString(R.string.exercise_log_saved_success))
                            if (findNavController().currentDestination?.id == R.id.logExerciseFragment) {
                                findNavController().popBackStack()
                            }
                        }
                        is LogExerciseUiState.Error -> {
                            showToast(state.message)
                        }
                        else -> {
                            // Loading and Saving states are handled by visibility toggles
                        }
                    }
                }
            }
        }
    }
}