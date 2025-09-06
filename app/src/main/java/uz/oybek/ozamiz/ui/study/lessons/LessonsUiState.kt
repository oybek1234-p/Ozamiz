package uz.oybek.ozamiz.ui.study.lessons

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import uz.oybek.ozamiz.data.lessons.LessonModel
import uz.oybek.ozamiz.data.lessons.LessonsController

data class LessonsUiState(
    val isLoading: Boolean = false,
    val isLoadingMore: Boolean = false,
    val lessons: List<LessonModel> = emptyList(),
    val canLoadMore: Boolean = true,
    val error: String? = null
)

class LessonsViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(LessonsUiState())
    val uiState: StateFlow<LessonsUiState> = _uiState.asStateFlow()

    init {
        if (_uiState.value.lessons.isEmpty()) {
            loadLessons(isRefresh = true)
        }
    }

    fun loadLessons(isRefresh: Boolean) {
        if (_uiState.value.isLoading || (!isRefresh && _uiState.value.isLoadingMore)) return

        viewModelScope.launch {
            if (isRefresh) {
                _uiState.update { it.copy(isLoading = true, canLoadMore = true, lessons = emptyList()) }
            } else {
                _uiState.update { it.copy(isLoadingMore = true) }
            }

            val lastOrder = if (isRefresh || _uiState.value.lessons.isEmpty()) "" else _uiState.value.lessons.last().order.toString()

            LessonsController.loadLessons(lastOrder) { newLessons ->
                if (newLessons != null) {
                    _uiState.update { currentState ->
                        val currentList = if (isRefresh) mutableListOf() else currentState.lessons.toMutableList()
                        currentList.addAll(newLessons)
                        currentState.copy(
                            isLoading = false,
                            isLoadingMore = false,
                            lessons = currentList.distinctBy { it.id }.sortedBy { it.order },
                            canLoadMore = newLessons.isNotEmpty() // Agar yangi darslar kelmasa, demak oxiriga yetdik
                        )
                    }
                } else {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            isLoadingMore = false,
                            error = "Darsliklarni yuklashda xatolik"
                        )
                    }
                }
            }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
}