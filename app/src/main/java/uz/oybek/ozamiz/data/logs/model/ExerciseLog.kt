package uz.oybek.ozamiz.data.logs.model

import androidx.annotation.Keep
import com.google.firebase.database.IgnoreExtraProperties
import uz.oybek.ozamiz.data.logs.base.LogItem

@Keep
@IgnoreExtraProperties
data class ExerciseLog(
    override var logId: String = "",
    var exerciseType: String = "",
    var durationMin: Int = 0,
    var caloriesBurned: Int = 0,
    var timestamp: Any? = null,
    var timestampLong: Long = System.currentTimeMillis()
) : LogItem {
    constructor() : this("", "", 0, 0, null, System.currentTimeMillis())
}