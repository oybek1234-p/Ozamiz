package uz.oybek.ozamiz.data.logs.model

import android.os.Parcelable
import androidx.annotation.Keep
import com.google.firebase.database.ServerValue
import kotlinx.serialization.Serializable
import uz.oybek.ozamiz.data.logs.base.LogItem
import kotlinx.parcelize.Parcelize
import kotlinx.parcelize.RawValue

@Keep
@Serializable
@Parcelize
data class WeightLog(
    override var logId: String = "",
    val userId: String = "",
    var timestamp: @RawValue @kotlinx.serialization.Contextual Any = ServerValue.TIMESTAMP,
    var weightKg: Double = 0.0,
    var photoUrl: String = ""
) : LogItem, Parcelable {

    @kotlinx.serialization.Transient
    val timestampLong: Long
        get() = when (val ts = timestamp) {
            is Long -> ts
            is Double -> ts.toLong()
            else -> System.currentTimeMillis()
        }

    constructor() : this(
        "",
        "",
        0L,
        0.0,
        ""
    )
}