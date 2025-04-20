import androidx.annotation.DrawableRes
import uz.oybek.ozamiz.R
import uz.oybek.ozamiz.ui.getinfo.ReminderItem
import uz.oybek.ozamiz.ui.getinfo.ReminderType

sealed class MealsType(
    val id: String,
    val name: String,
    @DrawableRes val iconRes: Int,
    val description: String,
    val defaultImageUrl: String? = null
) {

    data object Breakfast : MealsType(
        id = "breakfast",
        name = "Nonushta",
        iconRes = R.drawable.breakfast_ic,
        description = "Tavsiya: 6:00 dan 9:00 gacha"
    )

    data object Lunch : MealsType(
        id = "lunch",
        name = "Tushlik",
        iconRes = R.drawable.lunch_ic,
        description = "Tavsiya: 12:00 dan 14:00 gacha"
    )

    data object Dinner : MealsType(
        id = "dinner",
        name = "Kechki ovqat",
        iconRes = R.drawable.dinner_ic,
        description = ""
    )

    data object Snack : MealsType(
        id = "snack",
        name = "Yengil ovqat",
        iconRes = R.drawable.snack2,
        description = ""
    )

    fun toReminder() : ReminderItem {
        return ReminderItem(
            id = id,
            name = name,
            iconRes = iconRes,
            type = ReminderType.MEAL,
            description = description,
            imageUrl = defaultImageUrl
        )
    }

    companion object {
        val mealTypes = listOf(Breakfast, Lunch, Dinner)
    }
}
