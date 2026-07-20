package uz.oybek.ozamiz.ui.feature.exercises

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import uz.oybek.ozamiz.data.exercise.WeightLossExercisesRepository
import uz.oybek.ozamiz.data.exercise.model.HomeVideo

data class WeightLossVideoDetailUiState(
    val video: HomeVideo? = null,
    val isLoading: Boolean = false,
    val isLoggedSuccessfully: Boolean = false
)

class WeightLossVideoDetailViewModel : ViewModel() {

    private val _uiState = MutableLiveData<WeightLossVideoDetailUiState>(WeightLossVideoDetailUiState())
    val uiState: LiveData<WeightLossVideoDetailUiState> = _uiState

    fun loadVideo(id: String) {
        val video = WeightLossExercisesRepository.getVideoById(id)
        video?.let {
            _uiState.value = WeightLossVideoDetailUiState(video = it)
        }
    }

    fun completeWorkout() {
        val state = _uiState.value ?: return
        val video = state.video ?: return

        _uiState.value = state.copy(isLoading = true)
        viewModelScope.launch {
            val logged = WeightLossExercisesRepository.logCompletedWork(
                name = video.name,
                calories = video.calories,
                durationSeconds = video.durationSeconds
            )
            _uiState.value = _uiState.value?.copy(isLoading = false, isLoggedSuccessfully = logged)
        }
    }
}
