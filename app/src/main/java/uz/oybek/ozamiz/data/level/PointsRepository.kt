package uz.oybek.ozamiz.data.level

import kotlinx.coroutines.MainScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import uz.oybek.ozamiz.data.user.UserProfileRepository

object PointsRepository {

    val newLevelUnlockedFlow = MutableSharedFlow<Level>(
        replay = 1
    )

    fun onEvent(event: PointsEvent) {
        val user = UserProfileRepository.user
        if (user.valid) {
            val nextPoints = user.scorePoints + event.calculatePoints()
            val currentLevel = LevelRepository.getCurrentLevel(user.scorePoints)
            val nextLevel = LevelRepository.getCurrentLevel(nextPoints)

            if (nextLevel.requiredPoints > currentLevel.requiredPoints) {
                MainScope().launch {
                    newLevelUnlockedFlow.emit(nextLevel)
                }
            }

            UserProfileRepository.updateScorePoints(nextPoints)
        }
    }
}

@androidx.annotation.Keep
sealed class PointsEvent {

    data class AddWater(val milliliters: Int) : PointsEvent() {
        override fun calculatePoints(): Int {
            val baseMl = 2500f
            val maxPoints = 20f
            return ((milliliters / baseMl) * maxPoints).toInt().coerceAtLeast(1)
        }
    }

    data class Exercise(val caloriesBurned: Int) : PointsEvent() {
        override fun calculatePoints(): Int {
            val baseCalories = 300f
            val basePoints = 30f
            return ((caloriesBurned / baseCalories) * basePoints).toInt().coerceAtLeast(1)
        }
    }

    data class AddFood(val healthLevel: Int) : PointsEvent() {
        override fun calculatePoints(): Int {
            val level = healthLevel.coerceIn(1, 10)
            return (5 + (level - 1) * (15f / 9f)).toInt()
        }
    }

    abstract fun calculatePoints(): Int
}