package uz.oybek.ozamiz.data.logs.model

import androidx.annotation.Keep
import com.google.firebase.database.ServerValue
import uz.oybek.ozamiz.data.logs.base.LogItem

@Keep
data class ExerciseLog(
    override var logId: String = "",
    var exerciseType: String = "",
    var durationMin: Int = 0,
    var caloriesBurned: Int = 0,
    var timestamp: Any = ServerValue.TIMESTAMP
) : LogItem {
    constructor() : this(
        "",
        "",
        0,
        0,
        0L
    )
}