package uz.oybek.ozamiz.data.logs.model

import androidx.annotation.Keep
import com.google.firebase.database.ServerValue

@Keep
data class WaterLog(
    var logId: String = "",
    var amountMl: Int = 0,
    var timestamp: Any = ServerValue.TIMESTAMP
) {
    constructor() : this(
        "",
        0,
        0L
    )
}