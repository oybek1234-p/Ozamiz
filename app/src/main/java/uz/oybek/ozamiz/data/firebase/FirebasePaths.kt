package uz.oybek.ozamiz.data.firebase

import com.google.firebase.database.DatabaseReference


object FirebasePaths {

    private val root: DatabaseReference = database.reference

    object Database {
        const val PATH_USERS = "users"
        const val PATH_LOGS = "logs"
        const val FOOD_LOGS = "foodLogs"
        const val WATER_LOGS = "waterLogs"
        const val EXERCISE_LOGS = "exerciseLogs"
        const val WEIGHT_LOGS = "weightLogs"
        const val SAVED_FOODS = "savedFoods"
    }

    object Firestore {
        const val WEIGHT_PHOTOS = "weightPhoto"
        const val COMMUNITY_POSTS = "communityPosts"
        const val PUBLIC_USERS = "public_users"
        const val AI_CHATS = "aiChat"
        const val NOTIFICATIONS = "notifications"
        const val MENUS = "menus"
        const val MENU_CATEGORIES = "menu_categories"
        const val USERS = "users"
        const val USER_MENUS = "user_menus"
        const val DAILY_MENUS = "daily_menus"
        const val MESSAGES = "messages"
        const val AI_RECOMMENDATION_FEEDBACK = "ai_recommendation_feedback"
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

    fun savedFoodsReference(userId: String) =
        userReference(userId).child(Database.SAVED_FOODS)

    fun userDailySummariesReference(userId: String) =
        root.child("daily_summaries").child(userId)

    fun dailySummaryReference(userId: String, date: String) =
        userDailySummariesReference(userId).child(date)

}