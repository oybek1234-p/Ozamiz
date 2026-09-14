package uz.oybek.ozamiz.ui.feature.home

import androidx.annotation.Keep
import uz.oybek.ozamiz.data.food.model.MealTime

@Keep
data class AiMealRecommendation(
    val description: String,
    val mealTime: MealTime
)
