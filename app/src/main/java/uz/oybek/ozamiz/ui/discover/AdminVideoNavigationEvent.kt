package uz.oybek.ozamiz.ui.discover

import android.net.Uri
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.gson.Gson
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import uz.oybek.ozamiz.data.DataWrapper
import uz.oybek.ozamiz.data.workouts.Video
import uz.oybek.ozamiz.data.workouts.VideoRepository
import uz.oybek.ozamiz.data.workouts.WorkoutCategory // Import
import uz.oybek.ozamiz.data.workouts.WorkoutRepository // WorkoutCategoryRepository uchun
import uz.oybek.ozamiz.handleException
import uz.oybek.ozamiz.ui.getinfo.Event

sealed class AdminVideoNavigationEvent {
    data class ShowToast(val message: String) : AdminVideoNavigationEvent()
    object NavigateBack : AdminVideoNavigationEvent()
}

class AddEditVideoViewModel : ViewModel() {

    private val videoRepository = VideoRepository()
    private val workoutCategoryRepository = WorkoutRepository() // Workout kategoriyalarini olish uchun
    private val gson = Gson()
    private val TAG = "AddEditVideoVM"

    private val _uiState = MutableStateFlow(AddEditVideoUiState())
    val uiState: StateFlow<AddEditVideoUiState> = _uiState.asStateFlow()

    private val _navigationEvent = MutableLiveData<Event<AdminVideoNavigationEvent>>()
    val navigationEvent: LiveData<Event<AdminVideoNavigationEvent>> = _navigationEvent

    private var currentVideo = Video()
    private var initialVideo: Video? = null

    var selectedThumbnailUri: Uri? = null
    private var loadCategoriesJob: Job? = null

    fun initialize(videoJson: String?) {
        loadCategoriesJob?.cancel()
        _uiState.update { AddEditVideoUiState(isLoadingCategories = true) } // Reset and set loading for categories

        if (videoJson != null) {
            try {
                initialVideo = gson.fromJson(videoJson, Video::class.java)
                currentVideo = initialVideo!!.copy()
                _uiState.update {
                    it.copy(
                        videoToEdit = initialVideo,
                        selectedCategoryIds = initialVideo?.categoryIds ?: emptyList(),
                        title = "Videoni Tahrirlash",
                        canDelete = true,
                        thumbnailUriToDisplay = initialVideo?.thumbnailUrl?.let { Uri.parse(it) }
                    )
                }
            } catch (e: Exception) {
                handleException(e, "VideoJsonParseErrorAdmin")
                _uiState.update { it.copy(isLoadingCategories = false, errorMessage = "Video ma'lumotlarini o'qishda xatolik") }
            }
        } else {
            initialVideo = Video()
            currentVideo = Video()
            _uiState.update {
                it.copy(
                    title = "Yangi Video Qo'shish",
                    videoToEdit = null,
                    selectedCategoryIds = emptyList(),
                    canDelete = false
                )
            }
        }
        selectedThumbnailUri = null
        loadAvailableCategories() // Kategoriyalarni yuklash
        _uiState.update { it.copy(isSaving = false, saveSuccess = false, deleteSuccess = false, errorMessage = null) }
    }

    private fun loadAvailableCategories() {
        loadCategoriesJob = viewModelScope.launch {
            workoutCategoryRepository.getWorkoutCategoriesFlow()
                .catch { e ->
                    Log.e(TAG, "Error loading workout categories for video", e)
                    handleException(e as Exception, "LoadWorkoutCategoriesForVideo")
                    _uiState.update {
                        it.copy(
                            isLoadingCategories = false,
                            errorMessage = (it.errorMessage ?: "") + "\nKategoriyalarni yuklashda xatolik"
                        )
                    }
                }
                .collectLatest { result ->
                    when (result) {
                        is DataWrapper.Success -> {
                            _uiState.update {
                                it.copy(
                                    isLoadingCategories = false,
                                    availableCategories = result.data
                                )
                            }
                        }
                        is DataWrapper.Error -> _uiState.update {
                            it.copy(isLoadingCategories = false, errorMessage = (it.errorMessage ?: "") + "\n" + result.error)
                        }
                        is DataWrapper.Loading -> {
                            _uiState.update { it.copy(isLoadingCategories = true)}
                        }
                    }
                }
        }
    }


    fun updateVideoField(updateAction: Video.() -> Video) {
        currentVideo = currentVideo.updateAction()
    }

    // ChipGroup'dan kelgan tanlangan kategoriyalarni yangilash
    fun updateSelectedCategories(categoryIds: List<String>) {
        currentVideo = currentVideo.copy(categoryIds = categoryIds.ifEmpty { null })
        _uiState.update { it.copy(selectedCategoryIds = categoryIds) }
    }


    fun saveVideo() {
        if (currentVideo.name.isBlank()) {
            _navigationEvent.postValue(Event(AdminVideoNavigationEvent.ShowToast("Video nomini kiriting")))
            return
        }
        if (currentVideo.youtubeUrl.isBlank()) {
            _navigationEvent.postValue(Event(AdminVideoNavigationEvent.ShowToast("YouTube URL manzilini kiriting")))
            return
        }
        if (currentVideo.categoryIds.isNullOrEmpty()) {
            _navigationEvent.postValue(Event(AdminVideoNavigationEvent.ShowToast("Kamida bitta kategoriya tanlang")))
            return
        }
        // Rasm ixtiyoriy bo'lishi mumkin, shuning uchun tekshirilmaydi

        _uiState.update { it.copy(isSaving = true, errorMessage = null) }

        viewModelScope.launch {
            val result: DataWrapper<*> = if (currentVideo.id.isBlank()) {
                videoRepository.addVideo(currentVideo, selectedThumbnailUri)
            } else {
                videoRepository.updateVideo(currentVideo, selectedThumbnailUri)
            }

            when (result) {
                is DataWrapper.Success -> {
                    val message = if (initialVideo?.id.isNullOrEmpty()) "Video qo'shildi" else "Video yangilandi"
                    _navigationEvent.postValue(Event(AdminVideoNavigationEvent.ShowToast(message)))
                    _navigationEvent.postValue(Event(AdminVideoNavigationEvent.NavigateBack))
                    // State'ni tozalash kerak emas, chunki navigatsiya bo'ladi
                }
                is DataWrapper.Error -> {
                    _uiState.update { it.copy(isSaving = false, errorMessage = result.error) }
                }
                is DataWrapper.Loading -> {}
            }
            // Agar navigatsiya bo'lmasa ham isSaving'ni false qilish
            if (_uiState.value.isSaving) {
                _uiState.update { it.copy(isSaving = false) }
            }
        }
    }

    fun deleteVideo() {
        val videoIdToDelete = currentVideo.id
        if (videoIdToDelete.isBlank()) return

        _uiState.update { it.copy(isSaving = true, errorMessage = null) }
        viewModelScope.launch {
            when (val result = videoRepository.deleteVideo(videoIdToDelete)) {
                is DataWrapper.Success -> {
                    _navigationEvent.postValue(Event(AdminVideoNavigationEvent.ShowToast("Video o'chirildi")))
                    _navigationEvent.postValue(Event(AdminVideoNavigationEvent.NavigateBack))
                }
                is DataWrapper.Error -> {
                    _uiState.update { it.copy(isSaving = false, errorMessage = "O'chirishda xatolik: ${result.error}") }
                }
                is DataWrapper.Loading -> {}
            }
            if (_uiState.value.isSaving) {
                _uiState.update { it.copy(isSaving = false) }
            }
        }
    }

    fun hasChanges(): Boolean {
        // Eng so'nggi tanlangan kategoriyalarni hisobga olish
        val currentVideoWithLatestCategories = currentVideo.copy(categoryIds = _uiState.value.selectedCategoryIds.ifEmpty { null })
        val dataChanged = currentVideoWithLatestCategories != initialVideo
        val imageChanged = selectedThumbnailUri != null
        return dataChanged || imageChanged
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    override fun onCleared() {
        super.onCleared()
        loadCategoriesJob?.cancel()
    }
}