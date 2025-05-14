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
}

class AddEditWorkoutViewModel : ViewModel() {

    private val repository = WorkoutRepository()
    private val gson = Gson()
    private val TAG = "AddEditWorkoutVM"

    private val _uiState = MutableStateFlow(AddEditWorkoutUiState())
    val uiState: StateFlow<AddEditWorkoutUiState> = _uiState.asStateFlow()

    private val _navigationEvent = MutableLiveData<Event<AdminWorkoutNavigation>>()
    val navigationEvent: LiveData<Event<AdminWorkoutNavigation>> = _navigationEvent

    // Holds the workout data being actively modified by user input
    private var currentWorkoutData = Workout()
    private var initialWorkoutState: Workout? = null // To compare for changes

    var selectedImageUri: Uri? = null // Only for NEWLY selected image

    private var loadDataJob: Job? = null

    fun initialize(workoutJson: String?) {
        loadDataJob?.cancel() // Cancel any previous loading

        if (workoutJson != null) {
            // --- EDITING MODE ---
            try {
                initialWorkoutState = gson.fromJson(workoutJson, Workout::class.java)
                currentWorkoutData = initialWorkoutState!!.copy() // Start editing from a copy

                _uiState.update {
                    it.copy(
                        isLoading = true, // Start loading categories/exercises
                        workoutToEdit = initialWorkoutState, // Keep original for reference
                        selectedCategoryIds = currentWorkoutData.categoryIds ?: emptyList(),
                        selectedExerciseIds = currentWorkoutData.exerciseIds,
                        title = "Workoutni Tahrirlash",
                        canDelete = true,
                        imageUriToDisplay = currentWorkoutData.imageUrl?.let { Uri.parse(it) },
                        saveSuccess = false, // Reset flags
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
            // --- ADDING MODE ---
            initialWorkoutState = null // No initial state to compare against initially
            currentWorkoutData = Workout() // Start with a blank workout
            _uiState.update {
                AddEditWorkoutUiState(isLoading = true) // Reset to default loading state
            }
        }
        selectedImageUri = null // Reset image selection on init
        loadAuxiliaryData() // Load categories and exercises
    }

    private fun loadAuxiliaryData() {
        _uiState.update { it.copy(isLoading = true) } // Ensure loading is true
        loadDataJob = viewModelScope.launch {
            // Use async/awaitAll for parallel loading
            val categoriesDeferred = async { repository.getWorkoutCategoriesFlow().first { it !is DataWrapper.Loading } } // Get first result (cache or network)
            val exercisesDeferred = async { repository.getAllExercises() }

            val categoriesResult = categoriesDeferred.await()
            val exercisesResult = exercisesDeferred.await()

            var categoriesData: List<WorkoutCategory> = emptyList()
            var exercisesData: List<Exercise> = emptyList()
            var combinedError: String? = null

            when(categoriesResult) {
                is DataWrapper.Success -> categoriesData = categoriesResult.data
                is DataWrapper.Error -> combinedError = "Kategoriya xatosi: ${categoriesResult.error}"
                else -> {} // Should not happen with .first{}
            }
            when(exercisesResult) {
                is DataWrapper.Success -> exercisesData = exercisesResult.data
                is DataWrapper.Error -> combinedError = "${combinedError ?: ""}\nMashq xatosi: ${exercisesResult.error}".trim()
                else -> {}
            }

            _uiState.update {
                it.copy(
                    isLoading = false, // Finish loading
                    allCategories = categoriesData,
                    allExercises = exercisesData,
                    errorMessage = combinedError ?: it.errorMessage // Append errors or keep existing
                )
            }
        }
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

    // These update both 'currentWorkoutData' and the UI state for immediate feedback
    fun updateSelectedCategories(ids: List<String>) {
        currentWorkoutData = currentWorkoutData.copy(categoryIds = ids.ifEmpty { null })
        _uiState.update { it.copy(selectedCategoryIds = ids) }
    }

    fun updateSelectedExercises(ids: List<String>) {
        currentWorkoutData = currentWorkoutData.copy(exerciseIds = ids)
        _uiState.update { it.copy(selectedExerciseIds = ids) }
    }

    // Call this when an image is selected or removed in the fragment
    fun updateSelectedImageUri(uri: Uri?) {
        selectedImageUri = uri
        _uiState.update { it.copy(imageUriToDisplay = uri ?: initialWorkoutState?.imageUrl?.let { Uri.parse(it) }) }
    }


    fun saveWorkout() {
        // Validation using currentWorkoutData
        if (currentWorkoutData.name.isBlank()) {
            _navigationEvent.postValue(Event(AdminWorkoutNavigation.ShowToast("Workout nomini kiriting")))
            return
        }
        if (currentWorkoutData.exerciseIds.isEmpty()) {
            _navigationEvent.postValue(Event(AdminWorkoutNavigation.ShowToast("Kamida bitta mashq tanlang")))
            return
        }
        if (currentWorkoutData.categoryIds.isNullOrEmpty()) {
            _navigationEvent.postValue(Event(AdminWorkoutNavigation.ShowToast("Kamida bitta kategoriya tanlang")))
            return
        }
        // Check if an image exists (either newly selected or from the original workout)
        if (selectedImageUri == null && currentWorkoutData.imageUrl.isNullOrEmpty()) {
            _navigationEvent.postValue(Event(AdminWorkoutNavigation.ShowToast("Workout rasmini tanlang")))
            return
        }

        _uiState.update { it.copy(isSaving = true, errorMessage = null) }

        viewModelScope.launch {
            // Determine if adding or updating based on currentWorkoutData's ID
            val isUpdating = currentWorkoutData.id.isNotBlank()
            val result: DataWrapper<*> = if (!isUpdating) {
                // Add new workout
                repository.addWorkout(currentWorkoutData, selectedImageUri)
            } else {
                // Update existing workout
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
            // Ensure saving state is reset even if navigation doesn't happen immediately
            if (_uiState.value.isSaving) {
                _uiState.update { it.copy(isSaving = false)}
            }
        }
    }

    fun deleteWorkout() {
        val workoutIdToDelete = currentWorkoutData.id // Use ID from the data being edited
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