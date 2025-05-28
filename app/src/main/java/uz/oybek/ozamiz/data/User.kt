package uz.oybek.ozamiz.data

import androidx.annotation.Keep
import kotlinx.serialization.Transient
import uz.oybek.ozamiz.data.info.ActivityLevel

@Keep
data class User(
    var uid: String = "",
    var name: String = "",
    var phoneNumber: String = "",
    var gmail: String = "",
    var lastSeenTime: Long = 0L,
    var unreadMessages: Int = 0,
    var premium: Boolean = false,
    var premiumDate: Long = 0L,
    var pushToken: String = "",
    var gender: Int = -1,
    var manzil: String =" ",
    var age: Int = -1,
    var height: Int = -1, // sm
    var weight: Double = -1.0, // kg
    var targetWeight: Double? = -1.0,
    var activityLevel: String = ActivityLevel.LIGHTLY_ACTIVE.id,
    var targetCalories: Int = 0,
    var targetWaterMl: Int = 2500,
    var targetCaloriesBurned: Int = 300,
    var problems: List<String>? = null,
    var reminderSettings: Map<String, String?>? = null
) {
    constructor() : this(
        uid = "",
        name = "",
        phoneNumber = "",
        gmail = "",
        lastSeenTime = 0L,
        unreadMessages = 0,
        premium = false,
        premiumDate = 0L,
        pushToken = "",
        gender = -1,
        "",
        age = -1,
        height = -1,
        weight = -1.0,
        targetWeight = null,
        activityLevel = ActivityLevel.SEDENTARY.id,
        targetCalories = 0,
        targetWaterMl = 2500,
        targetCaloriesBurned = 300,
        problems = null,
        reminderSettings = null
    )

    @Transient
    val valid: Boolean get() = uid.isNotEmpty()

}

val weightGoalModifier = -750

fun User.admin() = false