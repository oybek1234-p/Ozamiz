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
    val category: String = "",
    val isPopular: Boolean = false,
    val searchAliases: List<String> = emptyList()
)

data class ServingSize(
    val description: String = "",
    val grams: Double = 0.0
)
