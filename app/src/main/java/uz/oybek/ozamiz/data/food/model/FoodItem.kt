package uz.oybek.ozamiz.data.food.model

data class FoodItem(
    val id: String = "",
    val name: String = "",
    val caloriesPer100g: Double = 0.0,
    val proteinPer100g: Double = 0.0,
    val fatPer100g: Double = 0.0,
    val carbsPer100g: Double = 0.0,
    val servingSizes: List<ServingSize> = emptyList(),
    val isVerified: Boolean = false,
    val category: String = ""
)

data class ServingSize(
    val label: String = "",
    val weight: Double = 0.0 // in grams
)
