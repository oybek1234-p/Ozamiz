package uz.oybek.ozamiz.ui.discover // Yoki mos paket

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.gson.Gson
import kotlinx.coroutines.launch
import uz.oybek.ozamiz.data.DataWrapper
import uz.oybek.ozamiz.data.workouts.WorkoutCategory
import uz.oybek.ozamiz.data.workouts.WorkoutRepository
import uz.oybek.ozamiz.handleException
import uz.oybek.ozamiz.ui.getinfo.Event

sealed class AdminCategoryNavigation {
    data class ShowToast(val message: String) : AdminCategoryNavigation()
    object NavigateBack : AdminCategoryNavigation()
}

class AddEditWorkoutCategoryViewModel : ViewModel() {

    private val repository = WorkoutRepository()
    private val gson = Gson()

    private val _uiState = MutableLiveData(AddEditWorkoutCategoryUiState())
    val uiState: LiveData<AddEditWorkoutCategoryUiState> = _uiState

    private val _navigationEvent = MutableLiveData<Event<AdminCategoryNavigation>>()
    val navigationEvent: LiveData<Event<AdminCategoryNavigation>> = _navigationEvent

    private var currentCategory = WorkoutCategory()
    private var initialCategoryJson: String? = null

    fun initialize(categoryJson: String?) {
        initialCategoryJson = categoryJson
        if (categoryJson != null) {
            try {
                currentCategory = gson.fromJson(categoryJson, WorkoutCategory::class.java)
                _uiState.value = AddEditWorkoutCategoryUiState(
                    categoryToEdit = currentCategory,
                    title = "Kategoriyani Tahrirlash",
                    canDelete = true
                )
            } catch (e: Exception) {
                handleException(e, "CategoryJsonParseErrorAdmin")
                _uiState.value = AddEditWorkoutCategoryUiState(errorMessage = "Kategoriya ma'lumotlarini o'qishda xatolik")
            }
        } else {
            currentCategory = WorkoutCategory()
            _uiState.value = AddEditWorkoutCategoryUiState(title = "Yangi Kategoriya Qo'shish")
        }
    }

    fun saveCategory(name: String, orderStr: String) {
        if (name.isBlank()) {
            _uiState.value = _uiState.value?.copy(errorMessage = "Kategoriya nomini kiriting")
            return
        }
        val order = orderStr.toIntOrNull()
        if (order == null || order < 0) {
            _uiState.value = _uiState.value?.copy(errorMessage = "Tartib raqamini to'g'ri kiriting")
            return
        }

        _uiState.value = _uiState.value?.copy(isSaving = true, errorMessage = null)
        currentCategory = currentCategory.copy(name = name.trim(), order = order)

        viewModelScope.launch {
            val result = if (currentCategory.id.isBlank()) {
                repository.addWorkoutCategory(currentCategory)
            } else {
                repository.updateWorkoutCategory(currentCategory)
            }

            when (result) {
                is DataWrapper.Success -> {
                    val message = if (_uiState.value?.categoryToEdit == null) "Kategoriya qo'shildi" else "Kategoriya yangilandi"
                    _navigationEvent.postValue(Event(AdminCategoryNavigation.ShowToast(message)))
                    _navigationEvent.postValue(Event(AdminCategoryNavigation.NavigateBack))
                }
                is DataWrapper.Error -> {
                    _uiState.postValue(_uiState.value?.copy(isSaving = false, errorMessage = result.error))
                }
                is DataWrapper.Loading -> {}
            }
        }
    }

     fun deleteCategory() {
         if (currentCategory.id.isBlank()) return
         _uiState.value = _uiState.value?.copy(isSaving = true, errorMessage = null)
         viewModelScope.launch {
             when(val result = repository.deleteWorkoutCategory(currentCategory.id)) {
                 is DataWrapper.Success -> {
                      _navigationEvent.postValue(Event(AdminCategoryNavigation.ShowToast("Kategoriya o'chirildi")))
                      _navigationEvent.postValue(Event(AdminCategoryNavigation.NavigateBack))
                 }
                 is DataWrapper.Error -> {
                     _uiState.postValue(_uiState.value?.copy(isSaving = false, errorMessage = "O'chirishda xatolik: ${result.error}"))
                 }
                  is DataWrapper.Loading -> {}
             }
         }
     }

     fun hasChanges(name: String, orderStr: String): Boolean {
         val initial = try {
             if (initialCategoryJson != null) gson.fromJson(initialCategoryJson, WorkoutCategory::class.java) else WorkoutCategory()
         } catch (e: Exception) { WorkoutCategory() }

         val current = currentCategory.copy(name = name.trim(), order = orderStr.toIntOrNull() ?: 0)
         return current != initial
     }

    fun clearError() {
        _uiState.value = _uiState.value?.copy(errorMessage = null)
    }
}