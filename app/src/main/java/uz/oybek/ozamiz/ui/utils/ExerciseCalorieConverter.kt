package uz.oybek.ozamiz.ui.utils

import android.content.Context
import uz.oybek.ozamiz.R
import kotlin.math.abs

object ExerciseCalorieConverter {
    val caloriesFoods = listOf(
        20 to (R.string.food_honey_spoon to "🍯"),
        52 to (R.string.food_apple to "🍎"),
        60 to (R.string.food_milk_glass to "🥛"),
        70 to (R.string.food_boiled_egg to "🥚"),
        89 to (R.string.food_banana to "🍌"),
        90 to (R.string.food_yogurt to "🥛"),
        100 to (R.string.food_salad_plate to "🥗"),
        105 to (R.string.food_cookie to "🍪"),
        120 to (R.string.food_slice_bread to "🍞"),
        120 to (R.string.food_oatmeal to "🥣"),
        130 to (R.string.food_shorva to "🍲"),
        135 to (R.string.food_cola_glass to "🥤"),
        150 to (R.string.food_cheese_slice to "🧀"),
        160 to (R.string.food_almonds to "🌰"),
        170 to (R.string.food_green_tea to "🍵"),
        200 to (R.string.food_lavash to "🫓"),
        210 to (R.string.food_hot_dog to "🌭"),
        220 to (R.string.food_lagman_bowl to "🍜"),
        230 to (R.string.food_ice_cream to "🍦"),
        240 to (R.string.food_manti to "🥟"),
        250 to (R.string.food_kebab_100g to "🍢"),
        260 to (R.string.food_donut to "🍩"),
        270 to (R.string.food_pasta_plate to "🍝"),
        280 to (R.string.food_somsa to "🥐"),
        300 to (R.string.food_shashlik to "🍖"),
        320 to (R.string.food_fries to "🍟"),
        350 to (R.string.food_snickers to "🍫"),
        400 to (R.string.food_plov_bowl to "🍚"),
        450 to (R.string.food_pizza_slice to "🍕"),
        500 to (R.string.food_hamburger to "🍔")
    )

    fun getEquivalentFood(context: Context, kcal: Int): String {
        if (kcal <= 0) return ""
        val match = caloriesFoods.minByOrNull { abs(it.first - kcal) } ?: return ""
        val foodName = context.getString(match.second.first)
        val emoji = match.second.second
        return "$foodName $emoji"
    }

    fun getFatBurnedGram(kcal: Int): Double {
        if (kcal <= 0) return 0.0
        val raw = kcal / 7.7
        return String.format(java.util.Locale.US, "%.1f", raw).toDoubleOrNull() ?: 0.0
    }
}
