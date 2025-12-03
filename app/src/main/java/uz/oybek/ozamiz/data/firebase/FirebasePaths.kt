package uz.oybek.ozamiz.data.firebase

import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase

object FirebasePaths {

    private val root: DatabaseReference = FirebaseDatabase.getInstance().reference

    const val USERS = "users"
    const val WEIGHT_PHOTOS = "weightPhoto"
    const val LOGS = "logs"

    const val FOOD_LOGS = "foodLogs"
    const val WATER_LOGS = "waterLogs"
    const val EXERCISE_LOGS = "exerciseLogs"

    private fun DatabaseReference.childPath(vararg segments: String): DatabaseReference =
        segments.fold(this) { ref, segment ->
            ref.child(segment)
        }

    fun userRoot(userId: String) = root.childPath(USERS, userId)

    fun userLogsRoot(userId: String) = root.childPath(LOGS, userId)

    fun dailyLogsRoot(userId: String, dateKey: String) =
        userLogsRoot(userId).childPath(dateKey)

    fun foodLogsRef(userId: String, dateKey: String) =
        dailyLogsRoot(userId, dateKey).childPath(FOOD_LOGS)

    fun waterLogsRef(userId: String, dateKey: String) =
        dailyLogsRoot(userId, dateKey).childPath(WATER_LOGS)

    fun exerciseLogsRef(userId: String, dateKey: String) =
        dailyLogsRoot(userId, dateKey).childPath(EXERCISE_LOGS)

    fun weightPhotosRef(userId: String) =
        userRoot(userId).childPath(WEIGHT_PHOTOS)
}