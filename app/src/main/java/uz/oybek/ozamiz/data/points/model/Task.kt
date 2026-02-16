package uz.oybek.ozamiz.data.points.model

import androidx.annotation.Keep

@Keep
data class Task(
    val id: String,
    val title: String,
    val info: String,
    val difficulty: TaskDifficulty,
    val points: Int,
    val timeOfDay: TimeOfDay
)

@Keep
enum class TaskDifficulty {
    EASY,
    MEDIUM,
    HARD,
    EXTREME
}

@Keep
enum class TimeOfDay {
    MORNING,
    AFTERNOON,
    EVENING
}
