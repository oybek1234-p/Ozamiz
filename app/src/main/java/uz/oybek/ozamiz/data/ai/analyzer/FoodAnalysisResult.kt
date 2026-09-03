package uz.oybek.ozamiz.data.ai.analyzer

import androidx.annotation.Keep
import uz.oybek.ozamiz.data.logs.model.FoodLog
import uz.oybek.ozamiz.data.utils.roundTo1Decimal

@Keep
data class AnalyzedIngredient(
    val name: String = "",
    val calories: Int? = null,
    val amount: Double? = null,
    val unit: String? = null,
    val protein: Double? = null,
    val fat: Double? = null,
    val carbs: Double? = null
) {
    constructor() : this("", null, null, null, null, null, null)

    fun clean(): AnalyzedIngredient = copy(
        amount = amount?.roundTo1Decimal(),
        protein = protein?.roundTo1Decimal(),
        fat = fat?.roundTo1Decimal(),
        carbs = carbs?.roundTo1Decimal()
    )
}

@Keep
data class SingleIngredientResult(
    val name: String = "",
    val calories: Int = 0,
    val amount: Double? = null,
    val unit: String? = null,
    val protein: Double? = null,
    val fat: Double? = null,
    val carbs: Double? = null
) {
    fun toAnalyzedIngredient() = AnalyzedIngredient(
        name = name,
        calories = calories,
        amount = amount?.roundTo1Decimal(),
        unit = unit,
        protein = protein?.roundTo1Decimal(),
        fat = fat?.roundTo1Decimal(),
        carbs = carbs?.roundTo1Decimal()
    )
}

@Keep
data class FoodAnalysis(
    var mealType: String,
    var photoUrl: String? = null,
    val id: Int = 0,
    val result: FoodAnalysisResult? = null
) {
    fun toFoodLog(): FoodLog {
        val rawJson = result?.let { uz.oybek.ozamiz.data.utils.gson?.toJson(it) }
        val ts = result?.timestamp?.takeIf { it > 0 } ?: System.currentTimeMillis()
        val normalizedMeal = uz.oybek.ozamiz.data.food.model.MealTime.getValidMeal(mealType, ts)
        return FoodLog(
            description = result?.name ?: "",
            calories = result?.getEffectiveCalories() ?: 0,
            protein = result?.getEffectiveProtein()?.roundTo1Decimal(),
            fat = result?.getEffectiveFat()?.roundTo1Decimal(),
            carbs = result?.getEffectiveCarbs()?.roundTo1Decimal(),
            photoUrl = photoUrl,
            timestamp = ts,
            mealType = normalizedMeal,
            aiInsights = result?.advice?.takeIf { it.isNotBlank() },
            aiGenerated = true,
            healthLevel = result?.healthScore,
            logId = id.toString(),
            foodItems = result?.foodItems?.map { it.clean() },
            advice = result?.advice,
            tags = result?.tags,
            rawAnalysisJson = rawJson
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
    val foodItems: List<AnalyzedIngredient>? = null,
    val healthScore: Int? = null,
    val pros: List<String>? = null,
    val cons: List<String>? = null,
    val advice: String? = null,
    val tags: List<String>? = null,
    val timestamp: Long = System.currentTimeMillis(),
    val portionMultiplier: Double? = 1.0
) {
    fun getEffectiveCalories(): Int {
        val sum = foodItems?.sumOf { it.calories ?: 0 } ?: 0
        return if (sum > 0) sum else calories
    }

    fun getEffectiveProtein(): Double? {
        val sum = foodItems?.mapNotNull { it.protein }?.takeIf { it.isNotEmpty() }?.sum()
        return (sum ?: protein)?.roundTo1Decimal()
    }

    fun getEffectiveFat(): Double? {
        val sum = foodItems?.mapNotNull { it.fat }?.takeIf { it.isNotEmpty() }?.sum()
        return (sum ?: fat)?.roundTo1Decimal()
    }

    fun getEffectiveCarbs(): Double? {
        val sum = foodItems?.mapNotNull { it.carbs }?.takeIf { it.isNotEmpty() }?.sum()
        return (sum ?: carbs)?.roundTo1Decimal()
    }

    fun getNormalizedFoodItems(): List<AnalyzedIngredient> {
        if (!foodItems.isNullOrEmpty()) return foodItems
        if (name.isNotBlank()) {
            val parts = name.split(",").map { it.trim() }.filter { it.isNotBlank() }
            if (parts.isNotEmpty()) {
                return parts.map { partName ->
                    AnalyzedIngredient(
                        name = partName,
                        calories = if (parts.size == 1) calories else null,
                        amount = null,
                        unit = null,
                        protein = if (parts.size == 1) protein else null,
                        fat = if (parts.size == 1) fat else null,
                        carbs = if (parts.size == 1) carbs else null
                    )
                }
            }
        }
        return emptyList()
    }

    fun getCleanTags(): List<String> {
        val forbiddenKeywords = setOf(
            "tushlik", "nonushta", "kechki", "tamaddi", "snack",
            "energiya", "manbai", "to'yimli", "toyimli", "to'yimsiz", "toyimsiz",
            // har bir ovqatga noto'g'ri qo'shiladigan umumiy taglar:
            "uglevodga", "shakarli", "shakar", "oqsilga", "yog'ga", "yog'li",
            "vitaminlarga", "mineralga", "kalorik", "kaloriyali",
            "past kaloriya", "yuqori kaloriya"
        )
        return tags?.filter { tag ->
            val lower = tag.lowercase().trim()
            forbiddenKeywords.none { lower.contains(it) }
        } ?: emptyList()
    }
}