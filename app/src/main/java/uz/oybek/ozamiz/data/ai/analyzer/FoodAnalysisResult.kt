package uz.oybek.ozamiz.data.ai.analyzer

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
            aiInsights = buildString {
                val prosStr = result?.pros?.joinToString("\n") { "✅ $it" }
                if (!prosStr.isNullOrBlank()) append(prosStr)
                
                val consStr = result?.cons?.joinToString("\n") { "❌ $it" }
                if (!consStr.isNullOrBlank()) {
                    if (isNotEmpty()) append("\n")
                    append(consStr)
                }

                val advStr = result?.advice
                if (!advStr.isNullOrBlank()) {
                    if (isNotEmpty()) append("\n\n")
                    append("💡 $advStr")
                }
            }.takeIf { it.isNotBlank() },
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
    val foodItems: List<AnalyzedIngredient>? = null,
    val healthScore: Int? = null,
    val pros: List<String>? = null,
    val cons: List<String>? = null,
    val advice: String? = null,
    val timestamp: Long = System.currentTimeMillis(),
    val limitBread: Boolean? = null,
    val sodaDrink: Boolean? = null,
    val heavyDinner: Boolean? = null,
    val limitCalorie: Boolean? = null,
    val waterBeforeMeal: Boolean? = null
) {
    fun getNormalizedFoodItems(): List<AnalyzedIngredient> {
        if (!foodItems.isNullOrEmpty()) return foodItems
        if (!ingredients.isNullOrEmpty()) {
            return ingredients.map { itemStr ->
                AnalyzedIngredient(name = itemStr, calories = null, amount = null, unit = null)
            }
        }
        return emptyList()
    }
}