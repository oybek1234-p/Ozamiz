package uz.oybek.ozamiz.data.streak

import uz.oybek.ozamiz.data.user.model.User
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

object StreakManager {

    private val dateFormatter: DateTimeFormatter = DateTimeFormatter.ISO_LOCAL_DATE

    @androidx.annotation.Keep
    data class StreakUpdateResult(
        val updatedUser: User,
        val streakContinued: Boolean = false,
        val streakBrokenAndReset: Boolean = false,
        val streakStartedNew: Boolean = false,
        val newBestStreakAchieved: Boolean = false,
        val isFirstActionToday: Boolean = false,
        val returnedAfterManyDays: Boolean = false,
        val streakWasFrozen: Boolean = false
    )

    fun getDaysSinceLastAction(streakInfo: StreakInfo?): Long {
        if (streakInfo == null) return Long.MAX_VALUE
        val lastDateStr = streakInfo.lastActionDate
        if (lastDateStr.isBlank()) return Long.MAX_VALUE
        return try {
            val lastDate = LocalDate.parse(lastDateStr, dateFormatter)
            val today = LocalDate.now()
            ChronoUnit.DAYS.between(lastDate, today)
        } catch (e: Exception) {
            Long.MAX_VALUE
        }
    }

    fun isStreakFrozen(streakInfo: StreakInfo?): Boolean {
        val daysDiff = getDaysSinceLastAction(streakInfo)
        // If user missed 1 or 2 days (daysDiff is 2 or 3) and had an existing streak
        return daysDiff in 2L..3L && (streakInfo?.currentStreak ?: 0) > 0
    }

    fun isStreakActive(streakInfo: StreakInfo?): Boolean {
        if (streakInfo == null) return false
        val daysDiff = getDaysSinceLastAction(streakInfo)
        return daysDiff <= 3L
    }

    fun updateUserStreakOnAction(currentUser: User): StreakUpdateResult {
        val today = LocalDate.now()
        val todayStr = today.format(dateFormatter)
        val currentStreakInfo = currentUser.streakInfo ?: StreakInfo()

        if (currentStreakInfo.lastActionDate == todayStr) {
            return StreakUpdateResult(currentUser, isFirstActionToday = false)
        }

        val lastActionDateLD = currentStreakInfo.lastActionDate.takeIf { it.isNotBlank() }
            ?.let { LocalDate.parse(it, dateFormatter) }

        val daysDiff = if (lastActionDateLD != null)
            ChronoUnit.DAYS.between(lastActionDateLD, today) else Long.MAX_VALUE

        var streakContinued = false
        var streakBroken = false
        var streakStartedNew = false
        var returnedAfterManyDays = false
        var streakWasFrozen = false

        val newStreakInfo = when {
            daysDiff == 1L -> {
                streakContinued = true
                currentStreakInfo.copy(
                    currentStreak = currentStreakInfo.currentStreak + 1,
                    activeDays = currentStreakInfo.activeDays + 1,
                    lastActionDate = todayStr
                )
            }
            daysDiff in 2L..3L -> {
                // User missed up to 2 days: Streak is frozen & preserved!
                streakContinued = true
                streakWasFrozen = true
                val streakToKeep = if (currentStreakInfo.currentStreak > 0) currentStreakInfo.currentStreak + 1 else 1
                currentStreakInfo.copy(
                    currentStreak = streakToKeep,
                    activeDays = currentStreakInfo.activeDays + 1,
                    lastActionDate = todayStr
                )
            }
            else -> {
                if (currentStreakInfo.currentStreak > 0) streakBroken = true
                if (daysDiff >= 4L) returnedAfterManyDays = true

                streakStartedNew = true
                currentStreakInfo.copy(
                    currentStreak = 1,
                    activeDays = currentStreakInfo.activeDays + 1,
                    lastActionDate = todayStr
                )
            }
        }

        val newBestStreakAchieved = newStreakInfo.currentStreak > newStreakInfo.bestStreak
        val finalStreakInfo = if (newBestStreakAchieved) {
            newStreakInfo.copy(bestStreak = newStreakInfo.currentStreak)
        } else newStreakInfo

        return StreakUpdateResult(
            updatedUser = currentUser.copy(streakInfo = finalStreakInfo),
            streakContinued = streakContinued,
            streakBrokenAndReset = streakBroken,
            streakStartedNew = streakStartedNew,
            newBestStreakAchieved = newBestStreakAchieved,
            isFirstActionToday = true,
            returnedAfterManyDays = returnedAfterManyDays,
            streakWasFrozen = streakWasFrozen
        )
    }
}