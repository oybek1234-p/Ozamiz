package uz.oybek.ozamiz.ui.feature.lessons.admin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import uz.oybek.ozamiz.data.lessons.LessonRepository
import uz.oybek.ozamiz.data.lessons.model.Quiz
import uz.oybek.ozamiz.data.lessons.model.Screen

/**
 * Topic Detail uchun ViewModel - Screens va Quizzes ni boshqaradi
 */
class AdminTopicDetailViewModel : ViewModel() {

    private val _screensState = MutableStateFlow(ScreensUiState())
    val screensState: StateFlow<ScreensUiState> = _screensState.asStateFlow()

    private val _quizzesState = MutableStateFlow(QuizzesUiState())
    val quizzesState: StateFlow<QuizzesUiState> = _quizzesState.asStateFlow()

    private var currentTopicId: String = ""

    fun setTopicId(topicId: String) {
        currentTopicId = topicId
        loadScreens()
        loadQuizzes()
    }

    // =============== SCREENS ===============

    fun loadScreens() {
        if (currentTopicId.isEmpty()) return
        viewModelScope.launch {
            _screensState.value = _screensState.value.copy(isLoading = true)
            LessonRepository.getScreens(currentTopicId).onSuccess { screens ->
                _screensState.value = _screensState.value.copy(
                    isLoading = false,
                    screens = screens.sortedBy { it.order }
                )
            }.onFailure {
                _screensState.value = _screensState.value.copy(isLoading = false, error = it.message)
            }
        }
    }

    fun addScreen(screen: Screen) {
        viewModelScope.launch {
            _screensState.value = _screensState.value.copy(isLoading = true)
            val screenWithTopic = screen.copy(topicId = currentTopicId)
            LessonRepository.addScreen(screenWithTopic).onSuccess {
                loadScreens()
            }.onFailure {
                _screensState.value = _screensState.value.copy(isLoading = false, error = it.message)
            }
        }
    }

    fun updateScreen(screen: Screen) {
        viewModelScope.launch {
            _screensState.value = _screensState.value.copy(isLoading = true)
            LessonRepository.updateScreen(screen).onSuccess {
                loadScreens()
            }.onFailure {
                _screensState.value = _screensState.value.copy(isLoading = false, error = it.message)
            }
        }
    }

    fun deleteScreen(screenId: String) {
        viewModelScope.launch {
            _screensState.value = _screensState.value.copy(isLoading = true)
            LessonRepository.deleteScreen(screenId).onSuccess {
                loadScreens()
            }.onFailure {
                _screensState.value = _screensState.value.copy(isLoading = false, error = it.message)
            }
        }
    }

    // =============== QUIZZES ===============

    fun loadQuizzes() {
        if (currentTopicId.isEmpty()) return
        viewModelScope.launch {
            _quizzesState.value = _quizzesState.value.copy(isLoading = true)
            LessonRepository.getQuizzes(currentTopicId).onSuccess { quizzes ->
                _quizzesState.value = _quizzesState.value.copy(
                    isLoading = false,
                    quizzes = quizzes
                )
            }.onFailure {
                _quizzesState.value = _quizzesState.value.copy(isLoading = false, error = it.message)
            }
        }
    }

    fun addQuiz(quiz: Quiz) {
        viewModelScope.launch {
            _quizzesState.value = _quizzesState.value.copy(isLoading = true)
            val quizWithTopic = quiz.copy(topicId = currentTopicId)
            LessonRepository.addQuiz(quizWithTopic).onSuccess {
                loadQuizzes()
            }.onFailure {
                _quizzesState.value = _quizzesState.value.copy(isLoading = false, error = it.message)
            }
        }
    }

    fun updateQuiz(quiz: Quiz) {
        viewModelScope.launch {
            _quizzesState.value = _quizzesState.value.copy(isLoading = true)
            LessonRepository.updateQuiz(quiz).onSuccess {
                loadQuizzes()
            }.onFailure {
                _quizzesState.value = _quizzesState.value.copy(isLoading = false, error = it.message)
            }
        }
    }

    fun deleteQuiz(quizId: String) {
        viewModelScope.launch {
            _quizzesState.value = _quizzesState.value.copy(isLoading = true)
            LessonRepository.deleteQuiz(quizId).onSuccess {
                loadQuizzes()
            }.onFailure {
                _quizzesState.value = _quizzesState.value.copy(isLoading = false, error = it.message)
            }
        }
    }

    fun clearScreensError() {
        _screensState.value = _screensState.value.copy(error = null)
    }

    fun clearQuizzesError() {
        _quizzesState.value = _quizzesState.value.copy(error = null)
    }
}

data class ScreensUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val screens: List<Screen> = emptyList()
)

data class QuizzesUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val quizzes: List<Quiz> = emptyList()
)
