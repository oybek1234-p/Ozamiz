package uz.oybek.ozamiz.data.ai

import uz.oybek.ozamiz.data.logs.LogRepository
import uz.oybek.ozamiz.data.user.local.user

object AiPromptBuilder {

    const val MODEL_CHAT = "gemini-flash-latest"

    private suspend fun getGroupedFoodLogs(): String {
        val today = java.util.Date()
        val logs = LogRepository.getDailyLogsSync(today)?.foodLogs?.values
        if (logs.isNullOrEmpty()) return "Bugun hali hech qanday ovqat kiritilmagan."

        return logs.groupBy { it.mealType }.map { (mealType, list) ->
                "• $mealType: " + list.joinToString("; ") { food ->
                    val macros =
                        "oqsil: ${food.protein}g, yog': ${food.fat}g, uglevod: ${food.carbs}g"
                    "${food.description} (${food.calories} kkal, $macros)"
                }
            }.joinToString("\n")
    }

    suspend fun buildUserContext(): String {
        val today = java.util.Date()
        val dailySummary = LogRepository.getDailySummarySync(today)
        val genderText = if (user.gender == 1) "Ayol" else "Erkak"
        val remainingCalories =
            (user.targetCalories - dailySummary.consumedCalories).coerceAtLeast(0)

        return """
            Profil: ${user.age} yosh, $genderText, ${user.weight} kg, ${user.height} sm. Maqsad: ${if (user.goal == "gain_weight") "Vazn yig'ish" else "Ozish"}.
            Bugungi holat: Kaloriya: ${dailySummary.consumedCalories}/${user.targetCalories} kkal (Qoldi: $remainingCalories kkal), Suv: ${dailySummary.consumedWaterMl}/${user.targetWaterMl} ml, Oqsil: ${dailySummary.consumedProtein}g, Yog': ${dailySummary.consumedFat}g, Uglevod: ${dailySummary.consumedCarbs}g.
            Bugun yeyilganlar:
            ${getGroupedFoodLogs()}
        """.trimIndent()
    }
}