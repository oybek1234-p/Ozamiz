package uz.oybek.ozamiz.ui.discover

import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.os.bundleOf
import androidx.core.view.children
import androidx.core.view.isVisible
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.setFragmentResultListener
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.bumptech.glide.Glide
import com.google.android.material.chip.Chip
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import uz.oybek.ozamiz.R
import uz.oybek.ozamiz.data.workouts.Exercise
import uz.oybek.ozamiz.data.workouts.Workout
import uz.oybek.ozamiz.data.workouts.WorkoutCategory
import uz.oybek.ozamiz.databinding.FragmentAddEditWorkoutBinding
import uz.oybek.ozamiz.ui.base.BaseFragment
import uz.oybek.ozamiz.ui.getinfo.Event
import uz.oybek.ozamiz.ui.hideSoftInput
import uz.oybek.ozamiz.ui.showToast

class AddEditWorkoutFragment : BaseFragment<FragmentAddEditWorkoutBinding>() {

    private val viewModel: AddEditWorkoutViewModel by activityViewModels()
    private val args: AddEditWorkoutFragmentArgs by navArgs()

    private val pickImageLauncher =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
            uri?.let {
                viewModel.selectedImageUri = it
                updateImagePickerState(binding, it) // UI ni darhol yangilash
            }
        }

    override fun inflateBinding(
        inflater: LayoutInflater, container: ViewGroup?
    ): FragmentAddEditWorkoutBinding {
        return FragmentAddEditWorkoutBinding.inflate(inflater, container, false)
    }

    override fun viewCreated(bind: FragmentAddEditWorkoutBinding) {
        viewModel.initialize(args.workoutJson)
        setupToolbar(bind)
        setupViews(bind)
        setupListeners(bind)
        observeViewModel(bind)
        setupBackPressedHandling()
        setupFragmentResultListeners()
    }

    private fun setupToolbar(bind: FragmentAddEditWorkoutBinding) {
        bind.toolbarAddEditWorkout.setNavigationOnClickListener { handleBackPress() }
        bind.toolbarAddEditWorkout.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.action_delete_item -> {
                    showDeleteConfirmation()
                    true
                }

                else -> false
            }
        }
    }

    private fun setupViews(bind: FragmentAddEditWorkoutBinding) {
        bind.includeImagePickerWorkout.textViewAddImagePlaceholder.text =
            getString(R.string.add_image)
        val difficultyLevels =
            listOf("Boshlang'ich", "O'rtacha", "Murakkab")
        val adapter = ArrayAdapter(requireContext(), R.layout.list_item_dropdown, difficultyLevels)
        bind.autoCompleteWorkoutDifficulty.setAdapter(adapter)
    }

    private fun setupListeners(bind: FragmentAddEditWorkoutBinding) {
        bind.includeImagePickerWorkout.buttonRemoveImage.setOnClickListener {
            viewModel.selectedImageUri = null
            val imageUrl = viewModel.uiState.value.workoutToEdit?.imageUrl
            updateImagePickerState(bind, imageUrl?.let { Uri.parse(it) })
        }
        bind.includeImagePickerWorkout.cardImagePicker.setOnClickListener {
            pickImageLauncher.launch("image/*")
        }

        bind.editTextWorkoutName.doAfterTextChanged { viewModel.updateName(it.toString()) }
        bind.editTextWorkoutDescription.doAfterTextChanged { viewModel.updateDescription(it.toString()) }
        bind.editTextWorkoutDuration.doAfterTextChanged { viewModel.updateDuration(it.toString()) }
        bind.editTextWorkoutCalories.doAfterTextChanged { viewModel.updateCalories(it.toString()) } // Kaloriya uchun listener
        bind.editTextWorkoutOrder.doAfterTextChanged { viewModel.updateOrder(it.toString()) }
        bind.editTextWorkoutTags.doAfterTextChanged {
            viewModel.updateTags(it.toString().split(",").map { tag -> tag.trim() })
        }
        bind.autoCompleteWorkoutDifficulty.setOnItemClickListener { _, _, position, _ ->
            viewModel.updateDifficulty(
                bind.autoCompleteWorkoutDifficulty.adapter.getItem(position).toString()
            )
        }
        bind.switchRequiresPremium.setOnCheckedChangeListener { _, isChecked ->
            viewModel.updatePremium(
                isChecked
            )
        }
        bind.switchRequiresAd.setOnCheckedChangeListener { _, isChecked ->
            viewModel.updateAdRequired(
                isChecked
            )
        }

        bind.chipGroupWorkoutCategories.setOnCheckedStateChangeListener { group, checkedIds ->
            val selectedIds = checkedIds.mapNotNull { group.findViewById<Chip>(it)?.tag as? String }
            viewModel.updateSelectedCategories(selectedIds)
        }
        bind.chipGroupTargetAreas.setOnCheckedStateChangeListener { group, checkedIds ->
            val selectedAreas =
                checkedIds.mapNotNull { group.findViewById<Chip>(it)?.text?.toString() }
            viewModel.updateTargetAreas(selectedAreas)
        }

        bind.buttonSelectExercises.setOnClickListener {
            viewModel.triggerExerciseSelection()
        }

        bind.buttonSaveWorkout.setOnClickListener {
            viewModel.saveWorkout()
            hideSoftInput(requireActivity())
        }
    }

    private fun observeViewModel(bind: FragmentAddEditWorkoutBinding) {
        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.uiState.collectLatest { state ->
                        val isProcessing = state.isLoading || state.isSaving
                        bind.progressBarSaveWorkout.isVisible = state.isSaving
                        bind.buttonSaveWorkout.isEnabled = !isProcessing
                        bind.buttonSaveWorkout.text =
                            if (state.isSaving) "" else getString(R.string.save)
                        bind.toolbarAddEditWorkout.title = state.title

                        val deleteMenuItem =
                            bind.toolbarAddEditWorkout.menu.findItem(R.id.action_delete_item)
                        deleteMenuItem?.isVisible = state.canDelete

                        bind.progressBarCategoriesWorkout.isVisible = state.isLoading

                        if (!state.isLoading) {
                            populateCategoryChips(
                                bind, state.allCategories, state.selectedCategoryIds
                            )
                            displaySelectedExercises(
                                bind, state.selectedExerciseIds, state.allExercises
                            )

                            if (state.workoutToEdit != null && bind.editTextWorkoutName.text.isNullOrEmpty()) {
                                populateFieldsForEditing(bind, state.workoutToEdit)
                                if (viewModel.selectedImageUri == null) {
                                    updateImagePickerState(
                                        bind,
                                        state.workoutToEdit.imageUrl?.let { Uri.parse(it) })
                                }
                            } else if (state.workoutToEdit == null) {
                                if (viewModel.selectedImageUri == null) {
                                    updateImagePickerState(bind, null)
                                }
                                displaySelectedExercises(
                                    bind, state.selectedExerciseIds, state.allExercises
                                )
                            }
                        }

                        state.errorMessage?.let {
                            showToast(it)
                            viewModel.clearError()
                        }
                    }
                }

                viewModel.navigationEvent.observe(viewLifecycleOwner) { event: Event<AdminWorkoutNavigation>? ->
                    event?.getContentIfNotHandled()?.let { navEvent ->
                        if (!isAdded || findNavController().currentDestination?.id != R.id.addEditWorkoutFragment) return@let

                        when (navEvent) {
                            is AdminWorkoutNavigation.ShowToast -> showToast(navEvent.message)
                            is AdminWorkoutNavigation.NavigateBack -> findNavController().popBackStack()
                            is AdminWorkoutNavigation.ShowExerciseSelectionDialog -> {
                                val initialIds =
                                    ArrayList(viewModel.uiState.value.selectedExerciseIds)
                                val bundle =
                                    bundleOf(SelectExercisesDialogFragment.ARG_INITIAL_SELECTED_IDS to initialIds)
                                findNavController().navigate(
                                    R.id.selectExercisesDialogFragment, bundle
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    private fun setupFragmentResultListeners() {
        setFragmentResultListener(SelectExercisesDialogFragment.REQUEST_KEY) { _, bundle ->
            val selectedIds =
                bundle.getStringArrayList(SelectExercisesDialogFragment.RESULT_SELECTED_IDS)
            if (selectedIds != null) {
                viewModel.updateSelectedExercises(selectedIds.toList())
            }
        }
    }

    private fun populateFieldsForEditing(bind: FragmentAddEditWorkoutBinding, workout: Workout) {
        bind.editTextWorkoutName.setText(workout.name)
        bind.editTextWorkoutDescription.setText(workout.description ?: "")
        bind.editTextWorkoutDuration.setText(workout.durationMinutes?.toString() ?: "")
        bind.editTextWorkoutCalories.setText(workout.estimatedCaloriesBurned?.toString() ?: "") // Kaloriyani to'ldirish
        bind.editTextWorkoutOrder.setText(workout.order.toString())
        bind.editTextWorkoutTags.setText(workout.tags?.joinToString(", ") ?: "")
        bind.autoCompleteWorkoutDifficulty.setText(workout.difficultyLevel ?: "", false)
        bind.switchRequiresPremium.isChecked = workout.requiredPremium
        bind.switchRequiresAd.isChecked = workout.adRequired

        bind.chipGroupTargetAreas.clearCheck()
        workout.targetArea?.forEach { areaName ->
            bind.chipGroupTargetAreas.children.filterIsInstance<Chip>()
                .find { it.text == areaName }?.isChecked = true
        }
        // Kategoriya chiplari observeViewModel ichida to'ldiriladi/tanlanadi
    }

    private fun populateCategoryChips(
        bind: FragmentAddEditWorkoutBinding,
        categories: List<WorkoutCategory>,
        selectedIds: List<String>
    ) {
        if (categories.isEmpty() && bind.progressBarCategoriesWorkout.isVisible) return
        bind.progressBarCategoriesWorkout.isVisible = false
        bind.chipGroupWorkoutCategories.removeAllViews()
        if (categories.isEmpty()) {
            return
        }
        categories.forEach { category ->
            val chip = Chip(
                requireContext(),
                null,
                com.google.android.material.R.style.Widget_Material3_Chip_Filter // Filter chip stilini ishlatish
            ).apply {
                text = category.name
                isCheckable = true
                tag = category.id
                isClickable = true
                isChecked = selectedIds.contains(category.id)
            }
            bind.chipGroupWorkoutCategories.addView(chip)
        }
    }

    private fun displaySelectedExercises(
        bind: FragmentAddEditWorkoutBinding, selectedIds: List<String>, allExercises: List<Exercise>
    ) {
        if (selectedIds.isEmpty()) {
            bind.textViewSelectedExercises.text = getString(R.string.no_exercises_selected)
            return
        }
        val exercisesMap = allExercises.associateBy { it.id }
        val displayText =
            selectedIds.mapNotNull { id -> exercisesMap[id] }.mapIndexed { index, exercise ->
                val durationRep =
                    if (exercise.durationSeconds != null) "${exercise.durationSeconds}s" else "x${exercise.repetitions}"
                "${index + 1}. ${exercise.name} ($durationRep)"
            }.joinToString("\n")
        bind.textViewSelectedExercises.text = displayText
    }

    private fun updateImagePickerState(bind: FragmentAddEditWorkoutBinding, uri: Uri?) {
        if (uri != null) {
            Glide.with(this).load(uri).placeholder(R.drawable.placeholder_workout)
                .error(R.drawable.placeholder_workout)
                .into(bind.includeImagePickerWorkout.imageViewPicked)
            bind.includeImagePickerWorkout.layoutAddImagePlaceholder.isVisible = false
            bind.includeImagePickerWorkout.buttonRemoveImage.isVisible = true
        } else {
            bind.includeImagePickerWorkout.imageViewPicked.setImageResource(0)
            bind.includeImagePickerWorkout.layoutAddImagePlaceholder.isVisible = true
            bind.includeImagePickerWorkout.buttonRemoveImage.isVisible = false
        }
    }

    private fun showDeleteConfirmation() {
        if (!isAdded) return
        AlertDialog.Builder(requireContext()).setTitle("Workoutni o'chirish")
            .setMessage("Haqiqatan ham bu workoutni o'chirmoqchimisiz?")
            .setPositiveButton(R.string.yes_delete) { dialog, _ ->
                viewModel.deleteWorkout()
                dialog.dismiss()
            }.setNegativeButton(R.string.no, null).show()
    }

    private fun setupBackPressedHandling() {
        val callback = object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                this@AddEditWorkoutFragment.handleBackPress()
            }
        }
        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner, callback)
    }

    private fun handleBackPress() {
        if (viewModel.hasChanges()) {
            if (!isAdded) return
            AlertDialog.Builder(requireContext()).setTitle("O'zgarishlar saqlanmadi")
                .setMessage("Chiqib ketishni xohlaysizmi? Kiritilgan ma'lumotlar yo'qoladi.")
                .setPositiveButton("Ha, chiqish") { _, _ -> if (isAdded) findNavController().popBackStack() }
                .setNegativeButton("Yo'q, qolish", null).show()
        } else {
            if (isAdded) findNavController().popBackStack()
        }
    }
}