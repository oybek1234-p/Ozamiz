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

val FirebaseFirestore.notificationsCollection: CollectionReference
    get() = collection("notifications")

val FirebaseFirestore.menusCollection: CollectionReference
    get() = collection("menus")

val FirebaseFirestore.menuCategoriesCollection: CollectionReference
    get() = collection("menu_categories")

val FirebaseFirestore.usersCollection: CollectionReference
    get() = collection("users")

fun FirebaseFirestore.userWeightLogsCollection(userId: String): CollectionReference {
    return usersCollection.document(userId).collection("weightLogs")
}

fun FirebaseFirestore.userMenusCollection(userId: String): CollectionReference {
    return collection("user_menus").document(userId).collection("daily_menus")
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
