package uz.oybek.ozamiz.data

import androidx.annotation.Keep
import uz.oybek.ozamiz.data.info.ActivityLevel

@Keep
data class User(
    var uid: String = "",
    var name: String = "",
    var phoneNumber: String = "",
    var gmail: String = "",
    var lastSeenTime: Long = 0L,
    var unreadMessages: Int = 0,
    var premium: Boolean = true,
    var premiumDate: Long = 0L,
    var pushToken: String = "",

    // Jismoniy ma'lumotlar
    var gender: Int = -1, // GenderEnum.value
    var age: Int = -1,
    var height: Int = -1, // sm
    var weight: Double = -1.0, // kg

    // Yangi qo'shilganlar
    var activityLevel: String = ActivityLevel.SEDENTARY.id, // ActivityLevel.id
    var weightGoal: String = WeightGoal.LOSE_WEIGHT_FAST.id, // <<<--- O'ZGARISH: Standart maqsad tez ozish
    var targetCalories: Int = 0, // Hisoblangan kunlik kaloriya maqsadi
    var targetWaterMl: Int = 2500, // Kunlik suv miqdori maqsadi (ml)
    var targetCaloriesBurned: Int = 300, // Kunlik harakat maqsadi (kkal)

    // Mavjudlari
    var problems: List<String>? = null,
    var reminderSettings: Map<String, Any?>? = null
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
        age = -1,
        height = -1,
        weight = -1.0,
        activityLevel = ActivityLevel.SEDENTARY.id,
        weightGoal = WeightGoal.LOSE_WEIGHT_FAST.id,
        targetCalories = 0,
        targetWaterMl = 2500,
        targetCaloriesBurned = 300,
        problems = null,
        reminderSettings = null
    )

    val valid: Boolean get() = uid.isNotEmpty()
}

fun User.admin() = true
enum class WeightGoal(val id: String, val description: String, val calorieModifier: Int) {
    LOSE_WEIGHT_FAST("lose_fast", "Tez ozish (-750 kkal)", -750),
    LOSE_WEIGHT("lose", "Ozish (-500 kkal)", -500),
    MAINTAIN_WEIGHT("maintain", "Vaznni saqlash (0 kkal)", 0),
    GAIN_WEIGHT("gain", "Vazn olish (+500 kkal)", 500);

    companion object {
        fun fromId(id: String?) = entries.find { it.id == id } ?: MAINTAIN_WEIGHT
        fun getAllDescriptions() = entries.map { it.description }
    }
}