package uz.oybek.ozamiz.data.gamification

import android.os.Parcelable
import androidx.annotation.DrawableRes
import androidx.annotation.Keep
import kotlinx.parcelize.Parcelize
import uz.oybek.ozamiz.R // R drawable uchun

@Keep
object GamificationConstants {

    const val POINTS_HEALTHY_MEAL = 10
    const val POINTS_HEALTHY_SNACK = 5
    const val POINTS_DAILY_GOAL_ALL_HEALTHY = 15 // Agar kunlik barcha ovqatlar sog'lom bo'lsa
    const val POINTS_STREAK_7_DAYS = 30 // 7 kunlik sog'lom ovqatlanish uchun bonus

    const val POINTS_UNHEALTHY_MEAL = -5
    const val POINTS_SKIP_LOGGING_PASSIVE = -3 // Ovqat kiritmaslik uchun (hozircha ishlatilmaydi)
    const val POINTS_EXTRA_UNHEALTHY_DAY = -5 // Kuniga 2+ nosog'lom ovqat uchun

    val BADGES_LIST: List<Badge> = listOf(
        Badge("beginner_leaf", "Sog‘lom boshlovchi", "Sog'lom hayot tarziga ilk qadam!", R.drawable.apple_ic, 100),
        Badge("meal_master", "Ovqat ustasi", "Sog'lom ovqat tanlashda ustasiz!", R.drawable.apple_ic, 200),
        Badge("calorie_controller", "Kaloriya nazoratchisi", "Kaloriyalarni mohirona boshqarasiz!", R.drawable.apple_ic, 400),
        Badge("motivation_torch", "Motivatsiya mash’alasi", "Siz boshqalarga ilhom berasiz!", R.drawable.apple_ic, 600),
        Badge("health_star", "Sog‘liq yulduzi", "Sog'lom turmush tarzi yulduzisiz!", R.drawable.apple_ic, 1000),
        Badge("inspiration_bulb", "Ilhom manbai", "Yangi g'oyalar va kuch manbaisiz!", R.drawable.apple_ic, 1500),
        Badge("ozamiz_master", "Ozamiz ustasi", "Ozish va sog'lom bo'lishda haqiqiy ustasiz!", R.drawable.apple_ic, 2000)
    )

    @Parcelize
    data class RecommendedSnackFood(
        val name: String,
        @DrawableRes val iconRes: Int,
        val mealType: String
    ) : Parcelable

    val FRUITS_FOR_SNACK: List<RecommendedSnackFood> = listOf(
        RecommendedSnackFood("Olma", R.drawable.apple_ic, "breakfast_done"),
        RecommendedSnackFood("Banan", R.drawable.apple_ic, "breakfast_done"),
        RecommendedSnackFood("Nok", R.drawable.apple_ic, "breakfast_done"),
        RecommendedSnackFood("Qatiq", R.drawable.apple_ic, "breakfast_done"),
        RecommendedSnackFood("Kefir", R.drawable.apple_ic, "breakfast_done"),
        RecommendedSnackFood("Tvorog", R.drawable.apple_ic, "breakfast_done"),
        RecommendedSnackFood("Kivi", R.drawable.apple_ic, "breakfast_done"),
        RecommendedSnackFood("Qaynatilgan tuxum", R.drawable.apple_ic, "breakfast_done"),
        RecommendedSnackFood("Yong'oq", R.drawable.apple_ic, "breakfast_done"),
        RecommendedSnackFood("Xurmo", R.drawable.apple_ic, "breakfast_done"),
    )

    val NUTS_AND_DAIRY_FOR_SNACK: List<RecommendedSnackFood> = listOf(
        RecommendedSnackFood("Olma", R.drawable.apple_ic, "breakfast_done"),
    )

    fun getSnackRecommendationsForMeal(mealTypeJustFinished: String): List<RecommendedSnackFood> {
        return when (mealTypeJustFinished.lowercase()) {
            "breakfast" -> FRUITS_FOR_SNACK
            "lunch" -> NUTS_AND_DAIRY_FOR_SNACK
            else -> emptyList() // Dinner or other snacks, no specific recommendations for now
        }
    }
}