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
        iconRes = R.drawable.bread
    )

    data object Lunch : MealTime(
        id = "lunch",
        nameRes = R.string.lunch,
        iconRes = R.drawable.lunch_simple
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

        fun fromId(id: String?): MealTime? {
            if (id.isNullOrBlank()) return null
            return mealTypes.find { it.id.equals(id.trim(), ignoreCase = true) }
        }

        fun getMealByTime(timestamp: Long = System.currentTimeMillis()): MealTime {
            val cal = java.util.Calendar.getInstance()
            if (timestamp > 0) {
                cal.timeInMillis = timestamp
            }
            val hour = cal.get(java.util.Calendar.HOUR_OF_DAY)
            return when (hour) {
                in 5..10 -> Breakfast
                in 11..15 -> Lunch
                in 16..22 -> Dinner
                else -> Snack
            }
        }

        fun getValidMeal(meal: String?, timestamp: Long = System.currentTimeMillis()): String {
            return fromId(meal)?.id ?: getMealByTime(timestamp).id
        }
    }
}