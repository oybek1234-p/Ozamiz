package uz.oybek.ozamiz.data.ai

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.firstOrNull
import uz.oybek.ozamiz.R
import uz.oybek.ozamiz.common.util.DateFormatter
import uz.oybek.ozamiz.common.util.DateFormatter.PATTERN_HOUR_MINUTE
import uz.oybek.ozamiz.data.profile.model.User
import uz.oybek.ozamiz.data.tracker.api.DailyTrackerRepo
import uz.oybek.ozamiz.data.tracker.api.FoodTrackerRepo
import uz.oybek.ozamiz.data.tracker.model.MealType
import uz.oybek.ozamiz.ui.modelUtils.getDisplayName
import java.util.Date
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AiPromptBuilder @Inject constructor(
    private val dailyTrackerRepo: DailyTrackerRepo,
    private val foodTrackerRepo: FoodTrackerRepo,
    @param:ApplicationContext private val context: Context
) {

    private suspend fun getGroupedFoodLogs(date: Date = Date()): String {
        val logs = foodTrackerRepo.observeFoodLogs(date).firstOrNull()
        if (logs.isNullOrEmpty()) return "Bugun hali hech qanday ovqat kiritilmagan."

        return logs.groupBy { food ->
            val meal = food.mealType
            MealType.fromIdOrNull(meal)?.getDisplayName(context)
                ?: meal.takeIf { it.isNotBlank() }?.replaceFirstChar { it.uppercase() }
                ?: context.getString(R.string.admin_field_unknown)
        }.map { (mealTitle, list) ->
            "• $mealTitle: " + list.joinToString("; ") { food ->
                val foodName = food.description.ifBlank { "Taom" }
                val macros =
                    "oqsil: ${food.protein}g, yog': ${food.fat}g, uglevod: ${food.carbs}g"
                "$foodName (${food.calories} kkal, $macros)"
            }
        }.joinToString("\n")
    }

    suspend fun buildFoodAnalysisContext(user: User, mealType: String?, timestamp: Long): String {
        val date = Date(timestamp)
        val timeStr = DateFormatter.format(date, PATTERN_HOUR_MINUTE)
        val dailySummary = dailyTrackerRepo.getDailySummary(date)
        val goalText = user.goal
        val genderText = if (user.gender == 1) {
            context.getString(R.string.gender_female)
        } else {
            context.getString(R.string.gender_male)
        }
        val remainingCalories = (user.targetCalories - dailySummary.consumedCalories).coerceAtLeast(0)
        val safeMeal = mealType?.takeIf { it.isNotBlank() }
            ?.let { MealType.fromIdOrNull(it)?.getDisplayName(context) ?: it }
            ?: context.getString(R.string.admin_field_unknown)

        return """
            Foydalanuvchi maqsadi: $goalText.
            Profil: ${user.age} yosh, $genderText, vazn: ${user.weight} kg, bo'y: ${user.height} sm.
            Ovqatlanish vaqti: $timeStr, Taom mahali: $safeMeal.
            Bugungi me'yor va statistika:
            - Kaloriya: ${dailySummary.consumedCalories}/${user.targetCalories} kkal (Qolgan limit: $remainingCalories kkal)
            - Oqsil: ${dailySummary.consumedProtein}g, Yog': ${dailySummary.consumedFat}g, Uglevod: ${dailySummary.consumedCarbs}g.
            Bugun hozirgacha yeyilgan ovqatlar:
            ${getGroupedFoodLogs(date)}
        """.trimIndent()
    }
}