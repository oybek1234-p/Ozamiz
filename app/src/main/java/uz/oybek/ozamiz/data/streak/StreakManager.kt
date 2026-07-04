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
        var returnedAfterManyDays = false
        
        if (!activityHistory.contains(todayStr)) {
            activityHistory.add(todayStr)
            if (activityHistory.size > 60) {
                 activityHistory.removeAt(0)
            }
        }

        if (lastActionDateLD == today) {
            isFirstActionToday = false
            currentStreakInfo = currentStreakInfo.copy(activityHistory = activityHistory)
        } else {
            isFirstActionToday = true
            
            val daysDiff = ChronoUnit.DAYS.between(lastActionDateLD, today)
            
            when {
                daysDiff == 1L -> {
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
                        currentStreak = currentStreakInfo.currentStreak + 1,
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
                    if (!isVeryFirstActionEver && daysDiff >= 4L) {
                        returnedAfterManyDays = true
                    }
                    currentStreakInfo = currentStreakInfo.copy(
                        currentStreak = 1,
                        activeDays = currentStreakInfo.activeDays + 1,
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
            isFirstActionToday,
            returnedAfterManyDays
        )
    }
}