package uz.oybek.ozamiz.ui.discover

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.firestore.DocumentSnapshot
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import uz.oybek.ozamiz.data.DataWrapper
import uz.oybek.ozamiz.data.workouts.Video
import uz.oybek.ozamiz.data.workouts.VideoRepository
import uz.oybek.ozamiz.handleException
import uz.oybek.ozamiz.ui.getinfo.Event

sealed class VideoListNavigationEvent {
    data class NavigateToVideoDetail(val video: Video) : VideoListNavigationEvent()
    object NavigateToAddVideo : VideoListNavigationEvent()
}

class VideoListViewModel : ViewModel() {

    private val repository = VideoRepository()

    private val _uiState = MutableStateFlow(VideoListUiState())
    val uiState: StateFlow<VideoListUiState> = _uiState.asStateFlow()

    private val _navigationEvent = MutableLiveData<Event<VideoListNavigationEvent>>()
    val navigationEvent: LiveData<Event<VideoListNavigationEvent>> = _navigationEvent

    private var lastVisibleDocument: DocumentSnapshot? = null
    private var currentVideosLoadingJob: Job? = null
    private var currentCategoryIdArg: String? = null // O'ZGARISH: categoryTag -> categoryId

    // O'ZGARISH: categoryTag -> categoryId
    fun initialize(categoryId: String?, title: String) {
        if (currentCategoryIdArg == categoryId && !_uiState.value.isLoadingInitial && _uiState.value.listTitle == title) return

        currentCategoryIdArg = categoryId
        _uiState.value = VideoListUiState(listTitle = title, isLoadingInitial = true)
        currentVideosLoadingJob?.cancel()
        loadVideos(isInitialLoad = true)
    }

    fun loadVideos(isInitialLoad: Boolean, isRefresh: Boolean = false) {
        if (_uiState.value.isRefreshing && isRefresh) return
        if (!isInitialLoad && _uiState.value.isLoadingNextPage) return
        if (!isInitialLoad && !_uiState.value.canLoadMore) return

        currentVideosLoadingJob?.cancel()

        if (isRefresh) {
            lastVisibleDocument = null
            _uiState.update {
                it.copy(
                    isRefreshing = true,
                    isLoadingInitial = true,
                    canLoadMore = true,
                    videos = emptyList()
                )
            }
        } else if (isInitialLoad) {
            lastVisibleDocument = null
            _uiState.update { it.copy(isLoadingInitial = true, isLoadingNextPage = false, videos = emptyList(), canLoadMore = true) }
        } else {
            _uiState.update { it.copy(isLoadingNextPage = true) }
        }

        currentVideosLoadingJob = viewModelScope.launch {
            repository.getPaginatedVideosFlow( // O'ZGARISH: categoryTag -> categoryId
                categoryId = currentCategoryIdArg,
                lastVisible = if (isInitialLoad || isRefresh) null else lastVisibleDocument
            )
                .catch { e ->
                    handleException(e as Exception, "LoadVideoListError")
                    _uiState.update {
                        it.copy(
                            isLoadingInitial = false,
                            isLoadingNextPage = false,
                            isRefreshing = false,
                            errorMessage = "Videolarni yuklashda xatolik: ${e.localizedMessage}"
                        )
                    }
                }
                .onCompletion {
                    _uiState.update { it.copy(isLoadingInitial = false, isLoadingNextPage = false, isRefreshing = false) }
                }
                .collectLatest { result ->
                    when (result) {
                        is DataWrapper.Success -> {
                            val (newVideos, newLastVisible) = result.data
                            val currentList = if (isInitialLoad || isRefresh) mutableListOf() else _uiState.value.videos.toMutableList()
                            currentList.addAll(newVideos)
                            lastVisibleDocument = newLastVisible

                            _uiState.update {
                                it.copy(
                                    videos = currentList.distinctBy { v -> v.id }.toList(),
                                    canLoadMore = newLastVisible != null && newVideos.isNotEmpty()
                                )
                            }
                        }
                        is DataWrapper.Error -> {
                            _uiState.update { it.copy(errorMessage = result.error) }
                        }
                        is DataWrapper.Loading -> {
                        }
                    }
                }
        }
    }

    fun loadNextPage() {
        loadVideos(isInitialLoad = false)
    }

    fun onVideoClick(video: Video) {
        _navigationEvent.value = Event(VideoListNavigationEvent.NavigateToVideoDetail(video))
    }

    fun onAddVideoClick() {
        _navigationEvent.value = Event(VideoListNavigationEvent.NavigateToAddVideo)
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    override fun onCleared() {
        super.onCleared()
        currentVideosLoadingJob?.cancel()
    }
}