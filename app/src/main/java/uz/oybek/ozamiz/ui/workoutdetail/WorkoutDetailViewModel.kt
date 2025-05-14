package uz.oybek.ozamiz.ui.workoutdetail

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import uz.oybek.ozamiz.data.DataWrapper
import uz.oybek.ozamiz.data.LocalUser
import uz.oybek.ozamiz.data.workouts.Exercise
import uz.oybek.ozamiz.data.workouts.Workout
import uz.oybek.ozamiz.data.workouts.WorkoutRepository
import uz.oybek.ozamiz.ui.getinfo.Event

class WorkoutDetailViewModel : ViewModel() {

    private val workoutRepository = WorkoutRepository()

    // Boshlang'ich holat: workout null, isLoadingExercises = true
    private val _uiState = MutableStateFlow(WorkoutDetailUiState(isLoadingExercises = true))
    val uiState: StateFlow<WorkoutDetailUiState> = _uiState.asStateFlow()

    private val _navigationEvent = MutableLiveData<Event<WorkoutDetailNavigationEvent>>()
    val navigationEvent: LiveData<Event<WorkoutDetailNavigationEvent>> = _navigationEvent

    private var currentWorkoutId: String? = null
    private var loadWorkoutJob: Job? = null
    private var loadExercisesJob: Job? = null

    fun loadWorkoutDetails(workoutId: String) {
        currentWorkoutId = workoutId
        loadWorkoutJob?.cancel()
        loadExercisesJob?.cancel()

        _uiState.update {
            WorkoutDetailUiState(
                isLoadingExercises = true, isPremiumUser = LocalUser.user.premium
            )
        }

        loadWorkoutJob = viewModelScope.launch {
            when (val workoutResult = workoutRepository.getWorkoutById(workoutId, cache = true)) {
                is DataWrapper.Success -> {
                    val workout = workoutResult.data
                    _uiState.update {
                        it.copy(workout = workout)
                    }
                    loadExercisesForWorkout(workout)
                }

                is DataWrapper.Error -> {
                    _uiState.update {
                        it.copy(
                            isLoadingExercises = false, // Xatolik bo'lsa mashq yuklashni to'xtatamiz
                            errorMessage = workoutResult.error, workout = null
                        )
                    }
                }

                is DataWrapper.Loading -> {
                    // Bu yerda alohida workout uchun loading ko'rsatish mumkin,
                    // lekin talab bo'yicha workout darhol ko'rinishi kerak.
                }
            }
        }
    }

    private fun loadExercisesForWorkout(workout: Workout) {
        if (workout.exerciseIds.isEmpty()) {
            _uiState.update {
                it.copy(
                    isLoadingExercises = false, exercises = emptyList(), canStartWorkout = false
                )
            }
            return
        }

        loadExercisesJob = viewModelScope.launch {
            when (val exercisesResult = workoutRepository.getExercisesByIds(workout.exerciseIds)) {
                is DataWrapper.Success -> {
                    val exercises = exercisesResult.data
                    val canStart =
                        exercises.isNotEmpty() && (!workout.requiredPremium || _uiState.value.isPremiumUser)
                    _uiState.update {
                        it.copy(
                            isLoadingExercises = false,
                            exercises = exercises,
                            canStartWorkout = canStart,
                            errorMessage = null
                        )
                    }
                }

                is DataWrapper.Error -> {
                    _uiState.update {
                        it.copy(
                            isLoadingExercises = false, // Xatolik bilan tugadi
                            errorMessage = exercisesResult.error,
                            exercises = emptyList(),
                            canStartWorkout = false
                        )
                    }
                }

                is DataWrapper.Loading -> {
                    // isLoadingExercises = true holati saqlanadi
                }
            }
        }
    }

    private fun checkAndUpdatePremiumStatus() {
        val isPremium = LocalUser.user.premium
        val workout = _uiState.value.workout
        val exercises = _uiState.value.exercises
        if (workout != null) {
            val canStart = exercises.isNotEmpty() && (!workout.requiredPremium || isPremium)
            if (_uiState.value.isPremiumUser != isPremium || _uiState.value.canStartWorkout != canStart) {
                _uiState.update { it.copy(isPremiumUser = isPremium, canStartWorkout = canStart) }
            }
        }
    }

    fun onStartWorkoutClicked() {
        val currentState = _uiState.value
        val workout = currentState.workout
        val exercises = currentState.exercises

        if (currentState.isLoadingExercises) {
            _uiState.update { it.copy(errorMessage = "Mashqlar hali yuklanmoqda...") }
            return
        }

        if (workout != null && exercises.isNotEmpty() && currentState.canStartWorkout) {
            _navigationEvent.value = Event(
                WorkoutDetailNavigationEvent.NavigateToWorkoutExecution(
                    workout.id, exercises
                )
            )
        } else if (workout != null && !currentState.canStartWorkout) {
            _uiState.update { it.copy(errorMessage = "Bu mashq uchun Premium kerak") }
        } else {
            _uiState.update { it.copy(errorMessage = "Mashqni boshlash uchun ma'lumotlar yetarli emas.") }
        }
    }

    fun onExerciseClicked(exercise: Exercise) {
        _navigationEvent.value =
            Event(WorkoutDetailNavigationEvent.NavigateToExerciseDetail(exercise))
    }

    fun deleteWorkout() {
        val workoutIdToDelete = currentWorkoutId ?: return
        if (_uiState.value.isLoadingExercises) return // Mashqlar yuklanayotganda o'chirmaymiz

        // O'chirish vaqtida umumiy loading ko'rsatish mumkin
        // _uiState.update { it.copy(isLoading = true) } // Agar kerak bo'lsa

        viewModelScope.launch {
            when (val result = workoutRepository.deleteWorkout(workoutIdToDelete)) {
                is DataWrapper.Success -> {
                    _navigationEvent.postValue(
                        Event(
                            WorkoutDetailNavigationEvent.NavigateBackWithMessage(
                                "Workout muvaffaqiyatli o'chirildi"
                            )
                        )
                    )
                }

                is DataWrapper.Error -> {
                    _uiState.update { it.copy(errorMessage = "O'chirishda xatolik: ${result.error}") }
                }

                is DataWrapper.Loading -> {}
            }
            // Reset loading if used: _uiState.update { it.copy(isLoading = false) }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    override fun onCleared() {
        super.onCleared()
        loadWorkoutJob?.cancel()
        loadExercisesJob?.cancel()
    }
}