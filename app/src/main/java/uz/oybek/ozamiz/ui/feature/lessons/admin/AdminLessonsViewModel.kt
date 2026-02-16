package uz.oybek.ozamiz.ui.feature.lessons.admin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import uz.oybek.ozamiz.data.lessons.LessonRepository
import uz.oybek.ozamiz.data.lessons.model.Section
import uz.oybek.ozamiz.data.lessons.model.Topic

/**
 * Admin Lessons uchun ViewModel
 * Sections va Topics ni boshqaradi
 */
class AdminLessonsViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(AdminLessonsUiState())
    val uiState: StateFlow<AdminLessonsUiState> = _uiState.asStateFlow()

    init {
        loadData()
    }

    fun loadData() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            
            val sectionsResult = LessonRepository.getSections()
            val topicsResult = LessonRepository.getAllTopics()
            
            sectionsResult.onSuccess { sections ->
                topicsResult.onSuccess { topics ->
                    val items = buildListItems(sections, topics)
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        items = items,
                        sections = sections,
                        topics = topics
                    )
                }
            }.onFailure {
                _uiState.value = _uiState.value.copy(isLoading = false, error = it.message)
            }
        }
    }

    private fun buildListItems(sections: List<Section>, topics: List<Topic>): List<AdminListItem> {
        val items = mutableListOf<AdminListItem>()
        sections.sortedBy { it.order }.forEach { section ->
            items.add(AdminListItem.SectionItem(section))
            topics.filter { it.sectionId == section.id }
                .sortedBy { it.order }
                .forEach { topic ->
                    items.add(AdminListItem.TopicItem(topic))
                }
        }
        return items
    }

    // =============== SECTION CRUD ===============

    fun addSection(title: String, order: Int) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            val section = Section(title = title, order = order)
            LessonRepository.addSection(section).onSuccess {
                loadData()
            }.onFailure {
                _uiState.value = _uiState.value.copy(isLoading = false, error = it.message)
            }
        }
    }

    fun updateSection(section: Section) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            LessonRepository.updateSection(section).onSuccess {
                loadData()
            }.onFailure {
                _uiState.value = _uiState.value.copy(isLoading = false, error = it.message)
            }
        }
    }

    fun deleteSection(sectionId: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            LessonRepository.deleteSection(sectionId).onSuccess {
                loadData()
            }.onFailure {
                _uiState.value = _uiState.value.copy(isLoading = false, error = it.message)
            }
        }
    }

    // =============== TOPIC CRUD ===============

    fun addTopic(topic: Topic) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            LessonRepository.addTopic(topic).onSuccess {
                loadData()
            }.onFailure {
                _uiState.value = _uiState.value.copy(isLoading = false, error = it.message)
            }
        }
    }

    fun updateTopic(topic: Topic) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            LessonRepository.updateTopic(topic).onSuccess {
                loadData()
            }.onFailure {
                _uiState.value = _uiState.value.copy(isLoading = false, error = it.message)
            }
        }
    }

    fun deleteTopic(topicId: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            LessonRepository.deleteTopic(topicId).onSuccess {
                loadData()
            }.onFailure {
                _uiState.value = _uiState.value.copy(isLoading = false, error = it.message)
            }
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
}

data class AdminLessonsUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val items: List<AdminListItem> = emptyList(),
    val sections: List<Section> = emptyList(),
    val topics: List<Topic> = emptyList()
)

sealed class AdminListItem {
    data class SectionItem(val section: Section) : AdminListItem()
    data class TopicItem(val topic: Topic) : AdminListItem()
}
