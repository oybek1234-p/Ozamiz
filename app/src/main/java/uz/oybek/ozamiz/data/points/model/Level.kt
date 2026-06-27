package uz.oybek.ozamiz.data.points.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class Level(
    val name: String,
    val emoji: String,
    val requiredPoints: Int,
    val motivationText: String
) : Parcelable
