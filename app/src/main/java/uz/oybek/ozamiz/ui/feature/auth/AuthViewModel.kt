package uz.oybek.ozamiz.ui.feature.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import uz.oybek.ozamiz.data.common.onFailure
import uz.oybek.ozamiz.data.common.onSuccess
import uz.oybek.ozamiz.data.profile.api.ProfileAuthRepo
import javax.inject.Inject

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val profileAuthRepo: ProfileAuthRepo
) : ViewModel() {

    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    private val _uiEffect = Channel<AuthUiEffect>(Channel.BUFFERED)
    val uiEffect: Flow<AuthUiEffect> = _uiEffect.receiveAsFlow()

    fun onGoogleSignInError(exception: Exception? = null) {
        _uiState.update { it.copy(isLoading = false) }
        viewModelScope.launch {
            _uiEffect.send(AuthUiEffect.ShowError(exception))
        }
    }

    fun signInWithGoogle(idToken: String) {
        if (_uiState.value.isLoading) return
        _uiState.update { it.copy(isLoading = true) }

        viewModelScope.launch {
            profileAuthRepo.loginGmail(idToken)
                .onSuccess {
                    _uiState.update { it.copy(isLoading = false) }
                }
                .onFailure { exception ->
                    _uiState.update { it.copy(isLoading = false) }
                    _uiEffect.send(AuthUiEffect.ShowError(exception))
                }
        }
    }

    fun signInEmail(email: String, password: String) {
        if (_uiState.value.isLoading) return
        _uiState.update { it.copy(isLoading = true) }

        viewModelScope.launch {
            profileAuthRepo.loginEmail(email, password)
                .onSuccess {
                    _uiState.update { it.copy(isLoading = false) }
                }
                .onFailure { exception ->
                    _uiState.update { it.copy(isLoading = false) }
                    _uiEffect.send(AuthUiEffect.ShowError(exception))
                }
        }
    }
}
