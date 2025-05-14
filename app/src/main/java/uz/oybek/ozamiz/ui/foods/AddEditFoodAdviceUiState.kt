package uz.oybek.ozamiz.ui.foods

import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.gson.Gson
import kotlinx.coroutines.launch
import uz.oybek.ozamiz.R
import uz.oybek.ozamiz.data.DataWrapper
import uz.oybek.ozamiz.data.foods.FoodAdvice
import uz.oybek.ozamiz.data.foods.FoodsRepository
import uz.oybek.ozamiz.ui.getinfo.Event
import uz.oybek.ozamiz.handleException
import uz.oybek.ozamiz.ui.showToast


data class AddEditFoodAdviceUiState(
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val errorMessage: String? = null,
    val adviceToEdit: FoodAdvice? = null,
    val title: String = "Yangi Maslahat Qo'shish",
    val saveSuccess: Boolean = false,
    val deleteSuccess: Boolean = false,
    val canDelete: Boolean = false
)

sealed class AddEditFoodAdviceNavigationEvent {
    data class ShowToast(val message: String) : AddEditFoodAdviceNavigationEvent()
    object DismissDialog : AddEditFoodAdviceNavigationEvent()
}

class AddEditFoodAdviceViewModel : ViewModel() {

    private val foodsRepository = FoodsRepository()
    private val gson = Gson()

    private val _uiState = MutableLiveData(AddEditFoodAdviceUiState())
    val uiState: LiveData<AddEditFoodAdviceUiState> = _uiState

    private val _navigationEvent = MutableLiveData<Event<AddEditFoodAdviceNavigationEvent>>()
    val navigationEvent: LiveData<Event<AddEditFoodAdviceNavigationEvent>> = _navigationEvent

    private var currentAdvice = FoodAdvice()
    private var initialAdviceJson: String? = null

    fun initialize(adviceJson: String?) {
        initialAdviceJson = adviceJson
        if (adviceJson != null) {
            try {
                currentAdvice = gson.fromJson(adviceJson, FoodAdvice::class.java)
                _uiState.value = AddEditFoodAdviceUiState(
                    adviceToEdit = currentAdvice,
                    title = "Maslahatni Tahrirlash",
                    canDelete = true
                )
            } catch (e: Exception) {
                 handleException(e, "AdviceJsonParseError")
                _uiState.value = AddEditFoodAdviceUiState(errorMessage = "Maslahat ma'lumotlarini o'qishda xatolik")
            }
        } else {
            currentAdvice = FoodAdvice()
            _uiState.value = AddEditFoodAdviceUiState(title = "Yangi Maslahat Qo'shish")
        }
         _uiState.value = _uiState.value?.copy(isSaving = false, saveSuccess = false, deleteSuccess = false, errorMessage = null)
    }

    fun saveAdvice(title: String, content: String, order: String, tag: String) {
        if (content.isBlank()) {
            _uiState.value = _uiState.value?.copy(errorMessage = "Maslahat matnini kiriting")
            return
        }
         val orderInt = order.toIntOrNull() ?: 0 // Default order to 0

        _uiState.value = _uiState.value?.copy(isSaving = true, errorMessage = null)

        val adviceToSave = currentAdvice.copy(
            title = title.trim().ifBlank { null },
            content = content.trim(),
            order = orderInt,
            tag = tag.trim().ifBlank { null }
        )

        viewModelScope.launch {
            val result = if (adviceToSave.id.isBlank()) {
                foodsRepository.addFoodAdvice(adviceToSave)
            } else {
                foodsRepository.updateFoodAdvice(adviceToSave)
            }

            when (result) {
                is DataWrapper.Success -> {
                    _navigationEvent.postValue(Event(AddEditFoodAdviceNavigationEvent.ShowToast(if (currentAdvice.id.isBlank()) "Maslahat qo'shildi" else "Maslahat yangilandi")))
                    _navigationEvent.postValue(Event(AddEditFoodAdviceNavigationEvent.DismissDialog))
                }
                is DataWrapper.Error -> {
                    _uiState.postValue(_uiState.value?.copy(isSaving = false, errorMessage = result.error))
                }
                is DataWrapper.Loading -> {}
            }
        }
    }

     fun deleteAdvice() {
         val adviceIdToDelete = currentAdvice.id
         if (adviceIdToDelete.isBlank()) return

         _uiState.value = _uiState.value?.copy(isSaving = true, errorMessage = null)
         viewModelScope.launch {
             when(val result = foodsRepository.deleteFoodAdvice(adviceIdToDelete)) {
                 is DataWrapper.Success -> {
                     _navigationEvent.postValue(Event(AddEditFoodAdviceNavigationEvent.ShowToast("Maslahat o'chirildi")))
                     _navigationEvent.postValue(Event(AddEditFoodAdviceNavigationEvent.DismissDialog))
                 }
                 is DataWrapper.Error -> {
                     _uiState.postValue(_uiState.value?.copy(isSaving = false, errorMessage = "O'chirishda xatolik: ${result.error}"))
                 }
                  is DataWrapper.Loading -> {}
             }
         }
     }

    fun hasChanges(title: String, content: String, order: String, tag: String): Boolean {
        val initial = try {
            if (initialAdviceJson != null) gson.fromJson(initialAdviceJson, FoodAdvice::class.java) else FoodAdvice()
        } catch (e: Exception) { FoodAdvice() }

        val currentTitle = title.trim().ifBlank { null }
        val currentContent = content.trim()
        val currentOrder = order.toIntOrNull() ?: 0
        val currentTag = tag.trim().ifBlank { null }

        return currentTitle != initial.title ||
               currentContent != initial.content ||
               currentOrder != initial.order ||
               currentTag != initial.tag
    }


    fun clearError() {
        _uiState.value = _uiState.value?.copy(errorMessage = null)
    }
}

// Add/Edit Food Advice Dialog Fragment
class AddEditFoodAdviceDialog : com.google.android.material.bottomsheet.BottomSheetDialogFragment() { // Or DialogFragment

    private var _binding: uz.oybek.ozamiz.databinding.DialogAddEditFoodAdviceBinding? = null
    private val binding get() = _binding!!
    private val viewModel: AddEditFoodAdviceViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = uz.oybek.ozamiz.databinding.DialogAddEditFoodAdviceBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val adviceJson = arguments?.getString("adviceJson")
        viewModel.initialize(adviceJson)

        setupListeners(binding)
        observeViewModel(binding)
    }

    private fun setupListeners(bind: uz.oybek.ozamiz.databinding.DialogAddEditFoodAdviceBinding) {
        bind.buttonSaveAdvice.setOnClickListener {
            val title = bind.editTextAdviceTitle.text.toString()
            val content = bind.editTextAdviceContent.text.toString()
            val order = bind.editTextAdviceOrder.text.toString()
            val tag = bind.editTextAdviceTag.text.toString()

            viewModel.saveAdvice(title, content, order, tag)
        }

        bind.buttonCancelAdvice.setOnClickListener {
            dismiss()
        }

         // Optional: Delete button listener
         bind.buttonDeleteAdvice.setOnClickListener {
             showDeleteConfirmationDialog()
         }
    }

    private fun observeViewModel(bind: uz.oybek.ozamiz.databinding.DialogAddEditFoodAdviceBinding) {
        viewModel.uiState.observe(viewLifecycleOwner) { state ->
            bind.progressBarSaveAdvice.isVisible = state.isSaving
            bind.layoutButtons.isVisible = !state.isSaving
            dialog?.setCancelable(!state.isSaving)

            bind.textViewDialogTitle.text = state.title
            bind.buttonDeleteAdvice.isVisible = state.canDelete // Show delete button if editing

            // Populate fields if editing
            if (state.adviceToEdit != null && bind.editTextAdviceTitle.text.isNullOrEmpty()) {
                bind.editTextAdviceTitle.setText(state.adviceToEdit.title ?: "")
                bind.editTextAdviceContent.setText(state.adviceToEdit.content)
                bind.editTextAdviceOrder.setText(state.adviceToEdit.order.toString())
                bind.editTextAdviceTag.setText(state.adviceToEdit.tag ?: "")
            }

            state.errorMessage?.let {
                showToast(it)
                viewModel.clearError()
            }
        }

        viewModel.navigationEvent.observe(viewLifecycleOwner) { event ->
            event?.getContentIfNotHandled()?.let { navEvent ->
                 when (navEvent) {
                     is AddEditFoodAdviceNavigationEvent.ShowToast -> showToast(navEvent.message)
                     is AddEditFoodAdviceNavigationEvent.DismissDialog -> dismiss()
                 }
            }
        }
    }

     private fun showDeleteConfirmationDialog() {
         if (!isAdded) return
         AlertDialog.Builder(requireContext())
             .setTitle("Maslahatni o'chirish")
             .setMessage("Haqiqatan ham bu maslahatni o'chirmoqchimisiz?")
             .setPositiveButton(R.string.yes_delete) { dialog, _ ->
                 viewModel.deleteAdvice()
                 dialog.dismiss()
             }
             .setNegativeButton(R.string.cancel, null)
             .show()
     }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}