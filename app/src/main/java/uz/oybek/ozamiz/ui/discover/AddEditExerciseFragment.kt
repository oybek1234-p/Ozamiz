package uz.oybek.ozamiz.ui.discover

import android.net.Uri
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.setFragmentResult
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
import uz.oybek.ozamiz.data.workouts.Exercise
import uz.oybek.ozamiz.databinding.FragmentAddEditExerciseBinding
import uz.oybek.ozamiz.databinding.IncludeFilePickerBinding
import uz.oybek.ozamiz.ui.base.BaseFragment
import uz.oybek.ozamiz.ui.getinfo.Event
import uz.oybek.ozamiz.ui.hideSoftInput
import uz.oybek.ozamiz.ui.showToast

class AddEditExerciseFragment : BaseFragment<FragmentAddEditExerciseBinding>() {

    private val viewModel: AddEditExerciseViewModel by viewModels()
    private val args: AddEditExerciseFragmentArgs by navArgs()

    private val pickMediaLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let { viewModel.onMediaSelected(it) }
    }
    private val pickNameAudioLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let { viewModel.onNameAudioSelected(it) }
    }
    private val pickAboutAudioLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let { viewModel.onAboutAudioSelected(it) }
    }

    override fun inflateBinding(inflater: LayoutInflater, container: ViewGroup?): FragmentAddEditExerciseBinding {
        return FragmentAddEditExerciseBinding.inflate(inflater, container, false)
    }

    override fun viewCreated(bind: FragmentAddEditExerciseBinding) {
        viewModel.initialize(args.exerciseJson)
        setupToolbar(bind)
        setupViewsAndListeners(bind)
        observeViewModel(bind)
        setupBackPressedHandling()
    }

    private fun setupToolbar(bind: FragmentAddEditExerciseBinding) {
        bind.toolbarAddEditExercise.setNavigationOnClickListener { handleBackPress() }
        bind.toolbarAddEditExercise.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.action_delete_item -> {
                    showDeleteConfirmation()
                    true
                }
                else -> false
            }
        }
    }

    private fun setupViewsAndListeners(bind: FragmentAddEditExerciseBinding) {
        bind.includeMediaPickerExercise.textViewAddImagePlaceholder.text = getString(R.string.add_gif_or_image)
        bind.includeMediaPickerExercise.buttonRemoveImage.setOnClickListener { viewModel.clearMedia() }
        bind.includeMediaPickerExercise.cardImagePicker.setOnClickListener { pickMediaLauncher.launch("image/*") }

        bind.includeNameAudioPicker.textViewFilePickerTitle.text = "Mashq nomi audiosi (ixtiyoriy)"
        bind.includeNameAudioPicker.cardFilePicker.setOnClickListener { pickNameAudioLauncher.launch("audio/*") }
        bind.includeNameAudioPicker.buttonRemoveFile.setOnClickListener { viewModel.clearNameAudio() }

        bind.includeAboutAudioPicker.textViewFilePickerTitle.text = "Mashq haqida audio (ixtiyoriy)"
        bind.includeAboutAudioPicker.cardFilePicker.setOnClickListener { pickAboutAudioLauncher.launch("audio/*") }
        bind.includeAboutAudioPicker.buttonRemoveFile.setOnClickListener { viewModel.clearAboutAudio() }

        bind.editTextExerciseName.doAfterTextChanged { viewModel.updateExerciseField { copy(name = it.toString().trim()) } }
        bind.editTextExerciseDescription.doAfterTextChanged { viewModel.updateExerciseField { copy(description = it.toString().trim().ifBlank { null }) } }
        bind.editTextExerciseDuration.doAfterTextChanged { viewModel.updateExerciseField { copy(durationSeconds = it.toString().toIntOrNull()) } }
        bind.editTextExerciseRepetitions.doAfterTextChanged { viewModel.updateExerciseField { copy(repetitions = it.toString().toIntOrNull()) } }

        bind.buttonSaveExercise.setOnClickListener {
            viewModel.saveExercise(
                name = bind.editTextExerciseName.text.toString(),
                description = bind.editTextExerciseDescription.text.toString(),
                durationStr = bind.editTextExerciseDuration.text.toString(),
                repetitionsStr = bind.editTextExerciseRepetitions.text.toString()
            )
            hideSoftInput(requireActivity())
        }
    }

    private fun observeViewModel(bind: FragmentAddEditExerciseBinding) {
        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.uiState.collectLatest { state ->
                        val isProcessing = state.isLoading || state.isSaving
                        bind.progressBarSaveExercise.isVisible = state.isSaving
                        bind.buttonSaveExercise.isEnabled = !isProcessing
                        bind.buttonSaveExercise.text = if (state.isSaving) "" else getString(R.string.save)
                        bind.toolbarAddEditExercise.title = state.title

                        val deleteMenuItem = bind.toolbarAddEditExercise.menu.findItem(R.id.action_delete_item)
                        deleteMenuItem?.isVisible = state.canDelete

                        if (!state.isLoading && state.exerciseToEdit != null && bind.editTextExerciseName.text.isNullOrEmpty()) {
                            populateFieldsForEditing(bind, state.exerciseToEdit)
                        }
                        updateMediaPickerState(bind, state.mediaUriToDisplay, state.exerciseToEdit?.mediaUrl)
                        updateAudioPickerState(bind.includeNameAudioPicker, viewModel.selectedNameAudioUri, state.exerciseToEdit?.exerciseNameAudioUrl)
                        updateAudioPickerState(bind.includeAboutAudioPicker, viewModel.selectedAboutAudioUri, state.exerciseToEdit?.exerciseAboutAudioUrl)

                        state.errorMessage?.let {
                            showToast(it)
                            viewModel.clearError()
                        }

                        if (state.saveSuccess || state.deleteSuccess) {
                            // FragmentResult yuborish AddEditWorkoutFragment'ga
                            val resultBundle = bundleOf(
                                AddEditExerciseViewModel.RESULT_MODIFIED_EXERCISE_ID to (state.exerciseToEdit?.id ?: viewModel.uiState.value.exerciseToEdit?.id ?: ""), // Yoki yangi ID
                                AddEditExerciseViewModel.RESULT_WAS_DELETED to state.deleteSuccess,
                                "toastMessage" to if(state.saveSuccess) (if(state.exerciseToEdit == null || state.exerciseToEdit.id.isBlank()) "Mashq qo'shildi" else "Mashq yangilandi") else "Mashq o'chirildi"
                            )
                            setFragmentResult(AddEditExerciseViewModel.REQUEST_KEY_EXERCISE_MODIFIED, resultBundle)
                            if (isAdded) findNavController().popBackStack()
                        }
                    }
                }
                // operationResultEvent kuzatuvi kerak emas, chunki bu fragment o'zi natijani yuboradi
            }
        }
    }

    private fun populateFieldsForEditing(bind: FragmentAddEditExerciseBinding, exercise: Exercise) {
        bind.editTextExerciseName.setText(exercise.name)
        bind.editTextExerciseDescription.setText(exercise.description ?: "")
        bind.editTextExerciseDuration.setText(exercise.durationSeconds?.toString() ?: "")
        bind.editTextExerciseRepetitions.setText(exercise.repetitions?.toString() ?: "")
    }

    private fun updateMediaPickerState(bind: FragmentAddEditExerciseBinding, selectedUri: Uri?, existingUrl: String?) {
        val uriToShow = selectedUri ?: existingUrl?.let { Uri.parse(it) }

        if (uriToShow != null) {
            Glide.with(this)
                .load(uriToShow)
                .placeholder(R.drawable.placeholder_exercise)
                .error(R.drawable.placeholder_exercise)
                .into(bind.includeMediaPickerExercise.imageViewPicked)
            bind.includeMediaPickerExercise.layoutAddImagePlaceholder.isVisible = false
            bind.includeMediaPickerExercise.buttonRemoveImage.isVisible = true
        } else {
            bind.includeMediaPickerExercise.imageViewPicked.setImageResource(0)
            bind.includeMediaPickerExercise.layoutAddImagePlaceholder.isVisible = true
            bind.includeMediaPickerExercise.buttonRemoveImage.isVisible = false
        }
    }

    private fun updateAudioPickerState(pickerBinding: IncludeFilePickerBinding, selectedUri: Uri?, existingUrl: String?) {
        val hasSelection = selectedUri != null
        val hasExisting = !existingUrl.isNullOrEmpty()

        pickerBinding.buttonRemoveFile.isVisible = hasSelection || (hasExisting && selectedUri == null) // O'chirish tugmasini ko'rsatish

        if (hasSelection) {
            val fileName = selectedUri?.lastPathSegment ?: "Fayl tanlandi"
            pickerBinding.textViewFilePickerStatus.text = fileName
            pickerBinding.textViewFilePickerStatus.setTextColor(getThemeColor(com.google.android.material.R.attr.colorPrimary))
        } else if (hasExisting) {
            if (existingUrl != null) {
                pickerBinding.textViewFilePickerStatus.text = "Audio yuklangan (${existingUrl.substringAfterLast('/')})"
            }
            pickerBinding.textViewFilePickerStatus.setTextColor(getThemeColor(com.google.android.material.R.attr.colorOnSurfaceVariant))
        } else {
            pickerBinding.textViewFilePickerStatus.text = "Audio fayl tanlang"
            pickerBinding.textViewFilePickerStatus.setTextColor(getThemeColor(com.google.android.material.R.attr.colorOnSurfaceVariant))
        }
    }

    private fun getThemeColor(attr: Int): Int {
        val typedValue = android.util.TypedValue()
        requireContext().theme.resolveAttribute(attr, typedValue, true)
        return typedValue.data
    }

    private fun showDeleteConfirmation() {
        if (!isAdded) return
        AlertDialog.Builder(requireContext())
            .setTitle("Mashqni o'chirish")
            .setMessage("Haqiqatan ham bu mashqni o'chirmoqchimisiz? Bu mashq ishlatilgan workoutlar xato ishlashi mumkin.")
            .setPositiveButton(R.string.yes_delete) { dialog, _ ->
                viewModel.deleteExercise()
                dialog.dismiss()
            }
            .setNegativeButton(R.string.no, null)
            .show()
    }

    private fun setupBackPressedHandling() {
        val callback = object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                this@AddEditExerciseFragment.handleBackPress()
            }
        }
        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner, callback)
    }

    private fun handleBackPress() {
        if (viewModel.hasChanges()) {
            if (!isAdded) return
            AlertDialog.Builder(requireContext())
                .setTitle("O'zgarishlar saqlanmadi")
                .setMessage("Chiqib ketishni xohlaysizmi? Kiritilgan ma'lumotlar yo'qoladi.")
                .setPositiveButton("Ha, chiqish") { _, _ -> if (isAdded) findNavController().popBackStack() }
                .setNegativeButton("Yo'q, qolish", null)
                .show()
        } else {
            if (isAdded) findNavController().popBackStack()
        }
    }
}