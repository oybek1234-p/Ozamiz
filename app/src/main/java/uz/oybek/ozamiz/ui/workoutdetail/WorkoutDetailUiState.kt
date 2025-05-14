// C:\Users\User\AndroidStudioProjects\Ozamiz\app\src\main\java\uz\oybek\ozamiz\ui\workoutdetail\WorkoutDetailUiState.kt
package uz.oybek.ozamiz.ui.workoutdetail

import uz.oybek.ozamiz.data.workouts.Exercise
import uz.oybek.ozamiz.data.workouts.Workout

data class WorkoutDetailUiState(
    // isLoading endi faqat mashqlar uchun
    val isLoadingExercises: Boolean = true,
    val errorMessage: String? = null,
    val workout: Workout? = null, // Bu darhol kelishi mumkin (keshdan)
    val exercises: List<Exercise> = emptyList(),
    val isPremiumUser: Boolean = false,
    val canStartWorkout: Boolean = false
)

// Navigation Events o'zgarishsiz
sealed class WorkoutDetailNavigationEvent {
    data class NavigateToWorkoutExecution(val workoutId: String, val exercises: List<Exercise>) : WorkoutDetailNavigationEvent()
    data class NavigateToExerciseDetail(val exercise: Exercise) : WorkoutDetailNavigationEvent()
    data class NavigateBackWithMessage(val message: String) : WorkoutDetailNavigationEvent()
}