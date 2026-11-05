package uz.oybek.ozamiz.data.premium.model

import androidx.annotation.Keep

@Keep
data class PremiumInfo(
    val card: String = "",
    val monthlyPrice: Long = 0L,
    val yearlyPrice: Long = 0L
)
