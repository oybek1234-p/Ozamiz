package uz.oybek.ozamiz.ui.discover

import android.net.Uri
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.gson.Gson
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import uz.oybek.ozamiz.data.DataWrapper
import uz.oybek.ozamiz.data.workouts.Exercise
import uz.oybek.ozamiz.data.workouts.Workout
import uz.oybek.ozamiz.data.workouts.WorkoutCategory
import uz.oybek.ozamiz.data.workouts.WorkoutRepository
import uz.oybek.ozamiz.handleException
import uz.oybek.ozamiz.ui.getinfo.Event

sealed class AdminWorkoutNavigation {
    data class ShowToast(val message: String) : AdminWorkoutNavigation()
    object NavigateBack : AdminWorkoutNavigation()
    object ShowExerciseSelectionDialog : AdminWorkoutNavigation()
    data class NavigateToEditExercise(val exerciseJson: String) : AdminWorkoutNavigation() // YANGI EVENT
}

class AddEditWorkoutViewModel : ViewModel() {

    private val repository = WorkoutRepository()
    private val gson = Gson()
    private val TAG = "AddEditWorkoutVM"

    private val _uiState = MutableStateFlow(AddEditWorkoutUiState())
    val uiState: StateFlow<AddEditWorkoutUiState> = _uiState.asStateFlow()

    private val _navigationEvent = MutableLiveData<Event<AdminWorkoutNavigation>>()
    val navigationEvent: LiveData<Event<AdminWorkoutNavigation>> = _navigationEvent

    private var currentWorkoutData = Workout()
    private var initialWorkoutState: Workout? = null

    var selectedImageUri: Uri? = null

    private var loadDataJob: Job? = null

    fun initialize(workoutJson: String?) {
        loadDataJob?.cancel()

        if (workoutJson != null) {
            try {
                initialWorkoutState = gson.fromJson(workoutJson, Workout::class.java)
                currentWorkoutData = initialWorkoutState!!.copy()

                _uiState.update {
                    it.copy(
                        isLoading = true,
                        workoutToEdit = initialWorkoutState,
                        selectedCategoryIds = currentWorkoutData.categoryIds ?: emptyList(),
                        selectedExerciseIds = currentWorkoutData.exerciseIds,
                        title = "Workoutni Tahrirlash",
                        canDelete = true,
                        imageUriToDisplay = currentWorkoutData.imageUrl?.let { Uri.parse(it) },
                        saveSuccess = false,
                        deleteSuccess = false,
                        isSaving = false,
                        errorMessage = null
                    )
                }
            } catch (e: Exception) {
                handleException(e, "WorkoutJsonParseErrorAdmin")
                _uiState.update { it.copy(isLoading = false, errorMessage = "Workout ma'lumotlarini o'qishda xatolik") }
                return
            }
        } else {
            initialWorkoutState = null
            currentWorkoutData = Workout()
            _uiState.update {
                AddEditWorkoutUiState(isLoading = true)
            }
        }
        selectedImageUri = null
        loadAuxiliaryData()
    }

    private fun loadAuxiliaryData(forceRefreshExercises: Boolean = false) {
        _uiState.update { it.copy(isLoading = true) }
        loadDataJob = viewModelScope.launch {
            val categoriesDeferred = async { repository.getWorkoutCategoriesFlow().first { it !is DataWrapper.Loading } }
            // Agar mashqlar ro'yxati allaqachon yuklangan bo'lsa va forceRefresh false bo'lsa, qayta yuklamaymiz
            val exercisesDeferred = if (_uiState.value.allExercises.isNotEmpty() && !forceRefreshExercises) {
                async { DataWrapper.Success(_uiState.value.allExercises) } // Mavjudini ishlatamiz
            } else {
                async { repository.getAllExercises() }
            }


            val categoriesResult = categoriesDeferred.await()
            val exercisesResult = exercisesDeferred.await()

            var categoriesData: List<WorkoutCategory> = emptyList()
            var exercisesDataList: List<Exercise> = emptyList()
            var combinedError: String? = null

            when(categoriesResult) {
                is DataWrapper.Success -> categoriesData = categoriesResult.data
                is DataWrapper.Error -> combinedError = "Kategoriya xatosi: ${categoriesResult.error}"
                else -> {}
            }
            when(exercisesResult) {
                is DataWrapper.Success -> exercisesDataList = exercisesResult.data
                is DataWrapper.Error -> combinedError = "${combinedError ?: ""}\nMashq xatosi: ${exercisesResult.error}".trim()
                else -> {}
            }

            _uiState.update {
                it.copy(
                    isLoading = false,
                    allCategories = categoriesData,
                    allExercises = exercisesDataList.sortedBy { ex -> ex.name }, // Ensure consistent order
                    errorMessage = combinedError ?: it.errorMessage
                )
            }
        }
    }

    fun refreshExercisesAfterModification() {
        loadAuxiliaryData(forceRefreshExercises = true)
    }


    fun updateName(name: String) { currentWorkoutData = currentWorkoutData.copy(name = name.trim()) }
    fun updateDescription(desc: String) { currentWorkoutData = currentWorkoutData.copy(description = desc.trim().ifBlank { null }) }
    fun updateDuration(durationStr: String) { currentWorkoutData = currentWorkoutData.copy(durationMinutes = durationStr.toIntOrNull()) }
    fun updateCalories(caloriesStr: String?) { currentWorkoutData = currentWorkoutData.copy(estimatedCaloriesBurned = caloriesStr?.toIntOrNull()) }
    fun updateDifficulty(level: String) { currentWorkoutData = currentWorkoutData.copy(difficultyLevel = level.trim().ifBlank { null }) }
    fun updateOrder(orderStr: String) { currentWorkoutData = currentWorkoutData.copy(order = orderStr.toIntOrNull() ?: 0) }
    fun updatePremium(required: Boolean) { currentWorkoutData = currentWorkoutData.copy(requiredPremium = required) }
    fun updateAdRequired(required: Boolean) { currentWorkoutData = currentWorkoutData.copy(adRequired = required) }
    fun updateTargetAreas(areas: List<String>) { currentWorkoutData = currentWorkoutData.copy(targetArea = areas.map { it.trim() }.filter { it.isNotEmpty() }.ifEmpty { null }) }
    fun updateTags(tags: List<String>) { currentWorkoutData = currentWorkoutData.copy(tags = tags.map { it.trim() }.filter { it.isNotEmpty() }.ifEmpty { null }) }

    fun updateSelectedCategories(ids: List<String>) {
        currentWorkoutData = currentWorkoutData.copy(categoryIds = ids.ifEmpty { null })
        _uiState.update { it.copy(selectedCategoryIds = ids) }
    }

    fun updateSelectedExercises(ids: List<String>) {
        // Order of selected exercises should match the order in allExercises for consistency
        val orderedSelectedIds = _uiState.value.allExercises
            .filter { ids.contains(it.id) }
            .map { it.id }
        currentWorkoutData = currentWorkoutData.copy(exerciseIds = orderedSelectedIds)
        _uiState.update { it.copy(selectedExerciseIds = orderedSelectedIds) }
    }

    fun updateSelectedImageUri(uri: Uri?) {
        selectedImageUri = uri
        _uiState.update { it.copy(imageUriToDisplay = uri ?: initialWorkoutState?.imageUrl?.let { Uri.parse(it) }) }
    }

    fun saveWorkout() {
        if (currentWorkoutData.name.isBlank()) {
            _navigationEvent.value = Event(AdminWorkoutNavigation.ShowToast("Workout nomini kiriting"))
            return
        }
        if (currentWorkoutData.exerciseIds.isEmpty()) {
            _navigationEvent.value = Event(AdminWorkoutNavigation.ShowToast("Kamida bitta mashq tanlang"))
            return
        }
        if (currentWorkoutData.categoryIds.isNullOrEmpty()) {
            _navigationEvent.value = Event(AdminWorkoutNavigation.ShowToast("Kamida bitta kategoriya tanlang"))
            return
        }
        if (selectedImageUri == null && currentWorkoutData.imageUrl.isNullOrEmpty()) {
            _navigationEvent.value = Event(AdminWorkoutNavigation.ShowToast("Workout rasmini tanlang"))
            return
        }

        _uiState.update { it.copy(isSaving = true, errorMessage = null) }

        viewModelScope.launch {
            val isUpdating = currentWorkoutData.id.isNotBlank()
            val result: DataWrapper<*> = if (!isUpdating) {
                repository.addWorkout(currentWorkoutData, selectedImageUri)
            } else {
                repository.updateWorkout(currentWorkoutData, selectedImageUri)
            }

            when (result) {
                is DataWrapper.Success -> {
                    val message = if (!isUpdating) "Workout qo'shildi" else "Workout yangilandi"
                    _navigationEvent.postValue(Event(AdminWorkoutNavigation.ShowToast(message)))
                    _navigationEvent.postValue(Event(AdminWorkoutNavigation.NavigateBack))
                }
                is DataWrapper.Error -> {
                    _uiState.update { it.copy(isSaving = false, errorMessage = result.error) }
                }
                is DataWrapper.Loading -> {}
            }
            if (_uiState.value.isSaving) {
                _uiState.update { it.copy(isSaving = false)}
            }
        }
    }

    fun deleteWorkout() {
        val workoutIdToDelete = currentWorkoutData.id
        if (workoutIdToDelete.isBlank()) {
            _uiState.update { it.copy(errorMessage = "O'chirish uchun Workout topilmadi") }
            return
        }
        _uiState.update { it.copy(isSaving = true, errorMessage = null) }
        viewModelScope.launch {
            when(val result = repository.deleteWorkout(workoutIdToDelete)) {
                is DataWrapper.Success -> {
                    _navigationEvent.postValue(Event(AdminWorkoutNavigation.ShowToast("Workout o'chirildi")))
                    _navigationEvent.postValue(Event(AdminWorkoutNavigation.NavigateBack))
                }
                is DataWrapper.Error -> {
                    _uiState.update { it.copy(isSaving = false, errorMessage = "O'chirishda xatolik: ${result.error}") }
                }
                is DataWrapper.Loading -> {}
            }
            if (_uiState.value.isSaving) {
                _uiState.update { it.copy(isSaving = false)}
            }
        }
    }

    fun triggerExerciseSelection() {
        if(_uiState.value.allExercises.isEmpty() && !_uiState.value.isLoading){
            _navigationEvent.value = Event(AdminWorkoutNavigation.ShowToast("Mashqlar ro'yxati yuklanmagan"))
            return
        }
        _navigationEvent.value = Event(AdminWorkoutNavigation.ShowExerciseSelectionDialog)
    }

    fun onEditExerciseClicked(exercise: Exercise) {
        _navigationEvent.value = Event(AdminWorkoutNavigation.NavigateToEditExercise(gson.toJson(exercise)))
    }

    fun onDeleteExerciseConfirmed(exerciseToDelete: Exercise) {
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true) } // Indicate processing
            when (val deleteResult = repository.deleteExercise(exerciseToDelete.id)) {
                is DataWrapper.Success -> {
                    val updatedAllExercises = _uiState.value.allExercises.filterNot { it.id == exerciseToDelete.id }.sortedBy { it.name }
                    val updatedSelectedIds = _uiState.value.selectedExerciseIds.filterNot { it == exerciseToDelete.id }
                    currentWorkoutData = currentWorkoutData.copy(exerciseIds = updatedSelectedIds) // Update workout data
                    _uiState.update {
                        it.copy(
                            isSaving = false,
                            allExercises = updatedAllExercises,
                            selectedExerciseIds = updatedSelectedIds
                        )
                    }
                    _navigationEvent.postValue(Event(AdminWorkoutNavigation.ShowToast("'${exerciseToDelete.name}' o'chirildi")))
                }
                is DataWrapper.Error -> {
                    _uiState.update { it.copy(isSaving = false, errorMessage = "Mashqni o'chirishda xatolik: ${deleteResult.error}") }
                }
                is DataWrapper.Loading -> {}
            }
        }
    }

    fun hasChanges(): Boolean {
        if (initialWorkoutState == null && selectedImageUri == null && currentWorkoutData == Workout()) {
            return false
        }
        if (initialWorkoutState == null) {
            return currentWorkoutData != Workout() || selectedImageUri != null
        }
        val imageChanged = selectedImageUri != null
        val dataChanged = currentWorkoutData != initialWorkoutState
        return dataChanged || imageChanged
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    override fun onCleared() {
        super.onCleared()
        loadDataJob?.cancel()
    }
}