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
        val isFirstActionToday: Boolean = false,
        val returnedAfterManyDays: Boolean = false
    )

    fun isStreakActive(streakInfo: StreakInfo?): Boolean {
        if (streakInfo == null) return false
        val lastDateStr = streakInfo.lastActionDate
        if (lastDateStr.isBlank()) return false
        return try {
            val lastDate = LocalDate.parse(lastDateStr, dateFormatter)
            val today = LocalDate.now()
            val daysDiff = ChronoUnit.DAYS.between(lastDate, today)
            daysDiff <= 1L
        } catch (e: Exception) {
            false
        }
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

        val newStreakInfo = when {
            daysDiff == 1L -> {
                streakContinued = true
                currentStreakInfo.copy(
                    currentStreak = currentStreakInfo.currentStreak + 1,
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
            returnedAfterManyDays = returnedAfterManyDays
        )
    }
}