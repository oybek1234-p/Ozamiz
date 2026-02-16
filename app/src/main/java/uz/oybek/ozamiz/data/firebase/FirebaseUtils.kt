package uz.oybek.ozamiz.data.firebase

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot

fun <T> DataSnapshot.getValueSafe(type: Class<T>, default: T? = null): T? {
    return try {
        getValue(type)
    } catch (_: Exception) {
        return default
    }
}

val currentUserId = FirebaseAuth.getInstance().currentUser?.uid

fun generateKey() = System.currentTimeMillis().toString()