package uz.oybek.ozamiz.data.utils

import uz.oybek.ozamiz.data.User
import uz.oybek.ozamiz.data.info.ActivityLevel
import uz.oybek.ozamiz.data.info.GenderEnum
import uz.oybek.ozamiz.data.weightGoalModifier
import kotlin.math.roundToInt

object NutritionCalculator {

    fun getIdealWeight(heightM: Double): Pair<Int, Int> {
        val pair = Pair((18.5 * heightM * heightM).toInt(),(24.9 * heightM * heightM).toInt())
        return pair
    }

    /**
     * Mifflin-St Jeor formulasidan foydalanib Bazal Metabolizm Tezligini (BMR) hisoblaydi.
     */
    fun calculateBMR(user: User): Double {
        if (GenderEnum.fromValue(user.gender) == GenderEnum.UNKNOWN || user.age <= 0 || user.height <= 0 || user.weight <= 0) {
            return 0.0 // Ma'lumotlar yetarli emas yoki noto'g'ri
        }

        return if (GenderEnum.fromValue(user.gender) == GenderEnum.MALE) {
            (10 * user.weight) + (6.25 * user.height) - (5 * user.age) + 5
        } else {
            (10 * user.weight) + (6.25 * user.height) - (5 * user.age) - 161
        }
    }

    /**
     * Umumiy Kunlik Energiya Sarfini (TDEE) hisoblaydi.
     */
    fun calculateTDEE(user: User): Double {
        val bmr = calculateBMR(user)
        if (bmr <= 0) return 0.0

        val activityLevel = ActivityLevel.fromId(user.activityLevel)
        return bmr * activityLevel.multiplier
    }

    fun calculateTargetCalories(user: User): Int {
        val tdee = calculateTDEE(user)
        if (tdee <= 0) return 2000
        val target = tdee + weightGoalModifier

        val minimumCalories = 1200.0
        return maxOf(target, minimumCalories).roundToInt()
    }

    fun calculateTargetWaterMl(weightKg: Double): Int {
        if (weightKg <= 0) return 2500 // Default
        val waterMl = weightKg * 35
        return (waterMl / 100).roundToInt() * 100
    }

    fun distributeCaloriesForMeals(totalTargetCalories: Int): Map<String, Int> {
        if (totalTargetCalories <= 0) return emptyMap()

        val breakfastPercent = 0.25 // 25%
        val lunchPercent = 0.35     // 35%
        val dinnerPercent = 0.25    // 25%
        val snackPercent = 0.15     // 15%

        return mapOf(
            "breakfast" to (totalTargetCalories * breakfastPercent).roundToInt(),
            "lunch" to (totalTargetCalories * lunchPercent).roundToInt(),
            "dinner" to (totalTargetCalories * dinnerPercent).roundToInt(),
            "snack" to (totalTargetCalories * snackPercent).roundToInt()
        )
    }
}