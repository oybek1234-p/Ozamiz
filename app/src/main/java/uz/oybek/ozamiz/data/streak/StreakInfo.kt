package uz.oybek.ozamiz.data.streak

import androidx.annotation.Keep
import java.io.Serializable

@Keep
data class StreakInfo constructor(
    val lastActionDate: String = "",
    val currentStreak: Int = 0,
    val bestStreak: Int = 0,
    val activeDays: Int = 0,
    val streakGoal: Int = 7
) : Serializable {
    constructor() : this(
        lastActionDate = "",
        currentStreak = 0,
        bestStreak = 0,
        activeDays = 0,
        streakGoal = 7
    )
}