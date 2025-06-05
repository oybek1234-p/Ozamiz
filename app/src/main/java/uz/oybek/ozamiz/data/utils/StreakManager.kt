package uz.oybek.ozamiz.util

import uz.oybek.ozamiz.data.User
import uz.oybek.ozamiz.data.model.StreakInfo
import java.time.LocalDate
import java.time.format.DateTimeFormatter

object StreakManager {

    private val dateFormatter: DateTimeFormatter = DateTimeFormatter.ISO_LOCAL_DATE

    data class StreakUpdateResult(
        val updatedUser: User,
        val streakContinued: Boolean = false,
        val streakBrokenAndReset: Boolean = false,
        val streakStartedNew: Boolean = false, // True if streak is 1 (either new or reset)
        val newBestStreakAchieved: Boolean = false,
        val isFirstActionToday: Boolean = false
    )

    fun updateUserStreakOnAction(currentUser: User): StreakUpdateResult {
        val today = LocalDate.now()
        val todayStr = today.format(dateFormatter)

        var currentStreakInfo = currentUser.streakInfo ?: StreakInfo() // Handles null streakInfo
        val oldCurrentStreak = currentStreakInfo.currentStreak
        val oldBestStreak = currentStreakInfo.bestStreak
        val oldLastActionDateStr = currentStreakInfo.lastActionDate

        var lastActionDateLD: LocalDate? = null
        var isVeryFirstActionEver = false

        if (oldLastActionDateStr.isNotBlank()) {
            lastActionDateLD = try {
                LocalDate.parse(oldLastActionDateStr, dateFormatter)
            } catch (e: Exception) {
                // Invalid date format, treat as very old
                today.minusDays(2) // Force reset/new
            }
        } else {
            // No last action date means this is the very first action for streak tracking
            isVeryFirstActionEver = true
            lastActionDateLD = today.minusDays(2) // Treat as if it was long ago to start a new streak
        }

        var streakContinued = false
        var streakBrokenAndReset = false
        var streakStartedNew = false
        var newBestStreakAchieved = false
        var isFirstActionToday = false

        if (lastActionDateLD == today) {
            // Action already performed today
            isFirstActionToday = false
            // Streak info remains unchanged for 'currentStreak' and 'lastActionDate'
            // Best streak might have been updated by a previous action today if it was a new best.
            currentStreakInfo = currentStreakInfo.copy() // No change to current/last date
        } else {
            isFirstActionToday = true
            val yesterday = today.minusDays(1)

            if (lastActionDateLD == yesterday) {
                // Streak continues
                currentStreakInfo = currentStreakInfo.copy(
                    currentStreak = currentStreakInfo.currentStreak + 1,
                    activeDays = currentStreakInfo.activeDays + 1,
                    lastActionDate = todayStr
                )
                streakContinued = true
                if (currentStreakInfo.currentStreak > oldBestStreak) {
                    currentStreakInfo = currentStreakInfo.copy(bestStreak = currentStreakInfo.currentStreak)
                    newBestStreakAchieved = true
                }
            } else {
                // Streak broken or first time action (or first after a long gap)
                if (oldCurrentStreak > 0 && !isVeryFirstActionEver) { // Was on a streak, but broke it
                    streakBrokenAndReset = true
                }
                currentStreakInfo = currentStreakInfo.copy(
                    currentStreak = 1,
                    lastActionDate = todayStr
                )
                streakStartedNew = true // currentStreak is now 1
                if (currentStreakInfo.currentStreak > oldBestStreak) {
                    currentStreakInfo = currentStreakInfo.copy(bestStreak = currentStreakInfo.currentStreak)
                    newBestStreakAchieved = true // If 1 is greater than previous best (e.g. 0)
                }
            }
        }

        val updatedUser = currentUser.copy(streakInfo = currentStreakInfo)
        return StreakUpdateResult(
            updatedUser,
            streakContinued,
            streakBrokenAndReset,
            streakStartedNew,
            newBestStreakAchieved,
            isFirstActionToday
        )
    }

    fun shouldShowFirstLogCongratulation(updateResult: StreakUpdateResult): Boolean {
        // Show if it's the first action today AND it's a newly started/reset streak (current streak is 1)
        return updateResult.isFirstActionToday && updateResult.streakStartedNew
    }
}