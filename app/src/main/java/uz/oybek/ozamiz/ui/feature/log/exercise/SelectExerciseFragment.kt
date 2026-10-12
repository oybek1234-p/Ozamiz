package uz.oybek.ozamiz.ui.feature.log.exercise

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import uz.oybek.ozamiz.R
import uz.oybek.ozamiz.databinding.FragmentSelectExerciseBinding
import uz.oybek.ozamiz.ui.base.BaseFragment
import uz.oybek.ozamiz.ui.utils.showToast
import uz.oybek.ozamiz.ui.utils.toBundle

@AndroidEntryPoint
class SelectExerciseFragment : BaseFragment<FragmentSelectExerciseBinding>() {

    private val viewModel: SelectExerciseViewModel by viewModels()
    private var exerciseAdapter: ExerciseTypeSelectableAdapter? = null

    override fun inflateBinding(
        inflater: LayoutInflater,
        container: ViewGroup?
    ): FragmentSelectExerciseBinding =
        FragmentSelectExerciseBinding.inflate(inflater, container, false)

    override fun viewCreated(bind: FragmentSelectExerciseBinding) {
        setupToolbar()
        setupRecyclerView()
        observeState()
    }

    private fun setupToolbar() = with(binding) {
        toolbarSelectExercise.setNavigationOnClickListener {
            popBackStack()
        }
    }

    private fun setupRecyclerView() = with(binding) {
        val adapter = ExerciseTypeSelectableAdapter { selectedType ->
            viewModel.onExerciseSelected(selectedType)
        }
        exerciseAdapter = adapter
        recyclerViewExerciseTypes.apply {
            layoutManager = LinearLayoutManager(context)
            this.adapter = adapter
            itemAnimator = null
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

    private fun renderUi(state: SelectExerciseUiState) = with(binding) {
        progressBar.isVisible = state.isLoading
        exerciseAdapter?.submitList(state.availableTypes)

        tvTodayBurnedHeadline.text = getString(R.string.exercise_today_burned_format, state.totalBurnedToday)

        tvTodayBurnedSubtitle.text = if (state.isGoalReached) {
            getString(R.string.exercise_goal_reached_sub, state.exerciseTarget)
        } else {
            getString(R.string.exercise_goal_zero_sub, state.exerciseTarget)
        }

        progressExerciseToday.max = state.exerciseTarget
        progressExerciseToday.setProgressCompat(state.totalBurnedToday, true)

        cardExerciseGoalReached.isVisible = state.isGoalReached
    }

    private fun handleEffect(effect: SelectExerciseUiEffect) {
        when (effect) {
            is SelectExerciseUiEffect.NavigateToAddExercise -> {
                val args = AddExerciseArgs(exerciseId = effect.exerciseId)
                navigate(R.id.addExerciseLogFragment, args.toBundle())
            }

            is SelectExerciseUiEffect.ShowToastRes -> {
                context?.let { ctx -> showToast(ctx.getString(effect.messageResId)) }
            }
        }
    }

    override fun onDestroyView() {
        exerciseAdapter = null
        super.onDestroyView()
    }
}