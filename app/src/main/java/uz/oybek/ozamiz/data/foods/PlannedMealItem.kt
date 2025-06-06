// data/foods/PlannedMealItem.kt
package uz.oybek.ozamiz.data.foods

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class PlannedMealItem(
    var name: String = "",
    var calories: Int? = null,
    var protein: Double? = null,
    var fat: Double? = null,
    var carbs: Double? = null,
    var ingredients: List<String> = emptyList(),
    var preparationSteps: List<String>? = null,
    var healthBenefits: String? = null,
    var mealTypeTag: String = "" // masalan, "breakfast", "lunch"
) : Parcelable {
    constructor() : this(
        name = "",
        calories = null,
        protein = null,
        fat = null,
        carbs = null,
        ingredients = emptyList(),
        preparationSteps = null,
        healthBenefits = null,
        mealTypeTag = ""
    )
}