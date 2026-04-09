package uz.oybek.ozamiz.data.level

object LevelManager {

    val levels = listOf(
        LevelInfo(
            name = "Yangi boshlovchi",
            emoji = "🌱",
            requiredPoints = 0,
            motivationText = "Siz sog'lom hayot yo'lidagi ilk qadamni tashladingiz. Davom eting!",
            color = 0xFF9E9E9E
        ),
        LevelInfo(
            name = "Harakatdagi",
            emoji = "🏃",
            requiredPoints = 100,
            motivationText = "Sizda intizom shakllana boshladi. To'xtab qolmang!",
            color = 0xFF4CAF50
        ),
        LevelInfo(
            name = "Intiluvchan",
            emoji = "✨",
            requiredPoints = 300,
            motivationText = "Natijalar ko'rina boshladi! O'zingizga bo'lgan ishonchni oshiring.",
            color = 0xFF2196F3
        ),
        LevelInfo(
            name = "Faol",
            emoji = "⚡",
            requiredPoints = 600,
            motivationText = "Siz allaqachon ko'pchilikdan oldindasiz. Energiyangiz tugamasin!",
            color = 0xFF9C27B0
        ),
        LevelInfo(
            name = "Tajribali",
            emoji = "🔥",
            requiredPoints = 1000,
            motivationText = "Sog'lom turmush tarzi sizning ajralmas qismingizga aylandi.",
            color = 0xFFFF9800
        ),
        LevelInfo(
            name = "Professional",
            emoji = "🎯",
            requiredPoints = 1500,
            motivationText = "Siz endi o'z tanangizning ustasisiz. Chegaralarni yengib o'ting!",
            color = 0xFFF44336
        ),
        LevelInfo(
            name = "Ekspert",
            emoji = "💎",
            requiredPoints = 2500,
            motivationText = "Sizning natijalaringiz boshqalarga o'rnak bo'la oladi.",
            color = 0xFFFFD700
        ),
        LevelInfo(
            name = "Chempion",
            emoji = "🏆",
            requiredPoints = 5000,
            motivationText = "Haqiqiy g'oliblik ruhi sizda mujassam. Siz bilan faxrlanamiz!",
            color = 0xFFB9F2FF
        ),
        LevelInfo(
            name = "Ozamiz Qiroli",
            emoji = "👑",
            requiredPoints = 10000,
            motivationText = "Siz cho'qqiga chiqdingiz! Endi siz sog'lom hayot afsonasisiz.",
            color = 0xFF673AB7
        )
    )

    fun getCurrentLevel(points: Int): LevelInfo {
        return levels.last { points >= it.requiredPoints }
    }

    fun getNextLevel(points: Int): LevelInfo? {
        return levels.firstOrNull { it.requiredPoints > points }
    }

    fun getProgressToNextLevel(points: Int): Float {
        val currentLevel = getCurrentLevel(points)
        val nextLevel = getNextLevel(points) ?: return 1.0f

        val range = nextLevel.requiredPoints - currentLevel.requiredPoints
        val earned = points - currentLevel.requiredPoints
        return earned.toFloat() / range.toFloat()
    }

    fun getCurrentLevelIndex(points: Int): Int {
        return levels.indexOf(getCurrentLevel(points))
    }
}
