package uz.oybek.ozamiz.data.points

import uz.oybek.ozamiz.data.points.model.Level

object LevelManager {

    val levels = listOf(
        Level(
            name = "Boshlovchi",
            emoji = "🌱",
            requiredPoints = 0,
            motivationText = "Birinchi qadamni tashladingiz, endi orqaga qaytish yo'q!"
        ),
        Level(
            name = "O'rganuvchi",
            emoji = "👀",
            requiredPoints = 100,
            motivationText = "Jarayonga kirdik. Harakatni to'xtatmang."
        ),
        Level(
            name = "Tirishqoq",
            emoji = "🏃",
            requiredPoints = 300,
            motivationText = "Sizdagi harakatga qoyil! Natijalar yaqin."
        ),
        Level(
            name = "Sabrli",
            emoji = "🧘",
            requiredPoints = 600,
            motivationText = "Eng qiyin paytlarda ham chidayapsiz. Barakalla."
        ),
        Level(
            name = "Irodali",
            emoji = "🛡️",
            requiredPoints = 1000,
            motivationText = "Dangasalikni yengdingiz. Irodangiz mustahkam."
        ),
        Level(
            name = "O'ziga ishongan",
            emoji = "😎",
            requiredPoints = 1500,
            motivationText = "Nima qilish kerakligini yaxshi bilasiz. Zo'r ketyapsiz!"
        ),
        Level(
            name = "Tajribali",
            emoji = "💪",
            requiredPoints = 2500,
            motivationText = "Siz endi bu yo'lda ancha pishib yetildingiz."
        ),
        Level(
            name = "Usta",
            emoji = "🏅",
            requiredPoints = 5000,
            motivationText = "O'z tanangiz va nafsingizni to'liq jilovladingiz."
        ),
        Level(
            name = "Afsona",
            emoji = "👑",
            requiredPoints = 10000,
            motivationText = "Cho'qqiga chiqdingiz! Siz sog'lom hayot afsonasisiz."
        )
    )

    fun getCurrentLevel(points: Int): Level {
        return levels.lastOrNull { points >= it.requiredPoints } ?: levels.first()
    }

    fun getCurrentLevelIndex(points: Int): Int {
        return levels.indexOfLast { points >= it.requiredPoints }.coerceAtLeast(0)
    }

    fun getNextLevel(points: Int): Level? {
        val currentIndex = getCurrentLevelIndex(points)
        return if (currentIndex < levels.size - 1) levels[currentIndex + 1] else null
    }

    fun getProgressToNextLevel(points: Int): Float {
        val current = getCurrentLevel(points)
        val next = getNextLevel(points) ?: return 1.0f

        val range = next.requiredPoints - current.requiredPoints
        val relativePoints = points - current.requiredPoints
        return (relativePoints.toFloat() / range.toFloat()).coerceIn(0f, 1f)
    }
}
