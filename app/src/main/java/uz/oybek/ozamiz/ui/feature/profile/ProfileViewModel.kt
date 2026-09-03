package uz.oybek.ozamiz.ui.feature.profile

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asFlow
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import uz.oybek.ozamiz.data.firebase.FirebasePaths
import uz.oybek.ozamiz.data.firebase.uploader.ImageUploader
import uz.oybek.ozamiz.data.user.UserAuthRepository
import uz.oybek.ozamiz.data.user.UserProfileRepository
import uz.oybek.ozamiz.data.user.local.LocalUser
import uz.oybek.ozamiz.data.user.model.User

class ProfileViewModel : ViewModel() {

    private val _cashbackProgress = MutableStateFlow(0)
    val cashbackProgress = _cashbackProgress.asStateFlow()

    val userState: StateFlow<User> = LocalUser.userLive.asFlow()
        .map { user -> user }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = LocalUser.user ?: User()
        )

    val userProfile: StateFlow<User> get() = userState

    val user: User
        get() = UserProfileRepository.user

    fun updateCalorieSettings(
        calories: Int,
        protein: Int,
        fat: Int,
        carbs: Int,
        goal: String,
        targetWeight: Double? = null,
        activityLevel: String? = null,
        currentWeight: Double? = null
    ) {
        UserProfileRepository.updateUser {
            targetCalories = calories
            targetProtein = protein
            targetFat = fat
            targetCarbs = carbs
            this.goal = goal
            if (targetWeight != null && targetWeight > 0) {
                this.targetWeight = targetWeight
            }
            if (currentWeight != null && currentWeight > 0) {
                this.weight = currentWeight
            }
            if (!activityLevel.isNullOrBlank()) {
                this.activityLevel = activityLevel
            }
        }
    }

    fun updateAvatar(uri: Uri) {
        viewModelScope.launch {
            val userId = LocalUser.user.uid
            if (userId.isBlank()) return@launch

            val url = ImageUploader.uploadImage(uri, FirebasePaths.Storage.avatars(userId))

            if (url != null) {
                UserProfileRepository.updateAvatarUrl(url)
            }
        }
    }

    fun logout() {
        UserAuthRepository.signOut()
    }

    fun deleteAccount() {
        UserAuthRepository.deleteAccount()
    }
}
