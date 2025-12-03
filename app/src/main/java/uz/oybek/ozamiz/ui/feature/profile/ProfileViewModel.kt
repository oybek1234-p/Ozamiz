package uz.oybek.ozamiz.ui.feature.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.asFlow
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import uz.oybek.ozamiz.data.user.local.LocalUser
import uz.oybek.ozamiz.data.user.model.User

sealed class ProfileNavigationEvent {
    data object NavigateToAuth : ProfileNavigationEvent()
    data class ShowToast(val message: String) : ProfileNavigationEvent()
}

class ProfileViewModel : ViewModel() {

    val userState: StateFlow<User> = LocalUser.userLive.asFlow()
        .map { user -> user ?: User() }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = LocalUser.user ?: User()
        )


    fun logout() {

    }
}