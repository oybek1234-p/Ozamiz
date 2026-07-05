package uz.oybek.ozamiz.data.streak

import kotlinx.coroutines.MainScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import uz.oybek.ozamiz.data.user.UserRepository

object StreakRepository {

    private val _levelFlow = MutableStateFlow<StreakManager.StreakUpdateResult?>(null)
    val levelFlow = _levelFlow.asStateFlow()

    fun clearStreakResult() {
        _levelFlow.value = null
    }

    fun updateStreak() {
        val currentUser = UserRepository.user
        if (!currentUser.valid) return

        val result = StreakManager.updateUserStreakOnAction(currentUser)

        try {
            if (result.streakContinued || result.streakBrokenAndReset || result.streakStartedNew || result.isFirstActionToday) {
                UserRepository.updateUser { user ->
                    user.streakInfo = result.updatedUser.streakInfo
                }

                if (result.streakContinued || result.isFirstActionToday) {
                    MainScope().launch {
                        _levelFlow.emit(result)
                    }
                }
            }
        } catch (e: Exception) {

        }
    }
}
