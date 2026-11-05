package uz.oybek.ozamiz.ui.feature.profile

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import uz.oybek.ozamiz.common.result.BaseResult
import uz.oybek.ozamiz.data.premium.api.AdminPremiumRepo
import uz.oybek.ozamiz.data.profile.api.ProfileAuthRepo
import uz.oybek.ozamiz.data.profile.api.ProfileManagerRepo
import uz.oybek.ozamiz.data.profile.api.ProfileRepo
import uz.oybek.ozamiz.data.profile.model.User
import javax.inject.Inject

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val profileRepo: ProfileRepo,
    private val profileManagerRepo: ProfileManagerRepo,
    private val profileAuthRepo: ProfileAuthRepo,
    private val adminPremiumRepo: AdminPremiumRepo
) : ViewModel() {

    private val _uiEffect = Channel<ProfileUiEffect>(Channel.BUFFERED)
    val uiEffect: Flow<ProfileUiEffect> = _uiEffect.receiveAsFlow()

    val userState: StateFlow<User> = profileRepo.user

    private val _pendingVerificationsCount = MutableStateFlow(0)
    val pendingVerificationsCount: StateFlow<Int> = _pendingVerificationsCount.asStateFlow()

    val user: User
        get() = profileRepo.currentUser

    init {
        viewModelScope.launch {
            userState.collect { currentUser ->
                if (currentUser.admin) {
                    loadPendingVerificationsCount()
                } else {
                    _pendingVerificationsCount.value = 0
                }
            }
        }
    }

    fun loadPendingVerificationsCount() {
        viewModelScope.launch {
            when (val result = adminPremiumRepo.getPendingVerificationsCount()) {
                is BaseResult.Success -> {
                    _pendingVerificationsCount.value = result.data
                }
                is BaseResult.Failure -> {
                    _pendingVerificationsCount.value = 0
                }
            }
        }
    }

    fun updateAvatar(uri: Uri) {
        viewModelScope.launch {
            profileManagerRepo.updateUser(onlyLocal = true) {
                it.copy(avatarUrl = uri.toString())
            }
            profileManagerRepo.updateAvatar(uri)
        }
    }

    fun logout() {
        viewModelScope.launch {
            profileAuthRepo.signOut()
            _uiEffect.send(ProfileUiEffect.NavigateToAuth)
        }
    }
}
