package uz.oybek.ozamiz.ui.feature.log.voice

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import uz.oybek.ozamiz.data.food.analyzer.GeminiSpeechRecognizer
import uz.oybek.ozamiz.data.utils.DataWrapper

sealed class VoiceUiState {
    data object Idle : VoiceUiState()
    data object Recording : VoiceUiState()
    data object Processing : VoiceUiState()
    data class Success(val text: String) : VoiceUiState()
    data class Error(val message: String) : VoiceUiState()
}

class VoiceRecorderViewModel : ViewModel() {

    private val _uiState = MutableStateFlow<VoiceUiState>(VoiceUiState.Idle)
    val uiState: StateFlow<VoiceUiState> = _uiState.asStateFlow()

    fun onRecordStarted() {
        _uiState.value = VoiceUiState.Recording
    }

    fun analyzeAudio(uri: Uri) {
        _uiState.value = VoiceUiState.Processing
        viewModelScope.launch {
            when (val result = GeminiSpeechRecognizer.transcribeAudio(uri)) {
                is DataWrapper.Success -> _uiState.value =
                    VoiceUiState.Success(result.data)
                is DataWrapper.Error -> _uiState.value =
                    VoiceUiState.Error(result.error ?: "Ovozni aniqlab bo'lmadi")
            }
        }
    }

    fun reset() {
        _uiState.value = VoiceUiState.Idle
    }
}