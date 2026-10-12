package uz.oybek.ozamiz.data.logs.model

import androidx.annotation.Keep
import uz.oybek.ozamiz.data.analyzer.model.AnalyzedIngredient
import uz.oybek.ozamiz.data.logs.base.LogItem

@Keep
data class FoodLog(
    override var logId: String = "",
    var description: String = "",
    var calories: Int = 0,
    var protein: Double? = null,
    var fat: Double? = null,
    var carbs: Double? = null,
    var mealType: String = "",
    var photoUrl: String? = null,
    var aiInsights: String? = null,
    var aiGenerated: Boolean = true,
    var healthLevel: Int? = null,
    var timestamp: Any = System.currentTimeMillis(),
    var foodItems: List<AnalyzedIngredient>? = null,
    var advice: String? = null,
    var tags: List<String>? = null,
    var rawAnalysisJson: String? = null
) : LogItem {
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
        0L,
        null,
        null,
        null,
        null
    )
}