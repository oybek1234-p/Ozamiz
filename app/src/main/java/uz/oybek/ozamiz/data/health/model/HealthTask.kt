package uz.oybek.ozamiz.data.health.model

import androidx.annotation.Keep

@Keep
data class HealthTask(
    val id: String,
    val emoji: String,
    val title: String,
    val benefit: String,
    val description: String,
    val difficulty: Int
)

@Keep
data class HealthTaskStatus(
    val task: HealthTask,
    var isCompleted: Boolean = false,
    var totalCompletions: Int = 0
)
