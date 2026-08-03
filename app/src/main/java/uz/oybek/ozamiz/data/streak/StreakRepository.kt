package uz.oybek.ozamiz.data.streak

import kotlinx.coroutines.MainScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import uz.oybek.ozamiz.data.user.model.User
import uz.oybek.ozamiz.data.user.UserProfileRepository
import java.time.LocalDate
import java.time.format.DateTimeFormatter

object StreakRepository {

    private val _levelFlow = MutableStateFlow<StreakManager.StreakUpdateResult?>(null)
    val levelFlow = _levelFlow.asStateFlow()

    fun clearStreakResult() {
        _levelFlow.value = null
    }

    fun unfreezeStreak() {
        val currentUser = UserProfileRepository.user
        if (!currentUser.valid) return
        val streakInfo = currentUser.streakInfo ?: return

        val today = LocalDate.now()
        val yesterdayStr = today.minusDays(1).format(DateTimeFormatter.ISO_LOCAL_DATE)

        val updatedStreakInfo = streakInfo.copy(
            lastActionDate = yesterdayStr
        )
        UserProfileRepository.updateStreakInfo(updatedStreakInfo)
    }

    fun updateStreak() {
        val currentUser = UserProfileRepository.user
        if (!currentUser.valid) return

        val result = StreakManager.updateUserStreakOnAction(currentUser)

        try {
            if (result.streakContinued || result.streakBrokenAndReset || result.streakStartedNew || result.isFirstActionToday) {
                UserProfileRepository.updateStreakInfo(result.updatedUser.streakInfo)

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
