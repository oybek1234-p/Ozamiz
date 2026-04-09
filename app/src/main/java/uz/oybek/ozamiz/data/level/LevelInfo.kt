package uz.oybek.ozamiz.data.level

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class LevelInfo(
    val name: String,
    val emoji: String,
    val requiredPoints: Int,
    val motivationText: String,
    val color: Long
) : Parcelable