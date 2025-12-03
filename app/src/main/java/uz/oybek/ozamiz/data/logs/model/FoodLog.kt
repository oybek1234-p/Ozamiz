package uz.oybek.ozamiz.data.logs.model

import androidx.annotation.Keep
import com.google.firebase.database.ServerValue

@Keep
data class FoodLog(
    var logId: String = "",
    var description: String = "",
    var calories: Int = 0,
    var protein: Double? = null,
    var fat: Double? = null,
    var carbs: Double? = null,
    var mealType: String = "",
    var photoUrl: String? = null,
    var aiInsights: String? = null,
    var aiGenerated: Boolean = true,
    var healthLevel: String? = null,
    var timestamp: Any = ServerValue.TIMESTAMP
) {
    constructor() : this(
        "",
        "",
        0,
        null,
        null,
        null,
        "",
        null,
        null,
        true,
        null,
        0L
    )
}