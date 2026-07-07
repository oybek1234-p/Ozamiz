package uz.oybek.ozamiz.ui.feature.streak

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import uz.oybek.ozamiz.data.streak.StreakInfo
import uz.oybek.ozamiz.data.user.AdminRepository
import uz.oybek.ozamiz.data.user.UserProfileRepository
import uz.oybek.ozamiz.data.user.model.User
import uz.oybek.ozamiz.data.user.model.UserSortType

class StreakViewModel : ViewModel() {

    private val _topUsers = MutableStateFlow<List<User>>(emptyList())
    val topUsers = _topUsers.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()
    
    fun loadTopUsers() {
        viewModelScope.launch {
            _isLoading.value = true
            val result = AdminRepository.getUsers(
                limit = 10,
                sortBy = UserSortType.TOP_STREAK
            )
            result.onSuccess { list ->
                _topUsers.value = list.filter { (it.streakInfo?.currentStreak ?: 0) >= 0 }.take(10)
            }.onFailure {
                // Ignore gracefully
            }
            _isLoading.value = false
        }
    }

    fun getCurrentUserStreak(): StreakInfo? {
        return UserProfileRepository.user.streakInfo
    }
}
