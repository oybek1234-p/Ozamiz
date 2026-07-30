package uz.oybek.ozamiz.data.logs.model

import androidx.annotation.Keep
import com.google.firebase.database.IgnoreExtraProperties
import uz.oybek.ozamiz.data.logs.base.LogItem

@Keep
@IgnoreExtraProperties
data class WaterLog(
    override var logId: String = "",
    var amountMl: Int = 0,
    var timestamp: Any? = null,
    var timestampLong: Long = System.currentTimeMillis()
) : LogItem {
    constructor() : this("", 0, null, System.currentTimeMillis())
}