package uz.oybek.ozamiz.ui.foods

import android.app.AlertDialog
import android.app.Application
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.google.gson.Gson
import kotlinx.coroutines.launch
import uz.oybek.ozamiz.ImageUploader
import uz.oybek.ozamiz.R
import uz.oybek.ozamiz.data.DataWrapper
import uz.oybek.ozamiz.data.foods.FoodsRepository
import uz.oybek.ozamiz.data.foods.SimpleMealIdea
import uz.oybek.ozamiz.data.info.MealsType
import uz.oybek.ozamiz.handleException
import uz.oybek.ozamiz.ui.getinfo.Event
import uz.oybek.ozamiz.ui.loadPhoto
import uz.oybek.ozamiz.ui.showToast


data class AddEditSimpleMealIdeaUiState(
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val errorMessage: String? = null,
    val ideaToEdit: SimpleMealIdea? = null,
    val title: String = "Yangi Oddiy Variant Qo'shish",
    val saveSuccess: Boolean = false,
    val deleteSuccess: Boolean = false,
    val canDelete: Boolean = false
)

sealed class AddEditSimpleMealIdeaNavigationEvent {
    data class ShowToast(val message: String) : AddEditSimpleMealIdeaNavigationEvent()
    object DismissDialog : AddEditSimpleMealIdeaNavigationEvent() // Dialogni yopish
}


class AddEditSimpleMealIdeaViewModel(application: Application) : AndroidViewModel(application) {

    private val foodsRepository = FoodsRepository()
    private val imageUploader = ImageUploader
    private val gson = Gson()

    private val _uiState = MutableLiveData(AddEditSimpleMealIdeaUiState())
    val uiState: LiveData<AddEditSimpleMealIdeaUiState> = _uiState

    private val _navigationEvent = MutableLiveData<Event<AddEditSimpleMealIdeaNavigationEvent>>()
    val navigationEvent: LiveData<Event<AddEditSimpleMealIdeaNavigationEvent>> = _navigationEvent

    private var currentIdea = SimpleMealIdea()
    private var initialIdeaJson: String? = null
    var selectedImageUri: Uri? = null

    fun initialize(ideaJson: String?) {
        initialIdeaJson = ideaJson
        if (ideaJson != null) {
            try {
                currentIdea = gson.fromJson(ideaJson, SimpleMealIdea::class.java)
                _uiState.value = AddEditSimpleMealIdeaUiState(
                    ideaToEdit = currentIdea, title = "Oddiy Variantni Tahrirlash", canDelete = true
                )
            } catch (e: Exception) {
                handleException(e, "IdeaJsonParseError")
                _uiState.value =
                    AddEditSimpleMealIdeaUiState(errorMessage = "Variant ma'lumotlarini o'qishda xatolik")
            }
        } else {
            currentIdea = SimpleMealIdea()
            _uiState.value = AddEditSimpleMealIdeaUiState(title = "Yangi Oddiy Variant Qo'shish")
        }
        selectedImageUri = null
        _uiState.value = _uiState.value?.copy(
            isSaving = false, saveSuccess = false, deleteSuccess = false, errorMessage = null
        )
    }

    fun saveIdea(
        name: String,
        description: String,
        mealType: String?,
        estimatedCalories: String,
        order: String
    ) {
        if (name.isBlank()) {
            _uiState.value = _uiState.value?.copy(errorMessage = "Nomi/tavsifini kiriting")
            return
        }
        val orderInt = order.toIntOrNull() ?: 0 // Default order to 0
        val caloriesInt = estimatedCalories.toIntOrNull() // Nullable

        _uiState.value = _uiState.value?.copy(isSaving = true, errorMessage = null)

        viewModelScope.launch {
            var finalImageUrl = currentIdea.imageUrl
            var oldImageUrlToDelete: String? = null

            if (selectedImageUri != null) {
                when (val uploadResult = imageUploader.uploadImage(selectedImageUri!!)) {
                    is DataWrapper.Success -> {
                        if (!currentIdea.imageUrl.isNullOrEmpty()) {
                            oldImageUrlToDelete = currentIdea.imageUrl
                        }
                        finalImageUrl = uploadResult.data
                    }

                    is DataWrapper.Error -> {
                        _uiState.postValue(
                            _uiState.value?.copy(
                                isSaving = false,
                                errorMessage = "Rasm yuklashda xatolik: ${uploadResult.error}"
                            )
                        )
                        return@launch
                    }

                    is DataWrapper.Loading -> {}
                }
            } else if (!currentIdea.id.isBlank() && currentIdea.imageUrl != null && selectedImageUri == null && !oldImageUrlToDelete.isNullOrEmpty()) {
                // Keep existing image if no new one selected and not explicitly marked for deletion (handled by UI)
                // But if we want to clear it, handle here
            }


            val ideaToSave = currentIdea.copy(
                name = name.trim(),
                description = description.trim().ifBlank { null },
                mealTypeTag = mealType?.trim()?.ifBlank { null },
                estimatedCalories = caloriesInt,
                order = orderInt,
                imageUrl = finalImageUrl
            )

            val result = if (ideaToSave.id.isBlank()) {
                foodsRepository.addSimpleMealIdea(ideaToSave)
            } else {
                foodsRepository.updateSimpleMealIdea(ideaToSave)
            }

            when (result) {
                is DataWrapper.Success -> {
                    oldImageUrlToDelete?.let { imageUploader.deleteImage(it) }
                    _navigationEvent.postValue(
                        Event(
                            AddEditSimpleMealIdeaNavigationEvent.ShowToast(
                                if (currentIdea.id.isBlank()) "Oddiy variant qo'shildi" else "Oddiy variant yangilandi"
                            )
                        )
                    )
                    _navigationEvent.postValue(Event(AddEditSimpleMealIdeaNavigationEvent.DismissDialog))
                }

                is DataWrapper.Error -> {
                    _uiState.postValue(
                        _uiState.value?.copy(
                            isSaving = false, errorMessage = result.error
                        )
                    )
                    // Rollback new image upload if save failed
                    selectedImageUri?.let { uploadedUrl ->
                        if (finalImageUrl == uploadedUrl.toString()) {
                            imageUploader.deleteImage(finalImageUrl)
                        }
                    }
                }

                is DataWrapper.Loading -> {}
            }
        }
    }

    fun deleteIdea() {
        val ideaIdToDelete = currentIdea.id
        val imageUrlToDelete = currentIdea.imageUrl
        if (ideaIdToDelete.isBlank()) return

        _uiState.value = _uiState.value?.copy(isSaving = true, errorMessage = null)
        viewModelScope.launch {
            when (val result = foodsRepository.deleteSimpleMealIdea(ideaIdToDelete)) {
                is DataWrapper.Success -> {
                    imageUrlToDelete?.let { imageUploader.deleteImage(it) }
                    _navigationEvent.postValue(
                        Event(
                            AddEditSimpleMealIdeaNavigationEvent.ShowToast(
                                "Oddiy variant o'chirildi"
                            )
                        )
                    )
                    _navigationEvent.postValue(Event(AddEditSimpleMealIdeaNavigationEvent.DismissDialog))
                }

                is DataWrapper.Error -> {
                    _uiState.postValue(
                        _uiState.value?.copy(
                            isSaving = false, errorMessage = "O'chirishda xatolik: ${result.error}"
                        )
                    )
                }

                is DataWrapper.Loading -> {}
            }
        }
    }

    fun hasChanges(
        name: String,
        description: String,
        mealType: String?,
        estimatedCalories: String,
        order: String
    ): Boolean {
        val initial = try {
            if (initialIdeaJson != null) gson.fromJson(
                initialIdeaJson, SimpleMealIdea::class.java
            ) else SimpleMealIdea()
        } catch (e: Exception) {
            SimpleMealIdea()
        }

        val currentName = name.trim()
        val currentDescription = description.trim().ifBlank { null }
        val currentMealType = mealType?.trim()?.ifBlank { null }
        val currentCalories = estimatedCalories.toIntOrNull()
        val currentOrder = order.toIntOrNull() ?: 0

        val dataChanged =
            currentName != initial.name || currentDescription != initial.description || currentMealType != initial.mealTypeTag || currentCalories != initial.estimatedCalories || currentOrder != initial.order

        val imageChanged =
            selectedImageUri != null || (initial.imageUrl != null && currentIdea.imageUrl == null) // Assuming UI handles image clear update on currentIdea

        // Need a way to track if existing image was explicitly cleared in UI
        // For now, rely on selectedImageUri for new, and assume if initial.imageUrl was present
        // and currentIdea.imageUrl becomes null (due to UI logic), it's a change.
        // A safer way is a dedicated flag like `isImageCleared`.

        return dataChanged || selectedImageUri != null || (initial.imageUrl != null && currentIdea.imageUrl == null)
    }


    fun clearError() {
        _uiState.value = _uiState.value?.copy(errorMessage = null)
    }
}


// Add/Edit Simple Meal Idea Dialog Fragment
class AddEditSimpleMealIdeaDialog :
    com.google.android.material.bottomsheet.BottomSheetDialogFragment() { // Or DialogFragment

    private var _binding: uz.oybek.ozamiz.databinding.DialogAddEditSimpleMealIdeaBinding? = null
    private val binding get() = _binding!!
    private val viewModel: AddEditSimpleMealIdeaViewModel by viewModels()

    private val pickImageLauncher = registerForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            viewModel.selectedImageUri = it
            binding.includeImagePickerIdea.imageViewPicked.loadPhoto(it)
            binding.includeImagePickerIdea.layoutAddImagePlaceholder.isVisible = false
            binding.includeImagePickerIdea.buttonRemoveImage.isVisible = true
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = uz.oybek.ozamiz.databinding.DialogAddEditSimpleMealIdeaBinding.inflate(
            inflater, container, false
        )
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val ideaJson = arguments?.getString("ideaJson")
        viewModel.initialize(ideaJson)

        setupViews(binding)
        setupListeners(binding)
        observeViewModel(binding)
    }

    private fun setupViews(bind: uz.oybek.ozamiz.databinding.DialogAddEditSimpleMealIdeaBinding) {
        bind.includeImagePickerIdea.textViewAddImagePlaceholder.text = "Rasm tanlang (ixtiyoriy)"

        val mealTypes = MealsType.mealTypes.map { it.name }.toTypedArray()
        val adapter =
            android.widget.ArrayAdapter(requireContext(), R.layout.list_item_dropdown, mealTypes)
        bind.autoCompleteIdeaMealType.setAdapter(adapter)

        // Default meal type selection logic can be added here if needed
    }

    private fun setupListeners(bind: uz.oybek.ozamiz.databinding.DialogAddEditSimpleMealIdeaBinding) {
        bind.includeImagePickerIdea.buttonRemoveImage.setOnClickListener {
            viewModel.selectedImageUri = null
            // Need to update UI state to reflect removal of existing image too
            val existingImageUrl = viewModel.uiState.value?.ideaToEdit?.imageUrl
            if (!existingImageUrl.isNullOrEmpty()) {
                // Clear the image and show placeholder
                bind.includeImagePickerIdea.imageViewPicked.setImageResource(0)
                bind.includeImagePickerIdea.layoutAddImagePlaceholder.isVisible = true
                bind.includeImagePickerIdea.buttonRemoveImage.isVisible = false
                // Update ViewModel's internal state to mark for deletion or clear the URL
                // This logic might be needed in ViewModel depending on how `hasChanges` tracks this
            } else {
                // This should only happen if selectedImageUri was set and is now cleared
                bind.includeImagePickerIdea.imageViewPicked.setImageResource(0)
                bind.includeImagePickerIdea.layoutAddImagePlaceholder.isVisible = true
                bind.includeImagePickerIdea.buttonRemoveImage.isVisible = false
            }
        }
        bind.includeImagePickerIdea.cardImagePicker.setOnClickListener {
            pickImageLauncher.launch("image/*")
        }

        bind.buttonSaveIdea.setOnClickListener {
            val name = bind.editTextIdeaName.text.toString()
            val description = bind.editTextIdeaDescription.text.toString()
            val mealType =
                bind.autoCompleteIdeaMealType.text.toString() // Get text from AutoCompleteTextView
            val estimatedCalories = bind.editTextIdeaCalories.text.toString()
            val order = bind.editTextIdeaOrder.text.toString()

            val selectedMealTypeId = MealsType.mealTypes.find { it.name == mealType }?.id

            viewModel.saveIdea(name, description, selectedMealTypeId, estimatedCalories, order)
        }

        bind.buttonCancelIdea.setOnClickListener {
            dismiss()
        }

        // Optional: Delete button listener
        bind.buttonDeleteIdea.setOnClickListener {
            showDeleteConfirmationDialog()
        }
    }

    private fun observeViewModel(bind: uz.oybek.ozamiz.databinding.DialogAddEditSimpleMealIdeaBinding) {
        viewModel.uiState.observe(viewLifecycleOwner) { state ->
            bind.progressBarSaveIdea.isVisible = state.isSaving
            bind.layoutButtons.isVisible = !state.isSaving
            dialog?.setCancelable(!state.isSaving)

            bind.textViewDialogTitle.text = state.title
            bind.buttonDeleteIdea.isVisible = state.canDelete // Show delete button if editing

            // Populate fields if editing
            if (state.ideaToEdit != null && bind.editTextIdeaName.text.isNullOrEmpty()) {
                bind.editTextIdeaName.setText(state.ideaToEdit.name)
                bind.editTextIdeaDescription.setText(state.ideaToEdit.description ?: "")
                bind.editTextIdeaCalories.setText(
                    state.ideaToEdit.estimatedCalories?.toString() ?: ""
                )
                bind.editTextIdeaOrder.setText(state.ideaToEdit.order.toString())
                // Set meal type in AutoCompleteTextView
                state.ideaToEdit.mealTypeTag?.let { tag ->
                    val mealTypeName = MealsType.fromId(tag)?.name
                    bind.autoCompleteIdeaMealType.setText(
                        mealTypeName ?: "", false
                    ) // 'false' to not trigger dropdown
                }

                if (!state.ideaToEdit.imageUrl.isNullOrEmpty()) {
                    bind.includeImagePickerIdea.imageViewPicked.loadPhoto(state.ideaToEdit.imageUrl)
                    bind.includeImagePickerIdea.layoutAddImagePlaceholder.isVisible = false
                    bind.includeImagePickerIdea.buttonRemoveImage.isVisible = true
                } else {
                    // Ensure placeholder is shown if existing image is null/empty
                    bind.includeImagePickerIdea.imageViewPicked.setImageResource(0)
                    bind.includeImagePickerIdea.layoutAddImagePlaceholder.isVisible = true
                    bind.includeImagePickerIdea.buttonRemoveImage.isVisible = false
                }
            }

            state.errorMessage?.let {
                showToast(it)
                viewModel.clearError()
            }
        }

        viewModel.navigationEvent.observe(viewLifecycleOwner) { event ->
            event?.getContentIfNotHandled()?.let { navEvent ->
                when (navEvent) {
                    is AddEditSimpleMealIdeaNavigationEvent.ShowToast -> showToast(navEvent.message)
                    is AddEditSimpleMealIdeaNavigationEvent.DismissDialog -> dismiss()
                }
            }
        }
    }

    private fun showDeleteConfirmationDialog() {
        if (!isAdded) return
        AlertDialog.Builder(requireContext()).setTitle("Oddiy variantni o'chirish")
            .setMessage("Haqiqatan ham bu variantni o'chirmoqchimisiz?")
            .setPositiveButton(R.string.yes_delete) { dialog, _ ->
                viewModel.deleteIdea()
                dialog.dismiss()
            }.setNegativeButton(R.string.cancel, null).show()
    }


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}