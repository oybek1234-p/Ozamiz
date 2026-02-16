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
                daysDiff > 1L && daysDiff <= 3L -> {
                    // Missed 1 or 2 days -> Streak Stays Same (Maintenance)
                    // We DO NOT increment currentStreak, but we also DO NOT reset it.
                    // We just update lastActionDate and activeDays?
                    // "Streak stays same" implies the count doesn't drop. 
                    // Should we increment activeDays? Yes, because we are active today.
                    // Should we increment current streak? The user said "stay same". 
                    // Technically if I skip a day, my consecutive streak is broken, but the user wants 'mercy'.
                    // Usually mercy means "it doesn't reset to 0", but maybe doesn't increment either if it's not consecutive?
                    // User said: "It will only use not opened the app for 3 days. otherwise it will stay same."
                    // Let's interpret "stay same" as "count is preserved". 
                    // Is today a continuation? If today is a new active day, logic suggests we add +1 if we are "keeping" the streak.
                    // BUT, accurate interpretation of "stay same" usually means "don't reset".
                    // If I have 15, miss a day, then come back. It is 15. 
                    // If I come back today, it should probably be 16? 
                    // Or maybe it stays 15 until I recover? 
                    // Let's assume standard "Freeze" logic: It doesn't reset, but since the chain is broken, maybe we just Resume?
                    // Let's Increment. Because if I am back, I am back!
                    // Wait, if I miss 1 day, and I am back, my streak flows. 
                    // Let's Increment it. 
                    
                    currentStreakInfo = currentStreakInfo.copy(
                        currentStreak = currentStreakInfo.currentStreak + 1, // Resume incrementing
                        activeDays = currentStreakInfo.activeDays + 1,
                        lastActionDate = todayStr,
                        activityHistory = activityHistory
                    )
                    streakContinued = true
                }
                else -> {
                    // > 3 days (4 or more) -> Reset
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

    fun shouldShowFirstLogCongratulation(updateResult: StreakUpdateResult): Boolean {
        return updateResult.isFirstActionToday && updateResult.streakStartedNew
    }
}