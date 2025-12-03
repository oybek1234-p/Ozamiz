package uz.oybek.ozamiz.data.food

import uz.oybek.ozamiz.data.user.local.LocalUser
import uz.oybek.ozamiz.data.user.model.User
import uz.oybek.ozamiz.data.user.model.info.Gender
import uz.oybek.ozamiz.data.user.model.info.UserActivityLevel
import java.util.Calendar
import java.util.Date
import kotlin.math.roundToInt

object DietController {


    private const val AVERAGE_WEIGHT_LOSS_PER_WEEK_KG = 0.7
    const val WEIGHT_CALORIE_LOSS_TARGET = -750

    fun calculateEstimatedGoalDate(): Date {
        val user = LocalUser.user
        val currentWeight = user.weight
        val targetWeight = user.targetWeight ?: return Date()

        if (currentWeight == targetWeight) return Date()

        val diff = kotlin.math.abs(currentWeight - targetWeight)
        val weeksNeeded = diff / AVERAGE_WEIGHT_LOSS_PER_WEEK_KG
        val daysNeeded = (weeksNeeded * 7).toInt()

        val calendar = Calendar.getInstance()
        calendar.add(Calendar.DAY_OF_YEAR, daysNeeded)
        return calendar.time
    }

    fun getIdealWeight(heightM: Double): Pair<Int, Int> {
        val pair = Pair(
            (18.5 * heightM * heightM).toInt(),
            (24.9 * heightM * heightM).toInt()
        )
        return pair
    }

    fun calculateBMR(user: User): Double {
        return if (user.gender == Gender.MALE) {
            (10 * user.weight) +
                    (6.25 * user.height) -
                    (5 * user.age) + 5
        } else {
            (10 * user.weight) +
                    (6.25 * user.height) -
                    (5 * user.age) - 161
        }
    }

    fun calculateTDEE(user: User): Double {
        val bmr = calculateBMR(user)
        val activityLevel =
            UserActivityLevel.valueOf(user.activityLevel)
        return bmr * activityLevel.multiplier
    }

    fun calculateTargetCalories(user: User): Int {
        val tdee = calculateTDEE(user)
        val target = tdee + WEIGHT_CALORIE_LOSS_TARGET
        val minimumCalories = 1200.0
        return maxOf(target, minimumCalories).roundToInt()
    }

    fun calculateTargetWaterMl(weightKg: Double): Int {
        val waterMl = weightKg * 35
        return (waterMl / 100).roundToInt() * 100
    }

    fun calculateMacrosByPercent(
        totalCalories: Int,
        proteinPercent: Double = 0.30,
        fatPercent: Double = 0.25,
        carbPercent: Double = 0.45
    ): Macros {
        if (totalCalories <= 0) return Macros()

        val proteinCalories = totalCalories * proteinPercent
        val fatCalories = totalCalories * fatPercent
        val carbCalories = totalCalories * carbPercent

        val proteinGr = (proteinCalories / 4).roundToInt()
        val fatGr = (fatCalories / 9).roundToInt()
        val carbsGr = (carbCalories / 4).roundToInt()

        return Macros(
            calories = totalCalories,
            protein = proteinGr,
            fat = fatGr,
            carbs = carbsGr
        )
    }

    data class Macros(
        val calories: Int = 0,
        val protein: Int = 0,
        val fat: Int = 0,
        val carbs: Int = 0
    )
}