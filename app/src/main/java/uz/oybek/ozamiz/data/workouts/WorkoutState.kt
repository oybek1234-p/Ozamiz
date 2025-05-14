package uz.oybek.ozamiz.data.workouts

enum class WorkoutState {
    PREPARING,
    WORKING,
    RESTING,
    PAUSED,
    FINISHED
}

data class WorkoutExecutionState(
    val workoutId: String = "",
    val currentExerciseIndex: Int = 0,
    val currentExercise: Exercise? = null,
    val nextExercise: Exercise? = null,
    val state: WorkoutState = WorkoutState.PREPARING,
    val timeRemainingSeconds: Int = 10,
    val timerProgress: Int = 100, // YANGI: 0-100 oralig'ida progress
    val totalExercises: Int = 0,
    val completedExercises: Int = 0,
    val totalWorkoutTimeSeconds: Int = 0, // Buni ham hisoblashimiz mumkin
    val elapsedWorkoutTimeSeconds: Int = 0,
    val estimatedCaloriesBurned: Int = 0, // Buni ham hisoblashimiz mumkin
    val workout: Workout? = null // Workout obyektini ham saqlash foydali bo'lishi mumkin
)