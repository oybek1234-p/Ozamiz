package uz.oybek.ozamiz.ui.feature.menu.list

enum class MenuCalorie(
    val id: String,
    val minKcal: Int,
    val maxKcal: Int,
    val emoji: String
) {
    CAL_50_100("50_100", 50, 100, "🍉"),
    CAL_100_200("100_200", 100, 200, "🥪"),
    CAL_200_300("200_300", 200, 300, "🍪"),
    CAL_300_400("300_400", 300, 400, "🥞"),
    CAL_400_500("400_500", 400, 500, "🥣"),
    CAL_500_600("500_600", 500, 600, "🧆");

    fun getText() = "$minKcal - $maxKcal kal"

    companion object {
        fun fromId(id: String): MenuCalorie? {
            return entries.find { it.id.equals(id, ignoreCase = true) }
        }
    }
}
