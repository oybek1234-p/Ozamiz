package uz.oybek.ozamiz.ui.discover

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AlertDialog
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.bumptech.glide.Glide
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import uz.oybek.ozamiz.R
import uz.oybek.ozamiz.data.workouts.WorkoutExecutionState
import uz.oybek.ozamiz.data.workouts.WorkoutState
import uz.oybek.ozamiz.databinding.FragmentWorkoutExecutionBinding
import uz.oybek.ozamiz.ui.getinfo.Event
import uz.oybek.ozamiz.ui.showToast
import java.util.Locale

class WorkoutExecutionFragment : Fragment() {

    private var _binding: FragmentWorkoutExecutionBinding? = null
    private val binding get() = _binding!!
    private val viewModel: WorkoutExecutionViewModel by viewModels()
    private val args: WorkoutExecutionFragmentArgs by navArgs()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentWorkoutExecutionBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupClickListeners(binding)
        observeViewModel(binding)
        setupBackPressedHandling()

        if (savedInstanceState == null) {
            viewModel.initialize(args.workoutId, args.exercises.toList())
        }
    }

    private fun setupClickListeners(bind: FragmentWorkoutExecutionBinding) {
        bind.backView.setOnClickListener { showExitConfirmationDialog() }
        bind.skipReadyToGo.setOnClickListener { viewModel.skip() }
        bind.materialButton.setOnClickListener { viewModel.pauseResumeWorkout() }
        bind.workingPrev.setOnClickListener { viewModel.previous() }
        bind.workingNext.setOnClickListener { viewModel.skip() }
        bind.addRestTime.setOnClickListener { viewModel.addRestTime() }
        bind.skipRestTime.setOnClickListener { viewModel.skip() }
    }

    private fun setupBackPressedHandling() {
        val callback = object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                showExitConfirmationDialog()
            }
        }
        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner, callback)
    }

    private fun showExitConfirmationDialog() {
        if (!isAdded) return
        viewModel.pauseResumeWorkout()

        AlertDialog.Builder(requireContext()).setTitle(R.string.confirm_exit_workout_title)
            .setMessage(R.string.confirm_exit_workout_message).setPositiveButton(R.string.exit) { dialog, _ ->
                viewModel.handleExitConfirmation()
                dialog.dismiss()
            }.setNegativeButton(R.string.stay) { dialog, _ ->
                dialog.dismiss()
            }.setCancelable(false).show()
    }

    private fun observeViewModel(bind: FragmentWorkoutExecutionBinding) {
        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.state.collectLatest { state ->
                        updateUi(bind, state)
                    }
                }

                launch {
                    viewModel.navigationEvent.observe(viewLifecycleOwner) { event: Event<WorkoutExecutionNavigationEvent>? ->
                        event?.getContentIfNotHandled()?.let { navEvent ->
                            if (!isAdded || findNavController().currentDestination?.id != R.id.workoutExecutionFragment) {
                                return@let
                            }

                            when (navEvent) {
                                WorkoutExecutionNavigationEvent.NavigateBack -> {
                                    findNavController().popBackStack()
                                }

                                WorkoutExecutionNavigationEvent.WorkoutFinished -> {
                                    findNavController().navigate(R.id.action_workoutExecutionFragment_to_home)
                                }

                                is WorkoutExecutionNavigationEvent.ShowError -> {
                                    showToast(navEvent.message)
                                }
                            }
                        }
                    }
                }

                launch {
                    viewModel.audioCueEvent.observe(viewLifecycleOwner) { event: Event<AudioCue?>? ->

                    }
                }
            }
        }
    }

    private fun updateUi(bind: FragmentWorkoutExecutionBinding, state: WorkoutExecutionState) {
        bind.readyToGoView.isVisible = state.state == WorkoutState.PREPARING
        bind.workingContainer.isVisible =
            state.state == WorkoutState.WORKING || state.state == WorkoutState.PAUSED
        bind.restContainer.isVisible = state.state == WorkoutState.RESTING

        if (state.state != WorkoutState.FINISHED) {
            val currentExNumber =
                if (state.state == WorkoutState.RESTING) state.completedExercises + 2 else state.completedExercises + 1
            bind.exerciseSizeView.text =
                "${currentExNumber.coerceAtMost(state.totalExercises)}/${state.totalExercises}"
            bind.exerciseSizeView.isVisible = true
        } else {
            bind.exerciseSizeView.isVisible = false
        }

        val exerciseForImage = when (state.state) {
            WorkoutState.RESTING -> state.currentExercise
            WorkoutState.FINISHED -> null
            else -> state.currentExercise
        }

        if (exerciseForImage != null) {
            Glide.with(this).load(exerciseForImage.mediaUrl).placeholder(R.drawable.placeholder_exercise)
                .error(R.drawable.placeholder_exercise).into(bind.animView)
        } else {
            bind.animView.setImageResource(R.drawable.placeholder_workout)
        }

        when (state.state) {
            WorkoutState.PREPARING -> {
                bind.nameView.text = state.currentExercise?.name ?: getString(R.string.loading)
                bind.textView2.text = state.timeRemainingSeconds.toString()
                bind.readyProgress.setProgress(state.timerProgress, true)
            }

            WorkoutState.WORKING -> {
                bind.workingName.text = state.currentExercise?.name ?: ""
                if (state.currentExercise?.durationSeconds != null && state.currentExercise.durationSeconds!! > 0) {
                    bind.workingCountView.text = formatTime(state.timeRemainingSeconds)
                } else {
                    bind.workingCountView.text =
                        state.currentExercise?.repetitions?.let { "x$it" } ?: ""
                }
                bind.materialButton.setText(R.string.pause)
                bind.materialButton.setIconResource(R.drawable.pause_ic)
                bind.materialButton.isEnabled = true
            }

            WorkoutState.PAUSED -> {
                bind.workingName.text = state.currentExercise?.name ?: ""
                if (state.currentExercise?.durationSeconds != null && state.currentExercise.durationSeconds!! > 0) {
                    bind.workingCountView.text = formatTime(state.timeRemainingSeconds)
                } else {
                    bind.workingCountView.text =
                        state.currentExercise?.repetitions?.let { "x$it" } ?: ""
                }
                bind.materialButton.setText(R.string.resume)
                bind.materialButton.setIconResource(R.drawable.play_ic)
                bind.materialButton.isEnabled = true
            }

            WorkoutState.RESTING -> {
                bind.restCountView.text = state.timeRemainingSeconds.toString()
                bind.restProgress.setProgress(state.timerProgress, true)
                bind.nextCountView.text = getString(
                    R.string.next_exercise_format,
                    (state.completedExercises + 2).coerceAtMost(state.totalExercises),
                    state.totalExercises
                )
                bind.restNextName.text =
                    "${state.nextExercise?.name ?: ""} ${state.nextExercise?.getDurationOrRepsText() ?: ""}"
                Glide.with(this).load(state.nextExercise?.mediaUrl).placeholder(R.drawable.placeholder_exercise)
                    .error(R.drawable.placeholder_exercise).into(bind.nextImageView)
            }

            WorkoutState.FINISHED -> {}
        }
    }

    private fun formatTime(seconds: Int): String {
        val minutes = seconds / 60
        val remainingSeconds = seconds % 60
        return String.format(Locale.getDefault(), "%02d:%02d", minutes, remainingSeconds)
    }

    override fun onPause() {
        super.onPause()
        viewModel.stopAndReleasePlayer()

        val currentState = viewModel.state.value.state
        if (currentState == WorkoutState.PREPARING || currentState == WorkoutState.WORKING || currentState == WorkoutState.RESTING) {
            viewModel.pauseResumeWorkout()
        }
    }

    override fun onResume() {
        super.onResume()
        val state = viewModel.state.value
        updateUi(binding, state)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}