package uz.oybek.ozamiz.data

import androidx.annotation.Keep

@Keep
data class User(
    var uid: String,
    var name: String,
    var phoneNumber: String,
    var gmail: String,
    var lastSeenTime: Long,
    var unreadMessages: Int,
    var premium: Boolean,
    var premiumDate: Long,
    var pushToken: String,
    var gender: Int = -1,
    var age: Int = -1,
    var height: Int = -1,
    var weight: Int = -1,
    var problems: Set<String>? = null
) {
    constructor() : this("", "", "", "", 0, 0, false, 0, "")
}

val User?.valid: Boolean get() = this?.uid?.isEmpty()?.not() ?: false

data class DateHour(val hour: Int, val min: Int)