package uz.oybek.ozamiz.data.firebase

import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.QuerySnapshot
import uz.oybek.ozamiz.data.streak.StreakInfo
import uz.oybek.ozamiz.data.user.model.User

// --- Firebase Instances ---
val firestore get() = com.google.firebase.firestore.FirebaseFirestore.getInstance()
val database get() = com.google.firebase.database.FirebaseDatabase.getInstance()
val auth get() = com.google.firebase.auth.FirebaseAuth.getInstance()
val crashlytics get() = com.google.firebase.crashlytics.FirebaseCrashlytics.getInstance()

// --- Collection Extensions ---

val FirebaseFirestore.postsCollection: CollectionReference
    get() = collection(FirebasePaths.Firestore.COMMUNITY_POSTS)

val FirebaseFirestore.publicUsersCollection: CollectionReference
    get() = collection(FirebasePaths.Firestore.PUBLIC_USERS)

val FirebaseFirestore.aiChatsCollection: CollectionReference
    get() = collection(FirebasePaths.Firestore.AI_CHATS)

val FirebaseFirestore.notificationsCollection: CollectionReference
    get() = collection(FirebasePaths.Firestore.NOTIFICATIONS)

val FirebaseFirestore.menusCollection: CollectionReference
    get() = collection(FirebasePaths.Firestore.MENUS)

val FirebaseFirestore.menuCategoriesCollection: CollectionReference
    get() = collection(FirebasePaths.Firestore.MENU_CATEGORIES)

val FirebaseFirestore.usersCollection: CollectionReference
    get() = collection(FirebasePaths.Firestore.USERS)

fun FirebaseFirestore.userWeightLogsCollection(userId: String): CollectionReference {
    return usersCollection.document(userId).collection(FirebasePaths.Database.WEIGHT_LOGS)
}

fun FirebaseFirestore.userMenusCollection(userId: String): CollectionReference {
    return collection(FirebasePaths.Firestore.USER_MENUS).document(userId)
        .collection(FirebasePaths.Firestore.DAILY_MENUS)
}

// --- Mapping Extensions ---

/**
 * Extension to safely map a QuerySnapshot to a List of objects.
 * Eliminates repetitive mapNotNull { it.toObject(T::class.java) } calls.
 */
inline fun <reified T> QuerySnapshot.toObjectsList(): List<T> {
    return documents.mapNotNull { it.toObject(T::class.java) }
}

/**
 * Extension to safely map a DocumentSnapshot to an object, returning null if it fails.
 */
inline fun <reified T> DocumentSnapshot.toObjectSafe(): T? {
    if (T::class.java == User::class.java) {
        return this.toUserSafe() as? T
    }
    return try {
        val obj = toObject(T::class.java)
        obj
    } catch (e: Exception) {
        null
    }
}

fun DocumentSnapshot.toUserSafe(): User {
    val u = User()
    u.uid = id
    u.name = getString("name") ?: ""
    u.phoneNumber = getString("phoneNumber") ?: ""
    u.gmail = getString("gmail") ?: ""
    u.lastSeenTime = getLong("lastSeenTime") ?: 0L
    u.unreadMessages =
        getLong("unreadMessages")?.toInt() ?: getDouble("unreadMessages")?.toInt() ?: 0
    u.premium = getBoolean("premium") ?: false
    u.premiumDate = getLong("premiumDate") ?: 0L
    u.isPremiumYearly = getBoolean("isPremiumYearly") ?: false
    u.isMonthlyPremium = getBoolean("isMonthlyPremium") ?: false
    u.gender = getLong("gender")?.toInt() ?: getDouble("gender")?.toInt() ?: -1
    u.region = getString("region") ?: " "
    u.age = getLong("age")?.toInt() ?: getDouble("age")?.toInt() ?: -1
    u.height = getLong("height")?.toInt() ?: getDouble("height")?.toInt() ?: -1
    u.weight = getDouble("weight") ?: getLong("weight")?.toDouble() ?: -1.0
    u.targetWeight = getDouble("targetWeight") ?: getLong("targetWeight")?.toDouble()
    u.startWeight = getDouble("startWeight") ?: getLong("startWeight")?.toDouble()
    u.goal = getString("goal") ?: ""
    u.activityLevel = getString("activityLevel") ?: "LIGHTLY_ACTIVE"
    u.targetCalories =
        getLong("targetCalories")?.toInt() ?: getDouble("targetCalories")?.toInt() ?: 0
    u.targetProtein = getLong("targetProtein")?.toInt() ?: getDouble("targetProtein")?.toInt() ?: 0
    u.targetFat = getLong("targetFat")?.toInt() ?: getDouble("targetFat")?.toInt() ?: 0
    u.targetCarbs = getLong("targetCarbs")?.toInt() ?: getDouble("targetCarbs")?.toInt() ?: 0
    u.targetWaterMl =
        getLong("targetWaterMl")?.toInt() ?: getDouble("targetWaterMl")?.toInt() ?: 2500
    u.avatarUrl = getString("avatarUrl")
    u.registerDate = get("registerDate")
    u.paymentChecking = getBoolean("paymentChecking") ?: false
    u.paymentCheckPhotoUrl = getString("paymentCheckPhotoUrl")
    u.admin = getBoolean("admin") ?: false
    u.scorePoints = getLong("scorePoints")?.toInt() ?: getDouble("scorePoints")?.toInt() ?: 0

    u.problems = try {
        (get("problems") as? List<*>)?.mapNotNull { it?.toString() }
    } catch (e: Exception) {
        null
    }
    u.streakInfo = try {
        val streakMap = get("streakInfo") as? Map<*, *>
        if (streakMap != null) {
            val lastActionDate = streakMap["lastActionDate"] as? String ?: ""
            val currentStreak = (streakMap["currentStreak"] as? Number)?.toInt() ?: 0
            val bestStreak = (streakMap["bestStreak"] as? Number)?.toInt() ?: 0
            val activeDays = (streakMap["activeDays"] as? Number)?.toInt() ?: 0
            val streakGoal = (streakMap["streakGoal"] as? Number)?.toInt() ?: 7
            val activityHistory =
                (streakMap["activityHistory"] as? List<*>)?.mapNotNull { it?.toString() }
                    ?: emptyList()
            StreakInfo(
                lastActionDate = lastActionDate,
                currentStreak = currentStreak,
                bestStreak = bestStreak,
                activeDays = activeDays,
                streakGoal = streakGoal,
                activityHistory = activityHistory
            )
        } else null
    } catch (e: Exception) {
        null
    }

    return u
}
