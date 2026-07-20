package uz.oybek.ozamiz.data.level

import kotlinx.coroutines.MainScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.launch
import uz.oybek.ozamiz.data.config.AppPreference
import uz.oybek.ozamiz.data.user.UserProfileRepository
import uz.oybek.ozamiz.data.user.model.User
import uz.oybek.ozamiz.data.logs.model.DailySummary
import uz.oybek.ozamiz.data.logs.model.FoodLog
import uz.oybek.ozamiz.data.logs.model.ExerciseLog
import uz.oybek.ozamiz.data.utils.DateUtils
import uz.oybek.ozamiz.data.utils.isToday
import java.util.Date

data class DailyAnalysisInfo(
    val foodCount: Int,
    val healthyFoodCount: Int,
    val points: Int,
    val waterMl: Int,
    val hasExercise: Boolean = false,
    val hasProteinGoal: Boolean = false,
    val noSweets: Boolean = false,
    val noBread: Boolean = false,
    val type: String = "DAILY",
    val caloriesBurned: Int = 0
)

object PointsRepository {

    val newLevelUnlockedFlow = MutableSharedFlow<Level>(
        replay = 1
    )

    private val _analysisEventFlow = MutableSharedFlow<DailyAnalysisInfo>(
        replay = 0,
        extraBufferCapacity = 64
    )
    val analysisEventFlow = _analysisEventFlow.asSharedFlow()

    private var prevFoodLogCount: Int? = null
    private var prevExerciseLogCount: Int? = null
    private var prevWaterConsumed: Int? = null
    private var loadingStartTime = 0L

    private var foodJustAdded = false
    private var waterJustAdded = false
    private var exerciseJustAdded = false

    fun resetState() {
        prevFoodLogCount = null
        prevExerciseLogCount = null
        prevWaterConsumed = null
        loadingStartTime = System.currentTimeMillis()
    }

    fun addWaterTrigger() {
        waterJustAdded = true
    }

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

    suspend fun checkActivitiesAndTrigger(
        date: Date,
        summary: DailySummary,
        foodLogs: List<FoodLog>,
        exerciseLogs: List<ExerciseLog>
    ) {
        val foodCount = foodLogs.size
        val exerciseCount = exerciseLogs.size
        val waterConsumed = summary.consumedWaterMl

        val prevFood = prevFoodLogCount
        val prevExercise = prevExerciseLogCount
        val prevWater = prevWaterConsumed

        val isSettling = System.currentTimeMillis() - loadingStartTime < 2000

        if (!isSettling) {
            if (prevFood != null && foodCount > prevFood) {
                foodJustAdded = true
            }
            if (prevExercise != null && exerciseCount > prevExercise) {
                exerciseJustAdded = true
            }
            if (prevWater != null && waterConsumed > prevWater) {
                waterJustAdded = true
            }
        }

        prevFoodLogCount = foodCount
        prevExerciseLogCount = exerciseCount
        prevWaterConsumed = waterConsumed

        checkFoodAnalysis(date, summary, foodLogs)
        checkWaterAnalysis(date, summary, foodLogs)
        checkExerciseAnalysis(date, summary, exerciseLogs)
    }

    private suspend fun checkFoodAnalysis(date: Date, summary: DailySummary, foodLogs: List<FoodLog>) {
        if (!date.isToday()) return
        val today = DateUtils.todayString()
        if (AppPreference.lastDailyAnalysisDate == today) return
        val user = UserProfileRepository.user
        val progress = if (user.targetCalories > 0) summary.consumedCalories.toFloat() / user.targetCalories else 0f
        if (foodJustAdded && progress >= 0.5f) {
            foodJustAdded = false
            AppPreference.lastDailyAnalysisDate = today
            val healthyCount = foodLogs.count { (it.healthLevel ?: 0) >= 8 }

            val sweetKeywords = listOf("shakar", "shirinlik", "konfet", "tort", "pechenye", "shokolad", "muzqaymoq")
            val breadKeywords = listOf("non", "xamir", "manti", "chuchvara", "somsa", "pitsa", "lavash", "burger")

            val hasSweets = foodLogs.any { log ->
                sweetKeywords.any { keyword -> log.description.contains(keyword, ignoreCase = true) }
            }
            val hasBread = foodLogs.any { log ->
                breadKeywords.any { keyword -> log.description.contains(keyword, ignoreCase = true) }
            }

            val hasExercise = summary.burnedCalories >= 100
            val hasProteinGoal = summary.consumedProtein >= (user.targetProtein * 0.8)

            onEvent(PointsEvent.DailyAnalysis)
            _analysisEventFlow.emit(
                DailyAnalysisInfo(
                    foodCount = foodLogs.size,
                    healthyFoodCount = healthyCount,
                    points = 50,
                    waterMl = summary.consumedWaterMl,
                    hasExercise = hasExercise,
                    hasProteinGoal = hasProteinGoal,
                    noSweets = !hasSweets,
                    noBread = !hasBread,
                    type = "DAILY"
                )
            )
        }
    }

    private suspend fun checkWaterAnalysis(date: Date, summary: DailySummary, foodLogs: List<FoodLog>) {
        if (!date.isToday()) return
        if (!waterJustAdded) return
        waterJustAdded = false
        val today = DateUtils.todayString()
        val user = UserProfileRepository.user
        val waterProgress = if (user.targetWaterMl > 0) summary.consumedWaterMl.toFloat() / user.targetWaterMl else 0f

        if (AppPreference.lastWaterBenefitsDate != today && summary.consumedWaterMl > 0) {
            AppPreference.lastWaterBenefitsDate = today
            onEvent(PointsEvent.Water60)
            _analysisEventFlow.emit(
                DailyAnalysisInfo(
                    foodCount = 0,
                    healthyFoodCount = 0,
                    points = 5,
                    waterMl = summary.consumedWaterMl,
                    hasExercise = false,
                    hasProteinGoal = false,
                    noSweets = false,
                    noBread = false,
                    type = "WATER_BENEFITS"
                )
            )
            return
        }

        if (AppPreference.lastWater60AnalysisDate != today && waterProgress >= 0.6f) {
            AppPreference.lastWater60AnalysisDate = today
            onEvent(PointsEvent.Water60)
            _analysisEventFlow.emit(
                DailyAnalysisInfo(
                    foodCount = 0,
                    healthyFoodCount = 0,
                    points = 5,
                    waterMl = summary.consumedWaterMl,
                    hasExercise = false,
                    hasProteinGoal = false,
                    noSweets = false,
                    noBread = false,
                    type = "WATER_60"
                )
            )
        }
    }

    private suspend fun checkExerciseAnalysis(date: Date, summary: DailySummary, exerciseLogs: List<ExerciseLog>) {
        if (!date.isToday()) return
        if (!exerciseJustAdded) return
        exerciseJustAdded = false
        val today = DateUtils.todayString()
        val exerciseTarget = 300
        val exerciseProgress = if (exerciseTarget > 0) summary.burnedCalories.toFloat() / exerciseTarget else 0f

        if (AppPreference.lastExerciseBenefitsDate != today && exerciseLogs.isNotEmpty()) {
            AppPreference.lastExerciseBenefitsDate = today
            onEvent(PointsEvent.Water60)
            _analysisEventFlow.emit(
                DailyAnalysisInfo(
                    foodCount = 0,
                    healthyFoodCount = 0,
                    points = 5,
                    waterMl = 0,
                    hasExercise = true,
                    hasProteinGoal = false,
                    noSweets = false,
                    noBread = false,
                    type = "EXERCISE_BENEFITS",
                    caloriesBurned = summary.burnedCalories
                )
            )
            return
        }

        if (AppPreference.lastExercise50AnalysisDate != today && exerciseProgress >= 0.5f) {
            AppPreference.lastExercise50AnalysisDate = today
            onEvent(PointsEvent.Water60)
            _analysisEventFlow.emit(
                DailyAnalysisInfo(
                    foodCount = 0,
                    healthyFoodCount = 0,
                    points = 5,
                    waterMl = 0,
                    hasExercise = true,
                    hasProteinGoal = false,
                    noSweets = false,
                    noBread = false,
                    type = "EXERCISE_50",
                    caloriesBurned = summary.burnedCalories
                )
            )
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

    object DailyAnalysis : PointsEvent() {
        override fun calculatePoints(): Int = 50
    }

    object Water60 : PointsEvent() {
        override fun calculatePoints(): Int = 5
    }

    abstract fun calculatePoints(): Int
}