package uz.oybek.ozamiz.data.info

import androidx.annotation.Keep

@Keep
data class DailyLogData(
    var foodLogs: Map<String, FoodLog>? = null,
    var waterLogs: Map<String, WaterLog>? = null, // logId -> WaterLog
    var exerciseLogs: Map<String, ExerciseLog>? = null // logId -> ExerciseLog
) {
     constructor() : this(null, null, null)
}