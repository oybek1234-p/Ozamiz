package uz.oybek.ozamiz.data.points

object LevelSystem {
    
    data class Level(val number: Int, val name: String, val minPoints: Int, val maxPoints: Int)

    private val levels = listOf(
        Level(1, "Boshlovchi", 0, 100),
        Level(2, "Qiziquvchi", 100, 250),
        Level(3, "Harakatdagi", 250, 450),
        Level(4, "Izlanuvchi", 450, 700),
        Level(5, "Bilimli", 700, 1000),
        Level(6, "Tajribali", 1000, 1400),
        Level(7, "Intiluvchan", 1400, 1900),
        Level(8, "Qat'iyatli", 1900, 2500),
        Level(9, "Irodali", 2500, 3200),
        Level(10, "Kuchli", 3200, 4000),
        Level(11, "Professional", 4000, 5000),
        Level(12, "Ekspert", 5000, 6200),
        Level(13, "Master", 6200, 7500),
        Level(14, "Grandmaster", 7500, 9000),
        Level(15, "Chempion", 9000, 11000),
        Level(16, "Afsona", 11000, 13500),
        Level(17, "Titan", 13500, 16500),
        Level(18, "Olimpus", 16500, 20000),
        Level(19, "Superqahramon", 20000, 25000),
        Level(20, "Yengilmas", 25000, Int.MAX_VALUE)
    )

    fun getLevel(points: Int): Level {
        return levels.find { points in it.minPoints until it.maxPoints } ?: levels.last()
    }

    fun getNextLevel(points: Int): Level? {
        val current = getLevel(points)
        return levels.find { it.number == current.number + 1 }
    }
    
    fun getProgress(points: Int): Float {
        val current = getLevel(points)
        if (current.maxPoints == Int.MAX_VALUE) return 1.0f
        val range = current.maxPoints - current.minPoints
        val currentInLevel = points - current.minPoints
        return currentInLevel.toFloat() / range.toFloat()
    }
}
