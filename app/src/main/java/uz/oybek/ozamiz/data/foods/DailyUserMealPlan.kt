// data/foods/DailyUserMealPlan.kt
package uz.oybek.ozamiz.data.foods

import com.google.firebase.database.Exclude
import com.google.firebase.database.IgnoreExtraProperties
import com.google.firebase.database.ServerValue

@IgnoreExtraProperties // Firebase uchun noma'lum maydonlarni e'tiborsiz qoldirish
data class DailyUserMealPlan(
    var userId: String = "",
    var dayOfWeek: String = "", // "monday", "tuesday", ..., "sunday"
    var breakfast: PlannedMealItem? = null,
    var lunch: PlannedMealItem? = null,
    var dinner: PlannedMealItem? = null,
    var snack: PlannedMealItem? = null,
    var timestamp: Any = ServerValue.TIMESTAMP, // Long yoki ServerValue.TIMESTAMP
    var generatedByAI: Boolean = true
) {
    constructor() : this(
        userId = "",
        dayOfWeek = "",
        breakfast = null,
        lunch = null,
        dinner = null,
        snack = null,
        timestamp = 0L, // Firebase ServerValue.TIMESTAMP bilan to'ldiriladi
        generatedByAI = true
    )

    @Exclude
    fun getMealByType(mealType: String): PlannedMealItem? {
        return when (mealType) {
            "breakfast" -> breakfast
            "lunch" -> lunch
            "dinner" -> dinner
            "snack" -> snack
            else -> null
        }
    }

    @Exclude
    fun updateMealByType(mealType: String, mealItem: PlannedMealItem?) {
        when (mealType) {
            "breakfast" -> breakfast = mealItem
            "lunch" -> lunch = mealItem
            "dinner" -> dinner = mealItem
            "snack" -> snack = mealItem
        }
    }

    @Exclude
    fun isEmpty(): Boolean {
        return breakfast == null && lunch == null && dinner == null && snack == null
    }
}