package uz.oybek.ozamiz.data.level

import kotlinx.coroutines.MainScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import uz.oybek.ozamiz.data.user.UserProfileRepository
import uz.oybek.ozamiz.data.user.model.User

object PointsRepository {

    val newLevelUnlockedFlow = MutableSharedFlow<Level>(
        replay = 1
    )

    fun onEvent(event: PointsEvent) {
        val earnedPoints = event.calculatePoints()
        val oldPoints = UserProfileRepository.user.scorePoints
        val newPoints = oldPoints + earnedPoints

        UserProfileRepository.updateScorePoints(newPoints)

        val oldLevel = LevelRepository.getCurrentLevel(oldPoints)
        val newLevel = LevelRepository.getCurrentLevel(newPoints)

        if (newLevel.requiredPoints > oldLevel.requiredPoints) {
            MainScope().launch {
                newLevelUnlockedFlow.emit(newLevel)
            }
        }
    }
}

sealed class PointsEvent {

    // Suv balini foizda hisoblash: 2500 ml (2.5L) -> 20 ball. 250 ml -> 2 ball.
    data class AddWater(val milliliters: Int) : PointsEvent() {
        override fun calculatePoints(): Int {
            val baseMl = 2500f
            val maxPoints = 20f
            return ((milliliters / baseMl) * maxPoints).toInt().coerceAtLeast(1)
        }
    }

    // Mashq balini hisoblash: Har 300 kaloriya uchun masalan 30 ball (yoki 20 ball). 20 kaloriya uchun ~2 ball.
    data class Exercise(val caloriesBurned: Int) : PointsEvent() {
        override fun calculatePoints(): Int {
            val baseCalories = 300f
            val basePoints = 30f // 300 kaloriya = 30 ball deb oldik
            return ((caloriesBurned / baseCalories) * basePoints).toInt().coerceAtLeast(1)
        }
    }

    // Ovqat balini hisoblash: Sifat darajasi 1 dan 10 gacha. 10 bo'lsa -> 20 ball, 1 bo'lsa -> 5 ball.
    // Har qanday holatda ham minimal 2 ball qo'shiladi.
    data class AddFood(val healthLevel: Int) : PointsEvent() {
        override fun calculatePoints(): Int {
            val level = healthLevel.coerceIn(1, 10)
            // 1 dan 10 gacha bo'lgan oraliqni 5 dan 20 ballgacha chiziqli hisoblash formulasi:
            return (5 + (level - 1) * (15f / 9f)).toInt()
        }
    }

    object StreakUp : PointsEvent() {
        override fun calculatePoints(): Int = 20
    }

    object WeightLog : PointsEvent() {
        override fun calculatePoints(): Int = 20
    }

    // Ball hisoblash funksiyasi prototipi
    abstract fun calculatePoints(): Int
}