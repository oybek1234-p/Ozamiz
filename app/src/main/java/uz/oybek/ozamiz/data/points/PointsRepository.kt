package uz.oybek.ozamiz.data.points

import uz.oybek.ozamiz.data.streak.StreakRepository
import uz.oybek.ozamiz.data.user.UserRepository
import uz.oybek.ozamiz.data.user.model.User
import java.time.LocalDate
import java.time.format.DateTimeFormatter

object PointsRepository {

    fun addPoints(amount: Int) {
        val currentUser = UserRepository.user
        if (!currentUser.valid) return

        val streak = currentUser.streakInfo?.currentStreak ?: 0
        val multiplier = calculateMultiplier(streak)
        val finalPoints = (amount * multiplier).toInt()

        UserRepository.updateUser { user ->
            user.scorePoints += finalPoints
        }
        
        // Check level up (UI can observe user.scorePoints)
    }

    fun completeTask(taskId: String) {
        val currentUser = UserRepository.user
        if (!currentUser.valid) return

        val today = LocalDate.now().format(DateTimeFormatter.ISO_DATE)
        
        // If it's a new day, we might need to clear old completed tasks, 
        // but User model logic should handle "is todays task completed" by checking date?
        // Actually, we store `completedTaskIds`. We should clear them if date changed?
        // Or better: store `lastTaskDate`. If `lastTaskDate` != today, clear list first.
        
        UserRepository.updateUser { user ->
            if (user.lastTaskDate != today) {
                user.lastTaskDate = today
                user.completedTaskIds = emptyList()
            }
            
            if (!user.completedTaskIds.contains(taskId)) {
                val newCompleted = user.completedTaskIds.toMutableList()
                newCompleted.add(taskId)
                user.completedTaskIds = newCompleted
                
                // Add points
                val task = TaskRepository.getTaskById(taskId)
                if (task != null) {
                    val streak = user.streakInfo?.currentStreak ?: 0
                    val multiplier = calculateMultiplier(streak)
                    user.scorePoints += (task.points * multiplier).toInt()
                }
                
                // Check for "All Tasks Completed" Bonus
                val todayTasks = TaskRepository.getTasksForUser(user) 
                val allTaskIds = todayTasks.map { it.id }
                if (newCompleted.containsAll(allTaskIds) && !user.completedTaskIds.containsAll(allTaskIds)) {
                     // All done! Bonus +10
                     user.scorePoints += 10
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
