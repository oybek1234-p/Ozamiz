package uz.oybek.ozamiz.data.logs.model

import androidx.annotation.Keep
import kotlinx.serialization.Serializable
import uz.oybek.ozamiz.data.logs.base.LogItem

@Keep
@Serializable
@com.google.firebase.firestore.IgnoreExtraProperties
data class WeightLog(
    override var logId: String = "",
    var userId: String = "",
    var weightKg: Double = 0.0,
    var photoUrl: String = "",
    var timestamp: Any? = null,
    var timestampLong: Long = 0L,
    var stability: Double = 0.0
) : LogItem {

    constructor() : this("", "", 0.0, "", null, 0L, 0.0)

    fun getEffectiveTimestamp(): Long {
        return when (val ts = timestamp) {
            is Long -> ts
            is com.google.firebase.Timestamp -> ts.toDate().time
            is Map<*, *> -> {
                timestampLong.takeIf { it > 0 } ?: System.currentTimeMillis()
            }
            else -> timestampLong.takeIf { it > 0 } ?: System.currentTimeMillis()
        }
    }
}