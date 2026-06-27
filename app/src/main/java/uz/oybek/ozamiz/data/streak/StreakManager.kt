package uz.oybek.ozamiz.data.streak

import uz.oybek.ozamiz.data.user.model.User
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

object StreakManager {

    private val dateFormatter: DateTimeFormatter = DateTimeFormatter.ISO_LOCAL_DATE

    data class StreakUpdateResult(
        val updatedUser: User,
        val streakContinued: Boolean = false,
        val streakBrokenAndReset: Boolean = false,
        val streakStartedNew: Boolean = false,
        val newBestStreakAchieved: Boolean = false,
        val isFirstActionToday: Boolean = false
    )

    fun updateUserStreakOnAction(currentUser: User): StreakUpdateResult {
        val today = LocalDate.now()
        val todayStr = today.format(dateFormatter)

        var currentStreakInfo = currentUser.streakInfo ?: StreakInfo()
        val oldCurrentStreak = currentStreakInfo.currentStreak
        val oldBestStreak = currentStreakInfo.bestStreak
        val oldLastActionDateStr = currentStreakInfo.lastActionDate
        val activityHistory = currentStreakInfo.activityHistory.toMutableList()

        var lastActionDateLD: LocalDate? = null
        var isVeryFirstActionEver = false

        if (oldLastActionDateStr.isNotBlank()) {
            lastActionDateLD = try {
                LocalDate.parse(oldLastActionDateStr, dateFormatter)
            } catch (e: Exception) {
                // Invalid date format, treat as very old
                today.minusDays(100)
            }
        } else {
            isVeryFirstActionEver = true
            lastActionDateLD = today.minusDays(100)
        }

        var streakContinued = false
        var streakBrokenAndReset = false
        var streakStartedNew = false
        var newBestStreakAchieved = false
        var isFirstActionToday = false
        
        // Add today to history if not present
        if (!activityHistory.contains(todayStr)) {
            activityHistory.add(todayStr)
            // Optional: Limit history size to last 60 entries to save space
            if (activityHistory.size > 60) {
                 activityHistory.removeAt(0)
            }
        }

        if (lastActionDateLD == today) {
            // Action already performed today
            isFirstActionToday = false
            // Just update history if it wasn't there (handled above) 
            // and keep other stats same
            currentStreakInfo = currentStreakInfo.copy(activityHistory = activityHistory)
        } else {
            isFirstActionToday = true
            
            // Calculate days between last action and today
            val daysDiff = ChronoUnit.DAYS.between(lastActionDateLD, today)
            
            when {
                daysDiff == 1L -> {
                    // Yesterday was last action -> Increment Streak
                    currentStreakInfo = currentStreakInfo.copy(
                        currentStreak = currentStreakInfo.currentStreak + 1,
                        activeDays = currentStreakInfo.activeDays + 1,
                        lastActionDate = todayStr,
                        activityHistory = activityHistory
                    )
                    streakContinued = true
                }
                daysDiff in 2..3L -> {
                    currentStreakInfo = currentStreakInfo.copy(
                        currentStreak = currentStreakInfo.currentStreak + 1, // Resume incrementing
                        activeDays = currentStreakInfo.activeDays + 1,
                        lastActionDate = todayStr,
                        activityHistory = activityHistory
                    )
                    streakContinued = true
                }
                else -> {
                     if (oldCurrentStreak > 0 && !isVeryFirstActionEver) {
                        streakBrokenAndReset = true
                    }
                    currentStreakInfo = currentStreakInfo.copy(
                        currentStreak = 1,
                        activeDays = currentStreakInfo.activeDays + 1, // Still an active day
                        lastActionDate = todayStr,
                        activityHistory = activityHistory
                    )
                    streakStartedNew = true
                }
            }
            
             if (currentStreakInfo.currentStreak > oldBestStreak) {
                currentStreakInfo = currentStreakInfo.copy(bestStreak = currentStreakInfo.currentStreak)
                newBestStreakAchieved = true
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
}