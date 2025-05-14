package uz.oybek.ozamiz.data.info

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
    var analysisInsights: String? = null,
    var estimatedByAI: Boolean = true,
    var timestamp: Any = ServerValue.TIMESTAMP
) {
    constructor() : this("", "", 0, null, null, null, "", null, null, true, 0L)
}