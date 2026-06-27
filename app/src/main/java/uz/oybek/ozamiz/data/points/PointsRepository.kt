package uz.oybek.ozamiz.data.points

import uz.oybek.ozamiz.data.user.UserRepository
import java.time.LocalDate
import java.time.format.DateTimeFormatter

object PointsRepository {

    fun completeTask(taskId: String) {
        val currentUser = UserRepository.user
        if (!currentUser.valid) return

        val today = LocalDate.now().format(DateTimeFormatter.ISO_DATE)

        UserRepository.updateUser { user ->
            if (user.lastTaskDate != today) {
                user.lastTaskDate = today
                user.completedTaskIds = emptyList()
            }

            if (!user.completedTaskIds.contains(taskId)) {
                val newCompleted = user.completedTaskIds.toMutableList()
                newCompleted.add(taskId)
                user.completedTaskIds = newCompleted

                val task = TaskRepository.getTaskById(taskId)
                if (task != null) {
                    val streak = user.streakInfo?.currentStreak ?: 0
                    val multiplier = calculateMultiplier(streak)
                    user.scorePoints += (task.points * multiplier).toInt()
                }
                val todayTasks = TaskRepository.getTasksForUser(user)
                val allTaskIds = todayTasks.map { it.id }
                if (newCompleted.containsAll(allTaskIds) && !user.completedTaskIds.containsAll(
                        allTaskIds
                    )
                ) { user.scorePoints += 10
                }
            }
        }
    }

    fun calculateMultiplier(streakDays: Int): Float {
        return when {
            streakDays >= 30 -> 1.5f
            streakDays >= 7 -> 1.2f
            else -> 1.0f
        }
    }
}
