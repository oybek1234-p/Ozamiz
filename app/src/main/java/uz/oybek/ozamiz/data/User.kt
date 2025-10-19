// C:\Users\User\AndroidStudioProjects\Ozamiz\app\src\main\java\uz\oybek\ozamiz\data\User.kt
package uz.oybek.ozamiz.data

import androidx.annotation.Keep
import com.google.firebase.database.PropertyName
import kotlinx.serialization.Transient
import uz.oybek.ozamiz.data.info.ActivityLevel
import uz.oybek.ozamiz.data.info.MealsType
import uz.oybek.ozamiz.data.model.StreakInfo
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

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
    var manzil: String = " ",
    var age: Int = -1,
    var height: Int = -1, // sm
    var weight: Double = -1.0, // kg
    var targetWeight: Double? = null,
    var startWeight: Double? = null,
    var activityLevel: String = ActivityLevel.LIGHTLY_ACTIVE.id,
    var targetCalories: Int = 0,
    var targetWaterMl: Int = 2500,
    var targetCaloriesBurned: Int = 300,
    var problems: List<String>? = null,
    var reminderSettings: Map<String, String?>? = null,
    var streakInfo: StreakInfo? = null,
    var lastWeightLogTimestamp: Long = 0L,
    var avatarUrl: String? = null,
    var hasSeenWeightPhotoInfo: Boolean = false,
    var registerDate: Date? = null,
    var paymentChecking: Boolean = false,
    var paymentCheckPhotoUrl: String? = null,
    var admin: Boolean = false,
    var experiencePoints: Int = 0,
    var lessonLives: Int = 5,
    var lastLifeRefillTimestamp: Long = 0L,
    var completedLessonIds: List<String> = emptyList(), // Tugatilgan darslar IDlari
    var unlockedLessonIds: List<String> = emptyList(), // Ochilgan (lekin hali tugatilmagan) darslar IDlari
    var scorePoints: Int = 0,
    var unlockedBadgeIds: List<String> = emptyList() // Ochilgan nishonlarning IDlari
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
        manzil = "",
        age = -1,
        height = -1,
        weight = -1.0,
        targetWeight = null,
        startWeight = null,
        activityLevel = ActivityLevel.SEDENTARY.id,
        targetCalories = 0,
        targetWaterMl = 2500,
        targetCaloriesBurned = 300,
        problems = null,
        reminderSettings = null,
        streakInfo = StreakInfo(),
        lastWeightLogTimestamp = 0L,
        avatarUrl = null,
        hasSeenWeightPhotoInfo = false,
        registerDate = Calendar.getInstance().time,
        paymentChecking = false,
        paymentCheckPhotoUrl = null,
        admin = false,
        experiencePoints = 0,
        lessonLives = 5,
        lastLifeRefillTimestamp = 0L,
        completedLessonIds = emptyList(),
        unlockedLessonIds = emptyList(),
        scorePoints = 0, // Default ball 0
        unlockedBadgeIds = emptyList() // Boshida nishonlar yo'q
    )

    @Transient
    val valid: Boolean get() = uid.isNotEmpty()

}

fun User.upcomingMeal(): String {
    val now = Calendar.getInstance()
    val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())

    val defaultTimes = mapOf(
        "breakfast" to "08:00", "lunch" to "13:00", "dinner" to "19:00"
    )

    val upcoming = MealsType.mealTypes.mapNotNull { meal ->
        val timeStr = reminderSettings?.get(meal.id) ?: defaultTimes[meal.id]
        try {
            val parsedTime = sdf.parse(timeStr)
            if (parsedTime != null) {
                val mealTime = Calendar.getInstance().apply {
                    time = parsedTime
                    set(Calendar.YEAR, now.get(Calendar.YEAR))
                    set(Calendar.MONTH, now.get(Calendar.MONTH))
                    set(Calendar.DAY_OF_MONTH, now.get(Calendar.DAY_OF_MONTH))
                }

                if (mealTime.after(now)) {
                    meal to mealTime.timeInMillis
                } else null
            } else null
        } catch (e: Exception) {
            null
        }
    }.minByOrNull { it.second }

    return upcoming?.first?.id ?: MealsType.Breakfast.id
}

const val weightGoalModifier = -750

fun User.admin() = this.admin