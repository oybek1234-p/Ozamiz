package uz.oybek.ozamiz.data.food.analyzer.model

import kotlinx.serialization.Serializable

@Serializable
data class AnalyzedIngredient(
    val name: String,
    val calories: Int?
)

@Serializable
data class FoodAnalysisResult(
    val calories: Int,
    val protein: Double?,
    val fat: Double?,
    val carbs: Double?,
    val ingredients: List<AnalyzedIngredient>,
    val isHealthy: String?,
    val feedback: String?,
    val name: String? = null
)