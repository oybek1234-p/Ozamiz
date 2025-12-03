package uz.oybek.ozamiz.ui.feature.log.voice

import androidx.core.net.toUri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import uz.oybek.ozamiz.data.utils.DataWrapper
import uz.oybek.ozamiz.data.food.analyzer.GeminiSpeechRecognizer
import java.io.File

sealed class VoiceRecorderUiState {
    object Idle : VoiceRecorderUiState()
    object Loading : VoiceRecorderUiState()
    data class Success(val transcribedText: String) : VoiceRecorderUiState()
    data class Error(val message: String) : VoiceRecorderUiState()
}

class VoiceRecorderViewModel : ViewModel() {

    private val _uiState = MutableStateFlow<VoiceRecorderUiState>(VoiceRecorderUiState.Idle)
    val uiState: StateFlow<VoiceRecorderUiState> = _uiState.asStateFlow()

    fun transcribeAudio(audioFile: File) {
        _uiState.value = VoiceRecorderUiState.Loading
        viewModelScope.launch {
            when (val result = GeminiSpeechRecognizer.transcribeAudio(audioFile.toUri())) {
                is DataWrapper.Success -> {
                    _uiState.value = VoiceRecorderUiState.Success(result.data)
                }
                is DataWrapper.Error -> {
                    _uiState.value = VoiceRecorderUiState.Error(result.error)
                }
            }
        }
    }
}