package uz.oybek.ozamiz.ui.feature.exercises

import androidx.annotation.Keep
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import uz.oybek.ozamiz.data.exercise.WeightLossExercisesRepository

@Keep
data class WeightLossExercisesUiState(
    val exercises: List<WeightLossExerciseUiModel> = emptyList(),
    val videos: List<WeightLossVideoUiModel> = emptyList()
)

class WeightLossExercisesViewModel : ViewModel() {

    private val _uiState = MutableLiveData<WeightLossExercisesUiState>()
    val uiState: LiveData<WeightLossExercisesUiState> = _uiState

    init {
        loadData()
    }

    fun loadData() {
        val exercises = WeightLossExercisesRepository.getHomeExercises()
        val videos = WeightLossExercisesRepository.getHomeVideos()
        val isPremium = WeightLossExercisesRepository.isPremiumUser()

        val uiExercises = exercises.mapIndexed { index, exercise ->
            val locked = !isPremium && index >= 3
            WeightLossExerciseUiModel(exercise, locked)
        }

        val uiVideos = videos.mapIndexed { index, video ->
            val locked = !isPremium && index >= 2
            WeightLossVideoUiModel(video, locked)
        }

        _uiState.value = WeightLossExercisesUiState(uiExercises, uiVideos)
    }
}
