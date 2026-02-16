package uz.oybek.ozamiz.data.logs.model

import androidx.annotation.Keep
import com.google.firebase.database.ServerValue
import uz.oybek.ozamiz.data.logs.base.LogItem

@Keep
data class WaterLog(
    override var logId: String = "",
    var amountMl: Int = 0,
    var timestamp: Any = ServerValue.TIMESTAMP
) : LogItem {
    constructor() : this(
        "",
        0,
        0L
    )
}