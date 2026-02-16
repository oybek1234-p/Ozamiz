package uz.oybek.ozamiz.ui.feature.lessons

import android.content.Context
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
 * ViewModel for user-facing lessons home
 */
class LessonsHomeViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(LessonsHomeUiState())
    val uiState: StateFlow<LessonsHomeUiState> = _uiState.asStateFlow()

    private var completedTopicIds: Set<String> = emptySet()

    fun loadData(context: Context) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            
            // Load completed topics from SharedPreferences
            completedTopicIds = loadCompletedTopics(context)
            
            val sectionsResult = LessonRepository.getSections()
            val topicsResult = LessonRepository.getAllTopics()
            
            sectionsResult.onSuccess { sections ->
                topicsResult.onSuccess { topics ->
                    val items = buildListItems(sections, topics)
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        items = items
                    )
                    
                    // Prefetch content for all topics (or just unlocked ones)
                    // Request caching for instant subsequent reads
                    launch {
                        topics.forEach { topic ->
                            LessonRepository.prefetchTopicContent(topic.id)
                        }
                    }
                }
            }.onFailure {
                _uiState.value = _uiState.value.copy(isLoading = false, error = it.message)
            }
        }
    }

    private fun buildListItems(sections: List<Section>, topics: List<Topic>): List<LessonListItem> {
        val items = mutableListOf<LessonListItem>()
        var isFirstGlobalTopic = true
        var isPreviousTopicCompleted = true 

        sections.sortedBy { it.order }.forEach { section ->
            items.add(LessonListItem.SectionHeader(section))
            
            val sectionTopics = topics.filter { it.sectionId == section.id }
                .sortedBy { it.order }
            
            sectionTopics.forEach { topic ->
                    val isCompleted = completedTopicIds.contains(topic.id)
                    
                    // Unlocked if: It's the first topic OR it's already completed OR the previous one is completed
                    val isLocked = !isFirstGlobalTopic && !isCompleted && !isPreviousTopicCompleted
                    
                    items.add(LessonListItem.TopicItem(
                        topic = topic,
                        isCompleted = isCompleted,
                        isLocked = isLocked
                    ))
                    
                    isFirstGlobalTopic = false
                    isPreviousTopicCompleted = isCompleted
                }
        }
        return items
    }

    fun markTopicCompleted(context: Context, topicId: String) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val currentSet = prefs.getStringSet(KEY_COMPLETED_TOPICS, emptySet())?.toMutableSet() ?: mutableSetOf()
        currentSet.add(topicId)
        prefs.edit().putStringSet(KEY_COMPLETED_TOPICS, currentSet).apply()
        completedTopicIds = currentSet
        
        // Refresh the list
        loadData(context)
    }

    private fun loadCompletedTopics(context: Context): Set<String> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getStringSet(KEY_COMPLETED_TOPICS, emptySet()) ?: emptySet()
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    companion object {
        private const val PREFS_NAME = "lessons_prefs"
        private const val KEY_COMPLETED_TOPICS = "completed_topic_ids"
    }
}

data class LessonsHomeUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val items: List<LessonListItem> = emptyList()
)

sealed class LessonListItem {
    data class SectionHeader(val section: Section) : LessonListItem()
    data class TopicItem(
        val topic: Topic,
        val isCompleted: Boolean = false,
        val isLocked: Boolean = false
    ) : LessonListItem()
}
