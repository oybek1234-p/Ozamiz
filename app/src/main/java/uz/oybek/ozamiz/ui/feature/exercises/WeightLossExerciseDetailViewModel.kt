package uz.oybek.ozamiz.ui.feature.exercises

import androidx.annotation.Keep
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import uz.oybek.ozamiz.data.exercise.WeightLossExercisesRepository
import uz.oybek.ozamiz.data.exercise.model.HomeExercise

@Keep
data class WeightLossExerciseDetailUiState(
    val exercise: HomeExercise? = null,
    val timeLeftSeconds: Int = 0,
    val totalDurationSeconds: Int = 0,
    val isRunning: Boolean = false,
    val progress: Int = 100
)

class WeightLossExerciseDetailViewModel : ViewModel() {

    private val _uiState = MutableLiveData<WeightLossExerciseDetailUiState>(WeightLossExerciseDetailUiState())
    val uiState: LiveData<WeightLossExerciseDetailUiState> = _uiState

    private var timerJob: Job? = null

    fun loadExercise(id: String) {
        val exercise = WeightLossExercisesRepository.getExerciseById(id)
        exercise?.let {
            _uiState.value = WeightLossExerciseDetailUiState(
                exercise = it,
                timeLeftSeconds = it.durationSeconds,
                totalDurationSeconds = it.durationSeconds,
                isRunning = false,
                progress = 100
            )
        }
    }

    fun toggleStartPause() {
        val currentState = _uiState.value ?: return
        if (currentState.isRunning) {
            pauseTimer()
        } else {
            startTimer()
        }
    }

    private fun startTimer() {
        val currentState = _uiState.value ?: return
        val currentExercise = currentState.exercise ?: return

        timerJob?.cancel()
        _uiState.value = currentState.copy(isRunning = true)

        timerJob = viewModelScope.launch {
            var remaining = _uiState.value?.timeLeftSeconds ?: currentExercise.durationSeconds
            if (remaining <= 0) {
                remaining = currentExercise.durationSeconds
            }
            while (remaining > 0) {
                delay(1000)
                remaining--
                val pct = (remaining * 100) / currentExercise.durationSeconds
                _uiState.value = _uiState.value?.copy(
                    timeLeftSeconds = remaining,
                    progress = pct
                )
            }
            _uiState.value = _uiState.value?.copy(
                isRunning = false,
                progress = 0
            )
        }
    }

    private fun pauseTimer() {
        timerJob?.cancel()
        _uiState.value = _uiState.value?.copy(isRunning = false)
    }

    fun restartTimer() {
        timerJob?.cancel()
        val currentState = _uiState.value ?: return
        val currentExercise = currentState.exercise ?: return
        _uiState.value = currentState.copy(
            timeLeftSeconds = currentExercise.durationSeconds,
            progress = 100,
            isRunning = false
        )
    }

    override fun onCleared() {
        super.onCleared()
        timerJob?.cancel()
    }
}
