package uz.oybek.ozamiz.ui.feature.home

import android.os.Parcelable
import androidx.annotation.Keep
import kotlinx.parcelize.Parcelize

@Keep
@Parcelize
data class HomeArgs(
    val showPremium: Boolean = false
) : Parcelable
