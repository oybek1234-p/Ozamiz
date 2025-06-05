package uz.oybek.ozamiz.data.model // Or your preferred data model package

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Parcelize
data class StreakInfo constructor(
    val lastActionDate: String = "",
    val currentStreak: Int = 0,
    val bestStreak: Int = 0,
    val activeDays: Int = 0,
    val streakGoal: Int = 7 // Default goal for UI, like in the image
) : Parcelable {
    constructor() : this(
        lastActionDate = "",
        currentStreak = 0,
        bestStreak = 0,
        0,
        streakGoal = 7
    )
}