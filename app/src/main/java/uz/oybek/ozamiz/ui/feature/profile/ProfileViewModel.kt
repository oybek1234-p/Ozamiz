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
import uz.oybek.ozamiz.data.firebase.uploader.ImageUploader
import uz.oybek.ozamiz.data.logs.DailySummaryRepository
import uz.oybek.ozamiz.data.user.UserRepository
import uz.oybek.ozamiz.data.user.local.LocalUser
import uz.oybek.ozamiz.data.user.model.User
import uz.oybek.ozamiz.data.utils.DateUtils
import java.util.Calendar

sealed class ProfileNavigationEvent {
    data object NavigateToAuth : ProfileNavigationEvent()
    data class ShowToast(val message: String) : ProfileNavigationEvent()
}

class ProfileViewModel : ViewModel() {

    private val _cashbackProgress = MutableStateFlow(0)
    val cashbackProgress = _cashbackProgress.asStateFlow()

    init {
        viewModelScope.launch {
            UserRepository.refreshUser()
            UserRepository.observeCurrentUser { }
            observeCashbackProgress()
        }
    }

    val userState: StateFlow<User> = LocalUser.userLive.asFlow()
        .map { user -> user ?: User() }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = LocalUser.user ?: User()
        )

    private fun observeCashbackProgress() {
        val calendar = Calendar.getInstance()
        
        // Start of current month
        calendar.set(Calendar.DAY_OF_MONTH, 1)
        val startOfMonth = DateUtils.getStartOfDay(calendar.time)
        
        // End of current month
        calendar.set(Calendar.DAY_OF_MONTH, calendar.getActualMaximum(Calendar.DAY_OF_MONTH))
        val endOfMonth = DateUtils.getStartOfDay(calendar.time)

        viewModelScope.launch {
            DailySummaryRepository.observeSummaries(startOfMonth, endOfMonth).collect { summaries ->
                val activeDays = summaries.values.count { it.mealsCount >= 2 }
                _cashbackProgress.value = activeDays
            }
        }
    }

    fun logout() {
        // Implement logout logic if needed
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
