package uz.oybek.ozamiz.ui.feature.lessons

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import uz.oybek.ozamiz.data.lessons.LessonRepository
import uz.oybek.ozamiz.data.lessons.model.Quiz

/**
 * ViewModel for QuizFragment
 */
class QuizViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(QuizUiState())
    val uiState: StateFlow<QuizUiState> = _uiState.asStateFlow()

    fun loadQuizzes(topicId: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            
            LessonRepository.getQuizzes(topicId).onSuccess { quizzes ->
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    quizzes = quizzes,
                    currentIndex = 0,
                    currentQuiz = quizzes.firstOrNull()
                )
            }.onFailure {
                _uiState.value = _uiState.value.copy(isLoading = false, error = it.message)
            }
        }
    }

    fun checkAnswer(selectedIndex: Int) {
        val currentQuiz = _uiState.value.currentQuiz ?: return
        
        val isCorrect = selectedIndex == currentQuiz.correctIndex
        
        var newCorrectCount = _uiState.value.correctAnswerCount
        if (isCorrect) {
            newCorrectCount++
        }
        
        var newLives = _uiState.value.lives
        if (!isCorrect) {
            newLives = (newLives - 1).coerceAtLeast(0)
        }
        
        _uiState.value = _uiState.value.copy(
            lastAnswerCorrect = isCorrect,
            lives = newLives,
            correctAnswerCount = newCorrectCount,
            selectedOptionIndex = selectedIndex
        )
    }

    fun goToPrevious() {
        val newIndex = _uiState.value.currentIndex - 1
        if (newIndex >= 0) {
            _uiState.value = _uiState.value.copy(
                currentIndex = newIndex,
                currentQuiz = _uiState.value.quizzes[newIndex],
                selectedOptionIndex = null,
                lastAnswerCorrect = null
            )
        }
    }

    fun goToNext() {
        val newIndex = _uiState.value.currentIndex + 1
        if (newIndex < _uiState.value.quizzes.size) {
            _uiState.value = _uiState.value.copy(
                currentIndex = newIndex,
                currentQuiz = _uiState.value.quizzes[newIndex],
                selectedOptionIndex = null,
                lastAnswerCorrect = null
            )
        } else {
            // Finished
            val total = _uiState.value.quizzes.size
            val correct = _uiState.value.correctAnswerCount
            val percent = if (total > 0) (correct.toFloat() / total.toFloat()) * 100f else 0f
            val isPassed = percent >= 50f // 50% or more
            
             _uiState.value = _uiState.value.copy(
                 isCompleted = true,
                 scorePercentage = percent.toInt(),
                 isPassed = isPassed
             )
        }
    }

    fun clearAnswerResult() {
        _uiState.value = _uiState.value.copy(lastAnswerCorrect = null)
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
}

data class QuizUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val quizzes: List<Quiz> = emptyList(),
    val currentIndex: Int = 0,
    val currentQuiz: Quiz? = null,
    val selectedOptionIndex: Int? = null,
    val lastAnswerCorrect: Boolean? = null,
    val lives: Int = 3,
    val correctAnswerCount: Int = 0,
    val scorePercentage: Int = 0,
    val isPassed: Boolean = false,
    val isCompleted: Boolean = false
)
