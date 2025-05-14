package uz.oybek.ozamiz.data.info

import androidx.annotation.Keep
import com.google.firebase.database.ServerValue

@Keep
data class ExerciseLog(
    var logId: String = "",
    var activityType: String = "", // Mashq turi (masalan, "yugurish", "raqs")
    var durationMin: Int = 0, // Davomiyligi (minut)
    var caloriesBurned: Int = 0, // Yoqilgan kaloriya (taxminiy)
    var timestamp: Any = ServerValue.TIMESTAMP
) {
    // Firebase RealtimeDB uchun kerakli no-argument konstruktor
     constructor() : this("", "", 0, 0, 0L)
}