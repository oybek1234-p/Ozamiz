package uz.oybek.ozamiz.data.level

object LevelRepository {

    val levels = listOf(
        Level(
            name = "Boshlovchi",
            emoji = "🌱",
            requiredPoints = 1,
            motivationText = "Birinchi qadamni tashladingiz, endi orqaga qaytish yo'q!"
        ),
        Level(
            name = "Faol",
            emoji = "💚",
            requiredPoints = 100,
            motivationText = "Ajoyib boshlanish! Tana yangi va sog‘lom rejimga kirishmoqda."
        ),
        Level(
            name = "Intizomli",
            emoji = "💪",
            requiredPoints = 300,
            motivationText = "Kaloriyalar tartibga tushmoqda. Intizom — muvaffaqiyat kalitidir!"
        ),
        Level(
            name = "Maqsadli",
            emoji = "🎯",
            requiredPoints = 600,
            motivationText = "Maqsad sari aniq zarba! Ko‘rsatkichlaringiz judayam ajoyib."
        ),
        Level(
            name = "Chiniqqan",
            emoji = "🏃",
            requiredPoints = 1000,
            motivationText = "Tanamiz chiniqib, vaqt o'tishi bilan microfilm yangi kuch to‘plamoqda!"
        ),
        Level(
            name = "Barqaror",
            emoji = "🌟",
            requiredPoints = 1500,
            motivationText = "Kunlik rejimingiz barqaror holatga keldi. Sizga qoyil!"
        ),
        Level(
            name = "Kuchli",
            emoji = "🦁",
            requiredPoints = 2200,
            motivationText = "Sizni yo‘ldan urib bo‘lmaydi. Ichki irodangiz judayam kuchli!"
        ),
        Level(
            name = "Ilg'or",
            emoji = "🚀",
            requiredPoints = 3000,
            motivationText = "Siz oddiy foydalanuvchi emassiz, siz eng ilg‘orlar qatoridasiz!"
        ),
        Level(
            name = "Professional",
            emoji = "💎",
            requiredPoints = 4000,
            motivationText = "Sog‘lom ovqatlanish endi siz uchun majburiyat emas, mahorat!"
        ),
        Level(
            name = "Tajribali",
            emoji = "🏅",
            requiredPoints = 5200,
            motivationText = "Siz endi bu yo'lda ancha pishib yetildingiz. Sodiqlik uchun rahmat!"
        ),
        Level(
            name = "Yetakchi",
            emoji = "👑",
            requiredPoints = 6500,
            motivationText = "Nafsni ham, dangasalikni ham bo‘ysundira olgan buyuk yetakchisiz!"
        ),
        Level(
            name = "Chempion",
            emoji = "🏆",
            requiredPoints = 8000,
            motivationText = "Zararli odatlar ustidan mutlaq g‘alaba! Haqiqiy chempionsiz!"
        ),
        Level(
            name = "Elita",
            emoji = "⭐",
            requiredPoints = 9500,
            motivationText = "Siz ilovamizning eng yuqori elita va eng nufuzli a'zosisiz."
        ),
        Level(
            name = "Ekspert",
            emoji = "🌠",
            requiredPoints = 11000,
            motivationText = "Kaloriyalar, oqsillar va kunlik balans bo‘yicha mukammal ekspertsiz!"
        ),
        Level(
            name = "Usta",
            emoji = "💫",
            requiredPoints = 12500,
            motivationText = "O‘z tanangiz va nafsingizni to'liq jilovladingiz. Sifat darajasi!"
        ),
        Level(
            name = "Afsona",
            emoji = "👑",
            requiredPoints = 13700,
            motivationText = "Cho'qqiga yaqinlashdingiz! Siz sog'lom hayot afsonasisiz."
        ),
        Level(
            name = "Buyuk Afsona",
            emoji = "🔱",
            requiredPoints = 14500,
            motivationText = "Sizning natijangiz va matonatingiz endi butun bir tarix va namuna!"
        ),
        Level(
            name = "Ozamiz Qahramoni",
            emoji = "👑",
            requiredPoints = 15000,
            motivationText = "Siz maqsadni zabt etdingiz! Siz sog‘lom hayot va Ozamiz Qahramonisiz!"
        )
    )

    private val emptyLevel = Level("", "", 0, "")

    fun getCurrentLevel(points: Int, getFirstIfEmpty: Boolean = false): Level {
        return levels.lastOrNull { points >= it.requiredPoints }
            ?: if (getFirstIfEmpty) levels.first() else emptyLevel
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
