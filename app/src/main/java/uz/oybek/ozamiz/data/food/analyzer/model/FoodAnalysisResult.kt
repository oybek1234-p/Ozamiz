package uz.oybek.ozamiz.data.food.analyzer.model

import androidx.annotation.Keep
import uz.oybek.ozamiz.data.logs.model.FoodLog

@Keep
data class AnalyzedIngredient(
    val name: String,
    val calories: Int?,
    val amount: Double?,
    val unit: String?
)

@Keep
data class FoodAnalysis(
    var mealType: String,
    var photoUrl: String? = null,
    val id: Int = 0,
    val result: FoodAnalysisResult? = null
) {
    fun toFoodLog(): FoodLog {
        return FoodLog(
            description = result?.name ?: "",
            calories = result?.calories ?: 0,
            protein = result?.protein,
            fat = result?.fat,
            carbs = result?.carbs,
            photoUrl = photoUrl,
            timestamp = System.currentTimeMillis(),
            mealType = mealType,
            aiInsights = result?.aiInsight,
            aiGenerated = true,
            healthLevel = result?.healthScore,
            logId = id.toString()
        )
    }
}

@Keep
data class FoodAnalysisResult(
    val name: String = "",
    val calories: Int = 0,
    val protein: Double? = null,
    val fat: Double? = null,
    val carbs: Double? = null,
    val ingredients: List<String>? = null,
    val healthScore: Int? = null,
    val aiInsight: String? = null,
    val timestamp: Long = System.currentTimeMillis(),
    
    // For interactive flow
    val isAmbiguous: Boolean = false,
    val question: String? = null,
    val options: List<String>? = null
)