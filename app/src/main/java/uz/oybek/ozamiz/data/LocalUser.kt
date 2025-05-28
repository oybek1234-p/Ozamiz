package uz.oybek.ozamiz.data

import android.content.Context
import android.content.SharedPreferences
import android.util.Log // Log uchun import
import androidx.lifecycle.MutableLiveData
import com.google.gson.Gson // Gson importi
import com.google.gson.reflect.TypeToken // TypeToken importi
import uz.oybek.ozamiz.appContext
import uz.oybek.ozamiz.data.info.ActivityLevel
import uz.oybek.ozamiz.reminders.ReminderScheduler
import uz.oybek.ozamiz.ui.postVal

object LocalUser {

    var user = User()
        set(value) {
            val oldSettings = field.reminderSettings
            field = value
            userLive.postVal(value)
            // Check if reminderSettings changed to reschedule alarms
            if (oldSettings != value.reminderSettings) {
                ReminderScheduler.scheduleAllActiveReminders(appContext)
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
                premium = getBoolean("premium", false) // Default false
                premiumDate = getLong("premiumDate", 0)
                pushToken = getString("pushToken", "") ?: ""
                gender = getInt("gender", -1)
                age = getInt("age", -1)
                height = getInt("height", -1)
                weight = getFloat("weight", -1f).toDouble()
                targetWeight = getFloat("targetWeight", -1f).takeIf { it != -1f }?.toDouble()
                activityLevel = getString("activityLevel", ActivityLevel.SEDENTARY.id) ?: ActivityLevel.SEDENTARY.id
                targetCalories = getInt("targetCalories", 0)
                targetWaterMl = getInt("targetWaterMl", 2500)
                targetCaloriesBurned = getInt("targetCaloriesBurned", 300)

                problems = getStringSet("problems", null)?.toList()

                val settingsJson = getString("reminderSettingsJson", null)
                if (!settingsJson.isNullOrEmpty()) {
                    try {
                        val type = object : TypeToken<Map<String, String?>>() {}.type // Value is String? for "HH:MM" or null
                        reminderSettings = gson.fromJson(settingsJson, type)
                    } catch (e: Exception) {
                        Log.e("LocalUser", "Error parsing reminderSettings JSON", e)
                        reminderSettings = null
                    }
                } else {
                    reminderSettings = null
                }
                unreadMessages = getInt("unreadMessages", 0)
            }
        }
        user = loadedUser // This will trigger the setter and potentially reschedule alarms
    }

    fun saveUser() {
        val prefEditor = preference(appContext).edit()
        // val oldReminderSettings = user.reminderSettings // Get current settings before they are potentially changed by this save

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
                putString("activityLevel", activityLevel)
                putInt("targetCalories", targetCalories)
                putInt("targetWaterMl", targetWaterMl)
                putInt("targetCaloriesBurned", targetCaloriesBurned)

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
                putInt("unreadMessages", unreadMessages)
            }
            apply()
        }
        userLive.postVal(user.copy())
    }

    fun clearUser() {
        val prefEditor = preference(appContext).edit()
        prefEditor.clear()
        prefEditor.apply()
        user = User()
    }
}