package uz.oybek.ozamiz.data.food.model

import androidx.annotation.Keep
import kotlin.math.roundToInt

@Keep
data class FoodItem(
    val id: String = "",
    val name: String = "",
    val caloriesPer100g: Double = 0.0,
    val proteinPer100g: Double = 0.0,
    val fatPer100g: Double = 0.0,
    val carbsPer100g: Double = 0.0,
    val servingSizes: List<ServingSize> = emptyList(),
    val isVerified: Boolean = false,
    val category: String = "",
    val isPopular: Boolean = false,
    val searchAliases: List<String> = emptyList()
) {
    val defaultServing: ServingSize
        get() = servingSizes.firstOrNull() ?: ServingSize(description = "100 g", grams = 100.0)

    fun caloriesForGrams(grams: Double): Int =
        (caloriesPer100g * grams / 100.0).roundToInt()

    fun proteinForGrams(grams: Double): Double =
        proteinPer100g * grams / 100.0

    fun fatForGrams(grams: Double): Double =
        fatPer100g * grams / 100.0

    fun carbsForGrams(grams: Double): Double =
        carbsPer100g * grams / 100.0

    fun defaultPortionDescription(): String {
        val serving = servingSizes.firstOrNull() ?: return "100 g"
        return "1 ${serving.description.lowercase()}"
    }

    fun defaultLoggedDescription(): String =
        "$name (${defaultPortionDescription()})"
}

@Keep
data class ServingSize(
    val description: String = "",
    val grams: Double = 0.0
)
