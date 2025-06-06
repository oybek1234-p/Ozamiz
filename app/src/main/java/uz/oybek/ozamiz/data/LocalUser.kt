// C:\Users\User\AndroidStudioProjects\Ozamiz\app\src\main\java\uz\oybek\ozamiz\data\LocalUser.kt
package uz.oybek.ozamiz.data

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.lifecycle.MutableLiveData
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import uz.oybek.ozamiz.appContext
import uz.oybek.ozamiz.data.info.ActivityLevel
import uz.oybek.ozamiz.data.model.StreakInfo
import uz.oybek.ozamiz.reminders.ReminderScheduler
import uz.oybek.ozamiz.ui.postVal
import uz.oybek.ozamiz.ui.showToast
import java.util.Date

object LocalUser {

    var user = User()
        set(value) {
            val oldSettings = field.reminderSettings
            // Gamification maydonlarini ham tekshirish mumkin, agar UI darhol yangilanishi kerak bo'lsa
            val oldScorePoints = field.scorePoints
            val oldUnlockedBadgeIds = field.unlockedBadgeIds

            field = value
            userLive.postVal(value) // LiveData orqali UI ni xabardor qilish

            if (oldSettings != value.reminderSettings) {
                ReminderScheduler.scheduleAllActiveReminders(appContext)
            }

            // Agar ballar yoki nishonlar o'zgarsa, qo'shimcha logikani ishga tushirish mumkin
            if (oldScorePoints != value.scorePoints || oldUnlockedBadgeIds != value.unlockedBadgeIds) {
                // Masalan, profil ekranini yangilash uchun signal yuborish
            }
        }

    val userLive = MutableLiveData<User>()

    private val gson: Gson by lazy { Gson() }

    private fun preference(context: Context): SharedPreferences =
        context.getSharedPreferences("localUser", Context.MODE_PRIVATE)

    fun getUser() {
        val pref = preference(appContext)
        val loadedUser = User()
        with(pref) {
            loadedUser.apply {
                uid = getString("uid", "") ?: ""
                name = getString("name", "") ?: ""
                phoneNumber = getString("phone", "") ?: ""
                gmail = getString("gmail", "") ?: ""
                lastSeenTime = getLong("lastSeenTime", 0L)
                premium = getBoolean("premium", false)
                premiumDate = getLong("premiumDate", 0)
                pushToken = getString("pushToken", "") ?: ""
                gender = getInt("gender", -1)
                age = getInt("age", -1)
                height = getInt("height", -1)
                weight = getFloat("weight", -1f).toDouble()
                targetWeight = getFloat("targetWeight", -1f).takeIf { it != -1f }?.toDouble()
                startWeight = getFloat("startWeight", -1f).takeIf { it != -1f }?.toDouble()
                activityLevel = getString("activityLevel", ActivityLevel.SEDENTARY.id) ?: ActivityLevel.SEDENTARY.id
                targetCalories = getInt("targetCalories", 0)
                targetWaterMl = getInt("targetWaterMl", 2500)
                targetCaloriesBurned = getInt("targetCaloriesBurned", 300)
                manzil = getString("manzil", "").toString()
                problems = getStringSet("problems", null)?.toList()

                val settingsJson = getString("reminderSettingsJson", null)
                if (!settingsJson.isNullOrEmpty()) {
                    try {
                        val type = object : TypeToken<Map<String, String?>>() {}.type
                        reminderSettings = gson.fromJson(settingsJson, type)
                    } catch (e: Exception) {
                        Log.e("LocalUser", "Error parsing reminderSettings JSON", e)
                        reminderSettings = null
                    }
                } else {
                    reminderSettings = null
                }

                val streakInfoJson = getString("streakInfo", null)
                if (!streakInfoJson.isNullOrEmpty()) {
                    try {
                        streakInfo = gson.fromJson(streakInfoJson, StreakInfo::class.java)
                    } catch (e: Exception) {
                        Log.e("LocalUser", "Error parsing streakInfo JSON", e)
                        streakInfo = StreakInfo()
                    }
                } else {
                    streakInfo = StreakInfo()
                }
                unreadMessages = getInt("unreadMessages", 0)

                lastWeightLogTimestamp = getLong("lastWeightLogTimestamp", 0L)
                avatarUrl = getString("avatarUrl", null)
                hasSeenWeightPhotoInfo = getBoolean("hasSeenWeightPhotoInfo", false)
                val regDateMillis = getLong("registerDateMillis", 0L)
                registerDate = if (regDateMillis > 0) Date(regDateMillis) else null

                paymentChecking = getBoolean("paymentChecking", false)
                paymentCheckPhotoUrl = getString("paymentCheckPhotoUrl", null)
                isAdmin = getBoolean("isAdmin", false)

                // Darslar uchun
                experiencePoints = getInt("experiencePoints", 0)
                lessonLives = getInt("lessonLives", 5)
                lastLifeRefillTimestamp = getLong("lastLifeRefillTimestamp", 0L)

                // Gamification
                scorePoints = getInt("scorePoints", 0)
                val unlockedBadgesJson = getString("unlockedBadgeIdsJson", null)
                if (!unlockedBadgesJson.isNullOrEmpty()) {
                    try {
                        val type = object : TypeToken<List<String>>() {}.type
                        unlockedBadgeIds = gson.fromJson(unlockedBadgesJson, type) ?: emptyList()
                    } catch (e: Exception) {
                        Log.e("LocalUser", "Error parsing unlockedBadgeIds JSON", e)
                        unlockedBadgeIds = emptyList()
                    }
                } else {
                    unlockedBadgeIds = emptyList()
                }
            }
        }
        user = loadedUser
    }

    fun saveUser() {
        val prefEditor = preference(appContext).edit()
        with(prefEditor) {
            user.apply {
                putString("uid", uid)
                putString("name", name)
                putString("phone", phoneNumber)
                putString("gmail", gmail)
                putLong("lastSeenTime", lastSeenTime)
                putBoolean("premium", premium)
                putLong("premiumDate", premiumDate)
                putString("pushToken", pushToken)
                putInt("gender", gender)
                putInt("age", age)
                putInt("height", height)
                putFloat("weight", weight.toFloat())
                putFloat("targetWeight", targetWeight?.toFloat() ?: -1f)
                putFloat("startWeight", startWeight?.toFloat() ?: -1f)
                putString("activityLevel", activityLevel)
                putInt("targetCalories", targetCalories)
                putInt("targetWaterMl", targetWaterMl)
                putInt("targetCaloriesBurned", targetCaloriesBurned)
                putString("manzil", manzil)

                if (problems != null) {
                    putStringSet("problems", problems?.toSet())
                } else {
                    remove("problems")
                }

                if (reminderSettings != null) {
                    try {
                        val settingsJson = gson.toJson(reminderSettings)
                        putString("reminderSettingsJson", settingsJson)
                    } catch (e: Exception) {
                        Log.e("LocalUser", "Error saving reminderSettings JSON", e)
                        remove("reminderSettingsJson")
                    }
                } else {
                    remove("reminderSettingsJson")
                }

                if (streakInfo != null) {
                    try {
                        val streakJson = gson.toJson(streakInfo)
                        putString("streakInfo", streakJson)
                    } catch (e: Exception) {
                        Log.e("LocalUser", "Error saving streakInfo JSON", e)
                        remove("streakInfo")
                    }
                } else {
                    remove("streakInfo")
                }
                putInt("unreadMessages", unreadMessages)

                putLong("lastWeightLogTimestamp", lastWeightLogTimestamp)
                putString("avatarUrl", avatarUrl)
                putBoolean("hasSeenWeightPhotoInfo", hasSeenWeightPhotoInfo)
                putLong("registerDateMillis", registerDate?.time ?: 0L)

                putBoolean("paymentChecking", paymentChecking)
                putString("paymentCheckPhotoUrl", paymentCheckPhotoUrl)
                putBoolean("isAdmin", isAdmin)

                // Darslar uchun
                putInt("experiencePoints", experiencePoints)
                putInt("lessonLives", lessonLives)
                putLong("lastLifeRefillTimestamp", lastLifeRefillTimestamp)

                // Gamification
                putInt("scorePoints", scorePoints)
                try {
                    val badgesJson = gson.toJson(unlockedBadgeIds)
                    putString("unlockedBadgeIdsJson", badgesJson)
                } catch (e: Exception) {
                    Log.e("LocalUser", "Error saving unlockedBadgeIds JSON", e)
                    remove("unlockedBadgeIdsJson")
                }
            }
            apply()
        }
        userLive.postVal(user.copy()) // Ensure LiveData is updated with a new instance
    }

    fun clearUser() {
        val prefEditor = preference(appContext).edit()
        prefEditor.clear()
        prefEditor.apply()
        user = User() // Reset to default User object
    }
}