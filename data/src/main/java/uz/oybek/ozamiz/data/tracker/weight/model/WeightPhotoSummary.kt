package uz.oybek.ozamiz.data.tracker.weight.model

import androidx.annotation.Keep

@Keep
data class WeightPhotoSummary(
    val photoBeforeUrl: String? = null,
    val photoAfterUrl: String? = null,
    val photoBeforeTimestamp: Long? = null,
    val photoAfterTimestamp: Long? = null
)
