package uz.oybek.ozamiz.data.streak

import java.io.Serializable

data class StreakInfo constructor(
    val lastActionDate: String = "",
    val currentStreak: Int = 0,
    val bestStreak: Int = 0,
    val activeDays: Int = 0,
    val streakGoal: Int = 7,
    val activityHistory: List<String> = emptyList()
) : Serializable {
    constructor() : this(
        lastActionDate = "",
        currentStreak = 0,
        bestStreak = 0,
        activeDays = 0,
        streakGoal = 7,
        activityHistory = emptyList()
    )
}