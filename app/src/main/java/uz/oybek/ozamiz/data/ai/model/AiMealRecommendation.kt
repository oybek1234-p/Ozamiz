package uz.oybek.ozamiz.data.ai.model

import uz.oybek.ozamiz.data.food.model.MealTime

data class AiMealRecommendation(
    val description: String,
    val mealTime: MealTime
)
