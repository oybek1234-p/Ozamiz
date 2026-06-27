package uz.oybek.ozamiz.ui.feature.tasks

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import uz.oybek.ozamiz.data.points.LevelManager
import uz.oybek.ozamiz.data.points.model.Level
import uz.oybek.ozamiz.data.points.PointsRepository
import uz.oybek.ozamiz.data.points.TaskRepository
import uz.oybek.ozamiz.data.points.model.Task
import uz.oybek.ozamiz.data.user.UserRepository
import uz.oybek.ozamiz.data.user.model.User
import java.time.LocalDate
import java.time.format.DateTimeFormatter

data class DailyTaskUiState(
    val tasks: List<Task> = emptyList(),
    val completedTaskIds: List<String> = emptyList(),
    val currentPoints: Int = 0,
    val currentLevel: Level = LevelManager.getCurrentLevel(0),
    val nextLevel: Level? = null,
    val levelProgress: Float = 0f,
    val streak: Int = 0,
    val multiplier: Float = 1.0f,
    val isLoading: Boolean = true
)

class TasksViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(DailyTaskUiState())
    val uiState: StateFlow<DailyTaskUiState> = _uiState.asStateFlow()

    init {
        UserRepository.observeCurrentUser { user ->
             if (user != null && user.valid) {
                 updateState(user)
             }
        }
    }

    private fun updateState(user: User) {
        val today = LocalDate.now().format(DateTimeFormatter.ISO_DATE)
        
        // If stored tasks date is old, clear completed tasks in UI logic 
        // (Data layer handles clear on update, but here we just read)
        val completedIds = if (user.lastTaskDate == today) user.completedTaskIds else emptyList()
        
        val tasks = TaskRepository.getTasksForUser(user)
        val level = LevelManager.getCurrentLevel(user.scorePoints)
        val nextLevel = LevelManager.getNextLevel(user.scorePoints)
        val progress = LevelManager.getProgressToNextLevel(user.scorePoints)
        val streak = user.streakInfo?.currentStreak ?: 0
        val multiplier = PointsRepository.calculateMultiplier(streak)

        _uiState.update {
            it.copy(
                tasks = tasks,
                completedTaskIds = completedIds,
                currentPoints = user.scorePoints,
                currentLevel = level,
                nextLevel = nextLevel,
                levelProgress = progress,
                streak = streak,
                multiplier = multiplier,
                isLoading = false
            )
        }
    }

    fun onTaskChecked(task: Task) {
        viewModelScope.launch {
            PointsRepository.completeTask(task.id)
            // UI update happens via observeCurrentUser
        }
    }
}
