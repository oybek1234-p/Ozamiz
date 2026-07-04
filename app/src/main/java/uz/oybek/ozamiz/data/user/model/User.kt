package uz.oybek.ozamiz.data.user.model

import androidx.annotation.Keep
import kotlinx.serialization.Transient
import uz.oybek.ozamiz.data.streak.StreakInfo
import uz.oybek.ozamiz.data.user.model.info.UserActivityLevel
import uz.oybek.ozamiz.data.utils.DateUtils

@Keep
data class User(
    var uid: String = "",
    var name: String = "",
    var phoneNumber: String = "",
    var gmail: String = "",
    var lastSeenTime: Any? = null,
    var unreadMessages: Int = 0,
    var premium: Boolean = false,
    var premiumDate: Long = 0L,
    var isPremiumYearly: Boolean = false,
    var isMonthlyPremium: Boolean = false,
    var gender: Int = -1,
    var region: String = " ",
    var age: Int = -1,
    var height: Int = -1,
    var weight: Double = -1.0,
    var targetWeight: Double? = null,
    var startWeight: Double? = null,
    var goal: String = "",
    var activityLevel: String = UserActivityLevel.LIGHTLY_ACTIVE.name,
    var targetCalories: Int = 0,
    var targetProtein: Int = 0,
    var targetFat: Int = 0,
    var targetCarbs: Int = 0,
    var targetWaterMl: Int = 2500,
    var problems: List<String>? = null,
    var streakInfo: StreakInfo? = null,
    var lastWeightLogTimestamp: Long = 0L,
    var avatarUrl: String? = null,
    var registerDate: Any? = null,
    var paymentChecking: Boolean = false,
    var paymentCheckPhotoUrl: String? = null,
    var admin: Boolean = false,
    var scorePoints: Int = 0,
    var lastTaskDate: String? = null,
    var completedTaskIds: List<String> = emptyList(),
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
        isPremiumYearly = false,
        isMonthlyPremium = false,
        gender = -1,
        region = "",
        age = -1,
        height = -1,
        weight = -1.0,
        targetWeight = null,
        startWeight = null,
        goal = "",
        activityLevel = UserActivityLevel.LIGHTLY_ACTIVE.name,
        targetCalories = 0,
        targetProtein = 0,
        targetFat = 0,
        targetCarbs = 0,
        targetWaterMl = 2500,
        problems = null,
        streakInfo = null,
        lastWeightLogTimestamp = 0L,
        avatarUrl = null,
        registerDate = null,
        paymentChecking = false,
        paymentCheckPhotoUrl = null,
        admin = false,
        scorePoints = 0,
        lastTaskDate = null,
        completedTaskIds = emptyList()
    )

    @Transient
    val valid: Boolean get() = uid.isNotEmpty()

    @get:com.google.firebase.database.Exclude
    val registerDateLong: Long?
        get() = registerDate?.let { DateUtils.toLongTimestamp(it) }
}

fun User.admin() = this.admin