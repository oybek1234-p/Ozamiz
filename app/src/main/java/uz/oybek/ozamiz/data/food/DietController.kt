package uz.oybek.ozamiz.data.food

import uz.oybek.ozamiz.data.user.local.LocalUser
import uz.oybek.ozamiz.data.user.model.User
import uz.oybek.ozamiz.data.user.model.info.Gender
import uz.oybek.ozamiz.data.user.model.info.UserActivityLevel
import java.util.Calendar
import java.util.Date
import kotlin.math.roundToInt

object DietController {

    private const val AVERAGE_WEIGHT_LOSS_PER_WEEK_KG = 0.5
    const val DEFICIT_CALORIES = 500
    const val SURPLUS_CALORIES = 400

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

    fun getIdealWeight(heightCm: Double): Pair<Int, Int> {
        val heightM = heightCm / 100.0
        val low = (18.5 * heightM * heightM).toInt()
        val high = (24.9 * heightM * heightM).toInt()
        return Pair(low, high)
    }

    fun calculateBMI(weightKg: Double, heightCm: Double): Double {
        if (heightCm <= 0) return 0.0
        val heightM = heightCm / 100.0
        return weightKg / (heightM * heightM)
    }

    fun calculateBMR(user: User): Double {
        return if (user.gender == Gender.MALE) {
            (10 * user.weight) + (6.25 * user.height) - (5 * user.age) + 5
        } else {
            (10 * user.weight) + (6.25 * user.height) - (5 * user.age) - 161
        }
    }

    fun calculateTDEE(user: User): Double {
        val bmr = calculateBMR(user)
        val activityLevel = UserActivityLevel.fromString(user.activityLevel)
        return bmr * activityLevel.multiplier
    }

    fun calculateTargetCalories(user: User): Int {
        val tdee = calculateTDEE(user)
        val target = if (user.goal == "gain_weight") {
            tdee + SURPLUS_CALORIES
        } else {
            tdee - DEFICIT_CALORIES
        }
        val minimumCalories = if (user.gender == Gender.MALE) 1500.0 else 1200.0
        return maxOf(target, minimumCalories).roundToInt()
    }

    fun calculateTargetWaterMl(weightKg: Double): Int {
        val waterMl = weightKg * 35
        return (waterMl / 100).roundToInt() * 100
    }

    fun updateTargetValues(user: User) {
        val targetCals = calculateTargetCalories(user)
        user.targetCalories = targetCals

        val macros = if (user.goal == "gain_weight") {
            calculateMacrosByPercent(targetCals, proteinPercent = 0.25, fatPercent = 0.25, carbPercent = 0.50)
        } else {
            calculateMacrosByPercent(targetCals, proteinPercent = 0.35, fatPercent = 0.25, carbPercent = 0.40)
        }
        
        user.targetProtein = macros.protein
        user.targetFat = macros.fat
        user.targetCarbs = macros.carbs
        user.targetWaterMl = calculateTargetWaterMl(user.weight)
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