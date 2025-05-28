package uz.oybek.ozamiz.ui.discover

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.gson.Gson
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import uz.oybek.ozamiz.data.DataWrapper
import uz.oybek.ozamiz.data.workouts.Video
import uz.oybek.ozamiz.data.workouts.VideoRepository
import uz.oybek.ozamiz.handleException
import uz.oybek.ozamiz.ui.getinfo.Event

sealed class VideoDetailNavigationEvent {
    data class NavigateToEditVideo(val video: Video) : VideoDetailNavigationEvent()
    data class NavigateBackWithMessage(val message: String) : VideoDetailNavigationEvent()
}

class VideoDetailViewModel : ViewModel() {

    private val repository = VideoRepository()
    private val gson = Gson()

    private val _uiState = MutableStateFlow(VideoDetailUiState())
    val uiState: StateFlow<VideoDetailUiState> = _uiState.asStateFlow()

    private val _navigationEvent = MutableLiveData<Event<VideoDetailNavigationEvent>>()
    val navigationEvent: LiveData<Event<VideoDetailNavigationEvent>> = _navigationEvent

    private var currentVideoId: String? = null

    fun loadVideo(videoJson: String?) {
        if (videoJson == null) {
            _uiState.update { it.copy(isLoading = false, errorMessage = "Video ma'lumoti topilmadi") }
            return
        }
        _uiState.update { it.copy(isLoading = true, errorMessage = null) }
        try {
            val video = gson.fromJson(videoJson, Video::class.java)
            currentVideoId = video.id
            _uiState.update { it.copy(isLoading = false, video = video) }
        } catch (e: Exception) {
            handleException(e, "VideoDetailJsonParseError")
            _uiState.update { it.copy(isLoading = false, errorMessage = "Video ma'lumotini o'qishda xatolik") }
        }
    }


    fun onEditVideoClick() {
        _uiState.value.video?.let {
            _navigationEvent.value = Event(VideoDetailNavigationEvent.NavigateToEditVideo(it))
        }
    }

    fun deleteVideo() {
        val videoToDelete = _uiState.value.video ?: return
        _uiState.update { it.copy(isLoading = true) }
        viewModelScope.launch {
            when (val result = repository.deleteVideo(videoToDelete.id)) {
                is DataWrapper.Success -> {
                    _navigationEvent.postValue(Event(VideoDetailNavigationEvent.NavigateBackWithMessage("Video o'chirildi")))
                }
                is DataWrapper.Error -> {
                    _uiState.update { it.copy(isLoading = false, errorMessage = "O'chirishda xatolik: ${result.error}") }
                }
                is DataWrapper.Loading -> {}
            }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }
}