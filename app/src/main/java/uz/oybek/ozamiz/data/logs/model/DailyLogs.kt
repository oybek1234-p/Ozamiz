package uz.oybek.ozamiz.data.logs.model

import androidx.annotation.Keep

@Keep
data class DailyLogs(
    var foodLogs: Map<String, FoodLog>? = null,
    var waterLogs: Map<String, WaterLog>? = null,
    var exerciseLogs: Map<String, ExerciseLog>? = null,
    var weightLogs: Map<String, WeightLog>? = null
) {
    constructor() : this(
        null,
        null,
        null,
        null
    )
}