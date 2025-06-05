package uz.oybek.ozamiz.ui.workoutdetail

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.chip.Chip
import com.google.android.material.color.MaterialColors
import com.google.gson.Gson
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import uz.oybek.ozamiz.R
import uz.oybek.ozamiz.data.LocalUser
import uz.oybek.ozamiz.data.admin
import uz.oybek.ozamiz.data.workouts.Exercise
import uz.oybek.ozamiz.data.workouts.Workout
import uz.oybek.ozamiz.databinding.FragmentWorkoutDetailBinding
import uz.oybek.ozamiz.ui.base.BaseFragment
import uz.oybek.ozamiz.ui.getinfo.Event
import uz.oybek.ozamiz.ui.loadPhoto
import uz.oybek.ozamiz.ui.showToast

class WorkoutDetailFragment : BaseFragment<FragmentWorkoutDetailBinding>() {

    private val viewModel: WorkoutDetailViewModel by viewModels()
    private val args: WorkoutDetailFragmentArgs by navArgs()
    private lateinit var exerciseListAdapter: ExerciseListAdapter

    override fun inflateBinding(
        inflater: LayoutInflater, container: ViewGroup?
    ): FragmentWorkoutDetailBinding {
        return FragmentWorkoutDetailBinding.inflate(inflater, container, false)
    }

    override fun viewCreated(bind: FragmentWorkoutDetailBinding) {
        setupToolbar(bind)
        setupRecyclerView(bind)
        setupClickListeners(bind)
        observeViewModel(bind)

        viewModel.loadWorkoutDetails(args.workoutId)
    }

    private fun setupToolbar(bind: FragmentWorkoutDetailBinding) {
        bind.toolbarWorkoutDetail.setNavigationOnClickListener { findNavController().popBackStack() }
        bind.toolbarWorkoutDetail.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.action_edit_workout -> {
                    viewModel.uiState.value.workout?.let { workout ->
                        if (findNavController().currentDestination?.id == R.id.workoutDetailFragment) {
                            val action =
                                WorkoutDetailFragmentDirections.actionWorkoutDetailFragmentToAddEditWorkoutFragment(
                                    workoutJson = Gson().toJson(workout)
                                )
                            findNavController().navigate(action)
                        }
                    }
                    true
                }
                R.id.action_delete_workout -> {
                    showDeleteConfirmationDialog()
                    true
                }
                else -> false
            }
        }
    }

    private fun showDeleteConfirmationDialog() {
        if (!isAdded) return
        AlertDialog.Builder(requireContext()).setTitle("Workoutni o'chirish")
            .setMessage("Haqiqatan ham bu workoutni o'chirmoqchimisiz?")
            .setPositiveButton(R.string.yes_delete) { dialog, _ ->
                viewModel.deleteWorkout()
                dialog.dismiss()
            }.setNegativeButton(R.string.no, null).show()
    }

    private fun setupRecyclerView(bind: FragmentWorkoutDetailBinding) {
        exerciseListAdapter = ExerciseListAdapter(
            onItemClick = { exercise -> viewModel.onExerciseClicked(exercise) },
            showInfoButton = true
        )
        bind.recyclerViewExercises.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = exerciseListAdapter
            isNestedScrollingEnabled = false
            itemAnimator = null
        }
    }

    private fun setupClickListeners(bind: FragmentWorkoutDetailBinding) {
        bind.buttonStartWorkout.setOnClickListener { viewModel.onStartWorkoutClicked() }
    }

    private fun observeViewModel(bind: FragmentWorkoutDetailBinding) {
        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.uiState.collectLatest { state ->
                        // Workout ma'lumotlari darhol ko'rsatiladi (agar mavjud bo'lsa)
                        state.workout?.let { workout ->
                            displayWorkoutData(bind, workout)
                            bind.nestedScrollView.isVisible = true // Kontentni ko'rsatish
                            bind.layoutActionButtonContainer.isVisible = true // Knopkalarni ko'rsatish

                            val isAdmin = LocalUser.user.admin()
                            val editMenuItem =
                                bind.toolbarWorkoutDetail.menu.findItem(R.id.action_edit_workout)
                            val deleteMenuItem =
                                bind.toolbarWorkoutDetail.menu.findItem(R.id.action_delete_workout)
                            editMenuItem?.isVisible = isAdmin
                            deleteMenuItem?.isVisible = isAdmin

                        } ?: run {
                            // Agar workout hali kelmagan bo'lsa (yoki xatolik)
                            if (!state.isLoadingExercises && state.errorMessage != null) { // Faqat xatolik bo'lsa
                                bind.nestedScrollView.isVisible = false // Kontentni yashirish
                                bind.layoutActionButtonContainer.isVisible = false
                                // Xatolik xabarini ko'rsatish mumkin
                            } else if (!state.isLoadingExercises){
                                bind.nestedScrollView.isVisible = true // Show empty state text
                                bind.textViewWorkoutDetailTitle.text = "Workout topilmadi"
                                bind.textViewWorkoutDetailDescription.text = ""
                                bind.imageViewWorkoutDetailHeader.setImageResource(R.drawable.placeholder_workout)
                                bind.layoutWorkoutInfo.removeAllViews()
                                bind.chipGroupWorkoutTags.removeAllViews()
                                bind.chipGroupWorkoutTags.isVisible = false
                                bind.textViewExerciseListTitle.text = ""
                                bind.layoutActionButtonContainer.isVisible = false
                            }
                        }

                        // Mashqlar ro'yxati yoki progress barni ko'rsatish
                        bind.progressBarExerciseList.isVisible = state.isLoadingExercises
                        bind.recyclerViewExercises.isVisible = !state.isLoadingExercises
                        exerciseListAdapter.submitList(state.exercises)

                        // Start tugmasini boshqarish (mashqlar yuklangandan keyin)
                        bind.buttonStartWorkout.isEnabled = state.canStartWorkout && !state.isLoadingExercises
                        bind.buttonStartWorkout.isVisible = state.canStartWorkout || state.isLoadingExercises // Show button even if loading exercises initially

                        // Xatolik xabari
                        state.errorMessage?.let {
                            showToast(it)
                            viewModel.clearError()
                            // Xatolikda ham kontentni yashirish mumkin
                            if (state.workout == null) {
                                bind.nestedScrollView.isVisible = false
                                bind.layoutActionButtonContainer.isVisible = false
                            }
                        }
                    }
                }

                viewModel.navigationEvent.observe(viewLifecycleOwner) { event: Event<WorkoutDetailNavigationEvent>? ->
                    event?.getContentIfNotHandled()?.let { navEvent ->
                        if (!isAdded || findNavController().currentDestination?.id != R.id.workoutDetailFragment) return@let

                        when (navEvent) {
                            is WorkoutDetailNavigationEvent.NavigateToWorkoutExecution -> {
                                val action =
                                    WorkoutDetailFragmentDirections.actionWorkoutDetailFragmentToWorkoutExecutionFragment(
                                        workoutId = navEvent.workoutId,
                                        exercises = navEvent.exercises.toTypedArray()
                                    )
                                findNavController().navigate(action)
                            }
                            is WorkoutDetailNavigationEvent.NavigateToExerciseDetail -> {
                                val action =
                                    WorkoutDetailFragmentDirections.actionWorkoutDetailFragmentToExerciseDetailDialog(
                                        exerciseJson = Gson().toJson(navEvent.exercise)
                                    )
                                findNavController().navigate(action)
                            }
                            is WorkoutDetailNavigationEvent.NavigateBackWithMessage -> {
                                showToast(navEvent.message)
                                findNavController().popBackStack()
                            }
                        }
                    }
                }
            }
        }
    }

    private fun displayWorkoutData(bind: FragmentWorkoutDetailBinding, workout: Workout) {
        bind.textViewWorkoutDetailTitle.text = workout.name
        bind.textViewWorkoutDetailDescription.text = workout.description ?: ""
        bind.imageViewWorkoutDetailHeader.loadPhoto(
            workout.imageUrl ?: R.drawable.placeholder_workout
        )

        bind.layoutWorkoutInfo.removeAllViews()
        val infoItems = mutableMapOf<Int, String?>()
        workout.durationMinutes?.let { infoItems[R.drawable.date_ic] = "$it min" }
        workout.difficultyLevel?.let { infoItems[R.drawable.trending] = it }
        workout.estimatedCaloriesBurned?.let { infoItems[R.drawable.fire_ic] = "~${it} kkal" }

        infoItems.onEachIndexed { index, (iconRes, text) ->
            if (text != null) {
                val textView = TextView(context).apply {
                    this.text = text
                    setTextAppearance(R.style.TextBodyMedium)
                    setCompoundDrawablesWithIntrinsicBounds(iconRes, 0, 0, 0)
                    compoundDrawablePadding = 4.dpToPx()
                    setCompoundDrawableTintList(
                        ContextCompat.getColorStateList(requireContext(), R.color.iconColor)
                    )
                }
                bind.layoutWorkoutInfo.addView(textView)

                if (index < infoItems.size - 1) {
                    val divider = View(context).apply {
                        layoutParams = LinearLayout.LayoutParams(
                            1.dpToPx(), LinearLayout.LayoutParams.MATCH_PARENT
                        ).apply {
                            marginStart = 12.dpToPx()
                            marginEnd = 12.dpToPx()
                        }
                        setBackgroundColor(
                            MaterialColors.getColor(textView, com.google.android.material.R.attr.colorOutlineVariant)
                        )
                    }
                    bind.layoutWorkoutInfo.addView(divider)
                }
            }
        }
        bind.layoutWorkoutInfo.isVisible = infoItems.isNotEmpty()

        bind.chipGroupWorkoutTags.removeAllViews()
        val tagsToShow = (workout.targetArea ?: emptyList()) + (workout.tags ?: emptyList())
        if (tagsToShow.isNotEmpty()) {
            bind.chipGroupWorkoutTags.isVisible = true
            tagsToShow.forEach { tagText ->
                val chip = Chip(requireContext(), null, R.style.Widget_App_Chip).apply {
                    text = tagText
                }
                bind.chipGroupWorkoutTags.addView(chip)
            }
        } else {
            bind.chipGroupWorkoutTags.isVisible = false
        }

        bind.textViewExerciseListTitle.text =
            getString(R.string.exercises_title_format, workout.exerciseIds.size)
    }


    private fun Int.dpToPx(): Int = (this * resources.displayMetrics.density).toInt()

    override fun onDestroyView() {
        binding.recyclerViewExercises.adapter = null
        super.onDestroyView()
    }
}