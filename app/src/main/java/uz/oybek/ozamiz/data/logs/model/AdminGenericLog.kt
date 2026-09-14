package uz.oybek.ozamiz.data.logs.model

import androidx.annotation.Keep

@Keep
data class AdminGenericLog(
    val id: String,
    val title: String,
    val subtitle: String,
    val extraInfo: String,
    val photoUrl: String?,
    val timestamp: Long,
    val type: String
)
