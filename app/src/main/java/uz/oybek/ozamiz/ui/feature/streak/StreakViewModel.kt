package uz.oybek.ozamiz.ui.feature.streak

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import uz.oybek.ozamiz.data.streak.api.StreakRepo
import uz.oybek.ozamiz.util.logMessage
import javax.inject.Inject

@HiltViewModel
class StreakViewModel @Inject constructor(
    private val streakRepo: StreakRepo
) : ViewModel() {

    private val _uiState = MutableStateFlow(StreakUiState())
    val uiState = _uiState.asStateFlow()

    private val _effects = Channel<StreakUiEffect>(Channel.BUFFERED)
    val effects = _effects.receiveAsFlow()

    init {
        logMessage("StreakView")
        observeStreakRepo()
    }

    private fun observeStreakRepo() {
        viewModelScope.launch {
            streakRepo.streakState.collect { state ->
                _uiState.update { current ->
                    current.copy(
                        status = state.status,
                        streakInfo = state.streakInfo,
                        weekDays = state.weekDays
                    )
                }
            }
        }
        viewModelScope.launch {
            streakRepo.streakEvents.collect {
                _effects.send(StreakUiEffect.ShowStreakSheet)
            }
        }
    }

    fun onActionClick() {
        viewModelScope.launch {
            _effects.send(StreakUiEffect.Dismiss)
        }
    }
}
