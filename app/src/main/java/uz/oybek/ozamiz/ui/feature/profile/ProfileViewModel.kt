package uz.oybek.ozamiz.ui.feature.profile

import uz.oybek.ozamiz.data.firebase.uploader.ImageUploader

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asFlow
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import uz.oybek.ozamiz.data.user.UserRepository
import uz.oybek.ozamiz.data.user.local.LocalUser
import uz.oybek.ozamiz.data.user.model.User

sealed class ProfileNavigationEvent {
    data object NavigateToAuth : ProfileNavigationEvent()
    data class ShowToast(val message: String) : ProfileNavigationEvent()
}

class ProfileViewModel : ViewModel() {

    init {
        viewModelScope.launch {
            UserRepository.refreshUser()
            UserRepository.observeCurrentUser { }
        }
    }
    val userState: StateFlow<User> = LocalUser.userLive.asFlow()
        .map { user -> user ?: User() }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = LocalUser.user ?: User()
        )


    fun logout() {

    }

    val user: User
        get() = UserRepository.user

    fun updateCalorieSettings(calories: Int, protein: Int, fat: Int, carbs: Int, goal: String) {
        UserRepository.updateUser {
            it.targetCalories = calories
            it.targetProtein = protein
            it.targetFat = fat
            it.targetCarbs = carbs
            it.goal = goal
        }
    }

    fun updateAvatar(uri: Uri) {
        viewModelScope.launch {
            val userId = LocalUser.user.uid
            if (userId.isBlank()) return@launch

            val path = "user_avatars/$userId"
            val url = ImageUploader.uploadImage(uri, path)

            if (url != null) {
                LocalUser.update {
                    avatarUrl = url
                }
                UserRepository.updateUser { user ->
                    user.avatarUrl = url
                }
            }
        }
    }
}