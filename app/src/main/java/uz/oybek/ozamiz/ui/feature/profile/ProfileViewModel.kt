package uz.oybek.ozamiz.ui.feature.profile

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import uz.oybek.ozamiz.data.profile.api.ProfileAuthRepo
import uz.oybek.ozamiz.data.profile.api.ProfileManagerRepo
import uz.oybek.ozamiz.data.profile.api.ProfileRepo
import uz.oybek.ozamiz.data.profile.model.User
import javax.inject.Inject

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val profileRepo: ProfileRepo,
    private val profileManagerRepo: ProfileManagerRepo,
    private val profileAuthRepo: ProfileAuthRepo
) : ViewModel() {

    private val _uiEffect = Channel<ProfileUiEffect>(Channel.BUFFERED)
    val uiEffect: Flow<ProfileUiEffect> = _uiEffect.receiveAsFlow()

    val userState: StateFlow<User> = profileRepo.user

    val user: User
        get() = profileRepo.currentUser

    fun updateAvatar(uri: Uri) {
        viewModelScope.launch {
            profileManagerRepo.updateAvatar(uri)
        }
    }

    fun updateUser(
        onlyLocal: Boolean = false,
        recalculateUserPlan: Boolean = false,
        transform: (User) -> User
    ) {
        viewModelScope.launch {
            profileManagerRepo.updateUser(onlyLocal, recalculateUserPlan, transform)
        }
    }

    fun logout() {
        viewModelScope.launch {
            profileAuthRepo.signOut()
        }
    }
}
