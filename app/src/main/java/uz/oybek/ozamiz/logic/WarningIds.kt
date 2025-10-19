package uz.oybek.ozamiz.logic

import androidx.annotation.DrawableRes
import uz.oybek.ozamiz.R
import uz.oybek.ozamiz.data.User
import uz.oybek.ozamiz.data.info.FoodLog

object WarningIds {
    const val HIGH_CARBS = "HIGH_CARBS"
    const val HIGH_FATS = "HIGH_FATS"
    const val SWEETS_CONSUMED = "SWEETS_CONSUMED"
    const val OSH_WARNING = "OSH_WARNING"
}

data class DietaryWarning(
    val id: String,
    @DrawableRes val iconRes: Int,
    val title: String,
    val suggestion: String? = null,
    val severity: Severity = Severity.WARNING
)

enum class Severity {
    INFO,
    WARNING,
    ALERT
}

object FoodAdviser {

    fun analyzeDailyIntake(user: User, foodLogs: List<FoodLog>): List<DietaryWarning> {
        val warnings = mutableListOf<DietaryWarning>()
        val targetCarbs = (user.targetCalories * 0.5) / 4
        val targetFats = (user.targetCalories * 0.3) / 9
        val totalCarbs = foodLogs.sumOf { it.carbs ?: 0.0 }
        val totalFats = foodLogs.sumOf { it.fat ?: 0.0 }
        val tagCounts = foodLogs.flatMap { it.tags ?: emptyList() }.groupingBy { it }.eachCount()

        if (totalCarbs > targetCarbs * 1.2) {
            warnings.add(DietaryWarning(
                id = WarningIds.HIGH_CARBS,
                iconRes = 0,
                title = "Non va unli ovqatlarni ko'p",
                suggestion = "Kuniga 1-2 bo'lak non xavfsiz, xamirli ovqatlarni ham kamaytiring",
                severity = Severity.WARNING
            ))
        }

        if (totalFats > targetFats * 1.2) {
             warnings.add(DietaryWarning(
                id = WarningIds.HIGH_FATS,
                iconRes = 0,
                title = "Yog‘li ovqatlar ko'p, semirtiradi",
                suggestion = "Ortiqcha yeyilsa — to‘g‘ridan-to‘g‘ri qorin va belda yog‘ bo‘lib yig‘iladi",
                severity = Severity.WARNING
            ))
        }

        if ((tagCounts["shirinlik"] ?: 0) > 0 || (tagCounts["yuqori_shakar"] ?: 0) > 0) {
            warnings.add(DietaryWarning(
                id = WarningIds.SWEETS_CONSUMED,
                iconRes = 0,
                title = "Bugun shirinlik ko‘p yedingiz \uD83C\uDF69",
                suggestion = "Boshqa shirinlik yemaganingiz ma'qul.",
                severity = Severity.ALERT
            ))
        }

        if ((tagCounts["osh"] ?: 0) > 0) {
            warnings.add(DietaryWarning(
                id = WarningIds.OSH_WARNING,
                iconRes = 0,
                title = "Osh juda mazali, lekin yog'li \uD83C\uDF5B",
                suggestion = "Tushlikda va haftada 1–2 martadan ko‘p yemang.",
                severity = Severity.INFO
            ))
        }
        
        return warnings.distinctBy { it.id }
    }
}