package uz.oybek.ozamiz.data.food.model

import uz.oybek.ozamiz.R

sealed class MealTime(
    val id: String,
    val name: String,
    val iconRes: Int
) {

    data object Breakfast : MealTime(
        id = "breakfast",
        name = "Nonushta",
        iconRes = R.drawable.breakfast_ic
    )

    data object Lunch : MealTime(
        id = "lunch",
        name = "Tushlik",
        iconRes = R.drawable.lunch_ic
    )

    data object Dinner : MealTime(
        id = "dinner",
        name = "Kechki ovqat",
        iconRes = R.drawable.dinner_ic
    )

    data object Snack : MealTime(
        id = "snack",
        name = "Tamaddi",
        iconRes = R.drawable.snack_ic
    )

    companion object {
        val mealTypes =
            listOf(Breakfast, Lunch, Dinner, Snack)

        fun fromId(id: String): MealTime? {
            return mealTypes.find { it.id == id }
        }
    }
}