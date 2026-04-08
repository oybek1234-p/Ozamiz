package uz.oybek.ozamiz.data.firebase

import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase

object FirebasePaths {

    private val root: DatabaseReference = FirebaseDatabase.getInstance().reference

    object Database {
        const val PATH_USERS = "users"
        const val PATH_LOGS = "logs"
        const val FOOD_LOGS = "foodLogs"
        const val WATER_LOGS = "waterLogs"
        const val EXERCISE_LOGS = "exerciseLogs"
        const val WEIGHT_LOGS = "weightLogs"
    }

    object Firestore {
        const val WEIGHT_PHOTOS = "weightPhoto"
        const val COMMUNITY_POSTS = "communityPosts"
    }

    object Storage {
        const val WEIGHT_PHOTOS = "weightLog"
        const val FOOD_PHOTOS = "loggedFood"
    }

    fun userReference(userId: String) =
        root.child(Database.PATH_USERS).child(userId)

    fun userLogsReference(userId: String) =
        root.child(Database.PATH_LOGS).child(userId)

    fun foodLogsReference(userId: String, date: String) =
        userLogsReference(userId)
            .child(date)
            .child(Database.FOOD_LOGS)

    fun exerciseLogsReference(userId: String, date: String) =
        userLogsReference(userId)
            .child(date)
            .child(Database.EXERCISE_LOGS)

    fun waterLogsReference(userId: String, date: String) =
        userLogsReference(userId)
            .child(date)
            .child(Database.WATER_LOGS)

    fun weightLogsReference(userId: String, date: String) =
        userLogsReference(userId).child(date).child(Database.WEIGHT_LOGS)

    fun userDailySummariesReference(userId: String) =
        root.child("daily_summaries").child(userId)

    fun dailySummaryReference(userId: String, date: String) =
        userDailySummariesReference(userId).child(date)

}