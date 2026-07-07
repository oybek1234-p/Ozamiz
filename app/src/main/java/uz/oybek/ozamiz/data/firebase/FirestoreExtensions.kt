package uz.oybek.ozamiz.data.firebase

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.QuerySnapshot
import uz.oybek.ozamiz.data.user.mapper.UserMapper
import uz.oybek.ozamiz.data.user.model.User

val firestore get() = FirebaseFirestore.getInstance()
val database get() = FirebaseDatabase.getInstance()
val auth get() = FirebaseAuth.getInstance()
val crashlytics get() = FirebaseCrashlytics.getInstance()

val FirebaseFirestore.postsCollection: CollectionReference
    get() = FirebasePaths.Firestore.posts()

val FirebaseFirestore.publicUsersCollection: CollectionReference
    get() = FirebasePaths.Firestore.publicUsers()

val FirebaseFirestore.aiChatsCollection: CollectionReference
    get() = FirebasePaths.Firestore.aiChats()

val FirebaseFirestore.notificationsCollection: CollectionReference
    get() = FirebasePaths.Firestore.notifications()

val FirebaseFirestore.menusCollection: CollectionReference
    get() = FirebasePaths.Firestore.menus()

val FirebaseFirestore.menuCategoriesCollection: CollectionReference
    get() = FirebasePaths.Firestore.menuCategories()

val FirebaseFirestore.usersCollection: CollectionReference
    get() = FirebasePaths.Firestore.publicUsers()

val FirebaseDatabase.usersReference: DatabaseReference
    get() = FirebasePaths.Database.users()

fun FirebaseDatabase.userReference(userId: String): DatabaseReference {
    return FirebasePaths.Database.user(userId)
}

fun FirebaseFirestore.userWeightLogsCollection(
    userId: String
): CollectionReference {
    return FirebasePaths.Firestore.weightLogs(userId)
}

inline fun <reified T> QuerySnapshot.toObjectsList(): List<T> {
    return documents.mapNotNull { it.toObjectSafe<T>() }
}

inline fun <reified T> DocumentSnapshot.toObjectSafe(): T? {
    if (T::class.java == User::class.java) {
        return UserMapper.fromDocumentSnapshot(this) as? T
    }
    return try {
        toObject(T::class.java)
    } catch (_: Exception) {
        null
    }
}