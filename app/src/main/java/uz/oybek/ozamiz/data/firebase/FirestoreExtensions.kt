package uz.oybek.ozamiz.data.firebase

import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.QuerySnapshot

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
    return collection(FirebasePaths.Firestore.USER_MENUS).document(userId).collection(FirebasePaths.Firestore.DAILY_MENUS)
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
    return try {
        toObject(T::class.java)
    } catch (e: Exception) {
        null
    }
}
