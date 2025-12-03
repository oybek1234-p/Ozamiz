package uz.oybek.ozamiz.data.user.model

import androidx.annotation.Keep
import kotlinx.serialization.Transient
import uz.oybek.ozamiz.data.streak.StreakInfo
import uz.oybek.ozamiz.data.user.model.info.UserActivityLevel
import java.util.Date

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
    var region: String = " ",
    var age: Int = -1,
    var height: Int = -1,
    var weight: Double = -1.0,
    var targetWeight: Double? = null,
    var startWeight: Double? = null,
    var activityLevel: String = UserActivityLevel.LIGHTLY_ACTIVE.name,
    var targetCalories: Int = 0,
    var targetWaterMl: Int = 2500,
    var problems: List<String>? = null,
    var reminderSettings: Map<String, String?>? = null,
    var streakInfo: StreakInfo? = null,
    var lastWeightLogTimestamp: Long = 0L,
    var avatarUrl: String? = null,
    var registerDate: Date? = null,
    var paymentChecking: Boolean = false,
    var paymentCheckPhotoUrl: String? = null,
    var admin: Boolean = false,
    var scorePoints: Int = 0,
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
        region = "",
        age = -1,
        height = -1,
        weight = -1.0,
        targetWeight = null,
        startWeight = null,
        activityLevel = UserActivityLevel.LIGHTLY_ACTIVE.name,
        targetCalories = 0,
        targetWaterMl = 2500,
        problems = null,
        reminderSettings = null,
        streakInfo = null,
        lastWeightLogTimestamp = 0L,
        avatarUrl = null,
        registerDate = null,
        paymentChecking = false,
        paymentCheckPhotoUrl = null,
        admin = false,
        scorePoints = 0
    )

    @Transient
    val valid: Boolean get() = uid.isNotEmpty()
}

fun User.admin() = this.admin