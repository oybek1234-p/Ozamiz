package uz.oybek.ozamiz.ui.feature.premium

import android.os.Parcelable
import androidx.annotation.Keep
import kotlinx.parcelize.Parcelize

@Keep
@Parcelize
data class PaymentGetCheckArgs(
    val price: Long = 0L,
    val isYearly: Boolean = false
) : Parcelable
