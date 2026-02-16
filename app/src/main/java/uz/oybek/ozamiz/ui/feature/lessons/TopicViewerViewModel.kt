package uz.oybek.ozamiz.ui.feature.lessons

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import uz.oybek.ozamiz.data.lessons.LessonRepository
import uz.oybek.ozamiz.data.lessons.model.Screen

/**
 * ViewModel for TopicViewerFragment
 * Manages screens within a topic
 */
class TopicViewerViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(TopicViewerUiState())
    val uiState: StateFlow<TopicViewerUiState> = _uiState.asStateFlow()

    private var currentTopicId: String = ""

    fun loadScreens(topicId: String) {
        currentTopicId = topicId
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            
            LessonRepository.getScreens(topicId).onSuccess { screens ->
                val sortedScreens = screens.sortedBy { it.order }
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    screens = sortedScreens,
                    currentIndex = 0,
                    currentScreen = sortedScreens.firstOrNull()
                )
            }.onFailure {
                _uiState.value = _uiState.value.copy(isLoading = false, error = it.message)
            }
        }
    }

    fun goToNext(): Boolean {
        val newIndex = _uiState.value.currentIndex + 1
        return if (newIndex < _uiState.value.screens.size) {
            _uiState.value = _uiState.value.copy(
                currentIndex = newIndex,
                currentScreen = _uiState.value.screens[newIndex]
            )
            true
        } else {
            false // No more screens, should start quiz
        }
    }

    fun goToPrevious(): Boolean {
        val newIndex = _uiState.value.currentIndex - 1
        return if (newIndex >= 0) {
            _uiState.value = _uiState.value.copy(
                currentIndex = newIndex,
                currentScreen = _uiState.value.screens[newIndex]
            )
            true
        } else {
            false
        }
    }

    fun isFirstScreen(): Boolean = _uiState.value.currentIndex == 0
    fun isLastScreen(): Boolean = _uiState.value.currentIndex >= _uiState.value.screens.size - 1

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
}

data class TopicViewerUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val screens: List<Screen> = emptyList(),
    val currentIndex: Int = 0,
    val currentScreen: Screen? = null
)
