package uz.oybek.ozamiz.data.streak

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class StreakInfo constructor(
    val lastActionDate: String = "",
    val currentStreak: Int = 0,
    val bestStreak: Int = 0,
    val activeDays: Int = 0,
    val streakGoal: Int = 7
) : Parcelable {
    constructor() : this(
        lastActionDate = "",
        currentStreak = 0,
        bestStreak = 0,
        0,
        streakGoal = 7
    )
}