package uz.oybek.ozamiz.data.user.model

import androidx.annotation.Keep
import kotlinx.serialization.Transient
import uz.oybek.ozamiz.data.profile.model.UserActivityLevel
import uz.oybek.ozamiz.data.streak.StreakInfo
import uz.oybek.ozamiz.data.utils.DateUtils

@Keep
data class User(
    var uid: String = "",
    var name: String = "",
    var phoneNumber: String = "",
    var gmail: String = "",
    var lastSeenTime: Long? = null,
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
    var goal: String = uz.oybek.ozamiz.data.profile.model.UserGoal.LOSE_WEIGHT.key,
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
    var registerDate: Long = System.currentTimeMillis(),
    var paymentChecking: Boolean = false,
    var paymentCheckPhotoUrl: String? = null,
    var admin: Boolean = false,
    var scorePoints: Int = 0,
    var isRated: Boolean = false,
    val hello: String = "",
    var userInfoAdded: Boolean = false
) {
    val userGoal: uz.oybek.ozamiz.data.profile.model.UserGoal
        get() = uz.oybek.ozamiz.data.profile.model.UserGoal.fromKey(goal)

    val userGender: uz.oybek.ozamiz.data.profile.model.UserGender
        get() = uz.oybek.ozamiz.data.profile.model.UserGender.fromId(gender)

    val isGainWeight: Boolean
        get() = userGoal.isGain

    constructor() : this(
        uid = "",
        name = "",
        phoneNumber = "",
        gmail = "",
        lastSeenTime = System.currentTimeMillis(),
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
        goal = uz.oybek.ozamiz.data.profile.model.UserGoal.LOSE_WEIGHT.key,
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
        registerDate = System.currentTimeMillis(),
        paymentChecking = false,
        paymentCheckPhotoUrl = null,
        admin = false,
        scorePoints = 0,
        isRated = false
    )

    @Transient
    val valid: Boolean get() = uid.isNotEmpty()

    @get:com.google.firebase.database.Exclude
    val registerDateLong: Long?
        get() = registerDate?.let { DateUtils.toLongTimestamp(it) }

}

fun User.admin() = this.admin

fun User.daysFromRegister(): Int {
    val registerDate = registerDateLong ?: return 0
    if (registerDate <= 0L) return 0
    return DateUtils.daysBetween(registerDate, System.currentTimeMillis()).coerceAtLeast(0)
}