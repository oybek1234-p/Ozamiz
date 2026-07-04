package uz.oybek.ozamiz.ui.feature.log.voice

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

sealed class VoiceUiState {
    data object Idle : VoiceUiState()
    data object Recording : VoiceUiState()
    data object Processing : VoiceUiState()
    data class Success(val uri: android.net.Uri) : VoiceUiState()
    data class Error(val message: String) : VoiceUiState()
}

class VoiceRecorderViewModel : ViewModel() {

    private val _uiState = MutableStateFlow<VoiceUiState>(VoiceUiState.Idle)
    val uiState: StateFlow<VoiceUiState> = _uiState.asStateFlow()

    fun onRecordStarted() {
        _uiState.value = VoiceUiState.Recording
    }

    fun analyzeAudio(uri: android.net.Uri) {
        _uiState.value = VoiceUiState.Success(uri)
    }

    fun reset() {
        _uiState.value = VoiceUiState.Idle
    }
}