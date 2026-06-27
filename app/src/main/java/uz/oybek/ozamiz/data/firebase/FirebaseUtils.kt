package uz.oybek.ozamiz.data.firebase

import com.google.firebase.database.DataSnapshot

import android.util.Log

fun <T> DataSnapshot.getValueSafe(type: Class<T>, default: T? = null): T? {
    return try {
        getValue(type)
    } catch (e: Exception) {
        Log.e("FirebaseUtils", "Error parsing ${type.simpleName}", e)
        default
    }
}

val currentUserId: String
    get() = auth.currentUser?.uid ?: ""

fun generateKey() = System.currentTimeMillis().toString()