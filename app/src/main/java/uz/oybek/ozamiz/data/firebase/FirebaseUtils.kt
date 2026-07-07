package uz.oybek.ozamiz.data.firebase

import com.google.firebase.database.DataSnapshot
import uz.oybek.ozamiz.data.user.local.user
import uz.oybek.ozamiz.data.user.mapper.UserMapper
import uz.oybek.ozamiz.data.user.model.User

fun <T> DataSnapshot.getValueSafe(type: Class<T>, default: T? = null): T? {
    if (type == User::class.java) {
        return UserMapper.fromSnapshot(this) as? T
    }
    return try {
        val value = getValue(type)
        value
    } catch (e: Exception) {
        default
    }
}

fun DataSnapshot.toUserSafe(): User = UserMapper.fromSnapshot(this)

val currentUserId: String
    get() = user.uid

fun generateKey() = System.currentTimeMillis().toString()