package uz.oybek.ozamiz.data.food.model

import uz.oybek.ozamiz.R

sealed class MealTime(
    val id: String,
    val nameRes: Int,
    val iconRes: Int
) {

    data object Breakfast : MealTime(
        id = "breakfast",
        nameRes = R.string.breakfast,
        iconRes = R.drawable.breakfast_ic
    )

    data object Lunch : MealTime(
        id = "lunch",
        nameRes = R.string.lunch,
        iconRes = R.drawable.lunch_ic
    )

    data object Dinner : MealTime(
        id = "dinner",
        nameRes = R.string.dinner,
        iconRes = R.drawable.dinner_ic
    )

    data object Snack : MealTime(
        id = "snack",
        nameRes = R.string.snack,
        iconRes = R.drawable.snack_ic
    )

    companion object {
        val mealTypes: List<MealTime>
            get() = listOf(Breakfast, Lunch, Dinner, Snack)

        fun fromId(id: String): MealTime? {
            return mealTypes.find { it.id == id }
        }
    }
}