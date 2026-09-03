package uz.oybek.ozamiz.ui.feature.log.exercise

import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
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
        setupExerciseTip(bind)
    }

    private fun setupExerciseTip(bind: FragmentSelectExerciseBinding) {
        val tips = listOf(
            R.string.exercise_tip_1,
            R.string.exercise_tip_2,
            R.string.exercise_tip_3,
            R.string.exercise_tip_4,
            R.string.exercise_tip_5,
            R.string.exercise_tip_6,
            R.string.exercise_tip_7,
            R.string.exercise_tip_8,
            R.string.exercise_tip_9,
            R.string.exercise_tip_10,
            R.string.exercise_tip_11,
            R.string.exercise_tip_12,
            R.string.exercise_tip_13,
            R.string.exercise_tip_14,
            R.string.exercise_tip_15,
            R.string.exercise_tip_16,
            R.string.exercise_tip_17,
            R.string.exercise_tip_18,
            R.string.exercise_tip_19,
            R.string.exercise_tip_20
        )
        val randomTip = tips.random()
        bind.tvExerciseTip.text = getString(randomTip)
    }

    private fun setupToolbar(bind: FragmentSelectExerciseBinding) {
        bind.toolbarSelectExercise.setNavigationOnClickListener {
            popBackStack()
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

                            // Update Today's Progress
                            bind.tvTodayBurnedValue.text = getString(R.string.exercise_progress_format, state.totalBurnedToday, state.exerciseTarget)
                            bind.progressExerciseToday.max = state.exerciseTarget
                            bind.progressExerciseToday.setProgressCompat(state.totalBurnedToday, true)
                            bind.tvGoalStatus.isVisible = state.totalBurnedToday >= state.exerciseTarget
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