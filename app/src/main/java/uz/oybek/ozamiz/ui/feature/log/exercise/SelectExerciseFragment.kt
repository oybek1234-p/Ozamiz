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
import uz.oybek.ozamiz.databinding.FragmentSelectExerciseBinding
import uz.oybek.ozamiz.ui.base.BaseFragment
import uz.oybek.ozamiz.ui.utils.showToast

class SelectExerciseFragment : BaseFragment<FragmentSelectExerciseBinding>() {

    private val viewModel: SelectExerciseViewModel by viewModels()
    private lateinit var exerciseAdapter: ExerciseTypeSelectableAdapter

    override fun inflateBinding(
        inflater: LayoutInflater, container: ViewGroup?
    ): FragmentSelectExerciseBinding {
        return FragmentSelectExerciseBinding.inflate(inflater, container, false)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val timestamp = arguments?.getLong("selectedDateTimestamp") ?: System.currentTimeMillis()
        viewModel.loadInitialData(timestamp)
    }

    override fun viewCreated(bind: FragmentSelectExerciseBinding) {
        setupToolbar(bind)
        setupRecyclerView(bind)
        observeViewModel(bind)
    }

    private fun setupToolbar(bind: FragmentSelectExerciseBinding) {
        bind.toolbarSelectExercise.setNavigationOnClickListener {
            findNavController().popBackStack()
        }
    }

    private fun setupRecyclerView(bind: FragmentSelectExerciseBinding) {
        exerciseAdapter = ExerciseTypeSelectableAdapter { selectedType ->
            val args = Bundle().apply {
                putString("exerciseId", selectedType.name)
                putLong("selectedDateTimestamp", viewModel.selectedDate.time)
            }
            navigate(R.id.addExerciseLogFragment, args)
        }
        bind.recyclerViewExerciseTypes.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = exerciseAdapter
            itemAnimator = null
        }
    }

    private fun observeViewModel(bind: FragmentSelectExerciseBinding) {
        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    bind.progressBar.isVisible = state is SelectExerciseUiState.Loading

                    when (state) {
                        is SelectExerciseUiState.Success -> {
                            exerciseAdapter.submitList(state.availableTypes)
                        }
                        is SelectExerciseUiState.Error -> {
                            showToast(state.message)
                        }
                        else -> {}
                    }
                }
            }
        }
    }
}