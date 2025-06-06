// C:\Users\User\AndroidStudioProjects\Ozamiz\app\src\main\java\uz\oybek\ozamiz\ui\profile\BadgesViewModel.kt
package uz.oybek.ozamiz.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import uz.oybek.ozamiz.data.LocalUser
import uz.oybek.ozamiz.data.gamification.Badge
import uz.oybek.ozamiz.data.gamification.GamificationManager

data class BadgesUiState(
    val badges: List<Badge> = emptyList(),
    val isLoading: Boolean = true
)

class BadgesViewModel : ViewModel() {
    private val gamificationManager = GamificationManager()

    private val _uiState = MutableStateFlow(BadgesUiState())
    val uiState: StateFlow<BadgesUiState> = _uiState.asStateFlow()

    init {
        loadBadges()
    }

    private fun loadBadges() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val user = LocalUser.user
            val allBadges = gamificationManager.getAllBadgesWithUnlockStatus(user.unlockedBadgeIds)
            _uiState.update { it.copy(badges = allBadges, isLoading = false) }
        }
    }
}