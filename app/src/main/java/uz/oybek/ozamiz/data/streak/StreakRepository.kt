package uz.oybek.ozamiz.data.streak

import uz.oybek.ozamiz.data.user.UserRepository


object StreakRepository {

    fun updateStreak(onUpdated: ((StreakInfo) -> Unit)? = null) {
        val currentUser = UserRepository.user
        if (!currentUser.valid) return

        val result = StreakManager.updateUserStreakOnAction(currentUser)
        
        if (result.streakContinued || result.streakBrokenAndReset || result.streakStartedNew || result.isFirstActionToday) {
             UserRepository.updateUser { user ->
                 user.streakInfo = result.updatedUser.streakInfo
             }
             if (result.isFirstActionToday || result.streakContinued) {
                 result.updatedUser.streakInfo?.let { onUpdated?.invoke(it) }
             }
        }
    }
}
