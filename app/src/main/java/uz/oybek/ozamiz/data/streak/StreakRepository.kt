package uz.oybek.ozamiz.data.streak

import uz.oybek.ozamiz.data.user.UserRepository


object StreakRepository {

    fun updateStreak() {
        val currentUser = UserRepository.user
        if (!currentUser.valid) return

        val result = StreakManager.updateUserStreakOnAction(currentUser)
        
        // Only update if something changed (avoid unnecessary writes)
        if (result.streakContinued || result.streakBrokenAndReset || result.streakStartedNew || result.isFirstActionToday) {
             UserRepository.updateUser { user ->
                 user.streakInfo = result.updatedUser.streakInfo
             }
        }
    }
}
