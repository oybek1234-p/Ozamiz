package uz.oybek.ozamiz.data

import android.content.Context
import android.content.SharedPreferences
import android.util.Log // Log uchun import
import androidx.lifecycle.MutableLiveData
import com.google.gson.Gson // Gson importi
import com.google.gson.reflect.TypeToken // TypeToken importi
import uz.oybek.ozamiz.appContext
import uz.oybek.ozamiz.ui.postVal

object LocalUser {

    var user = User()
        set(value) {
            field = value
            userLive.postVal(value)
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
                weight = getInt("weight", -1)
                problems = getStringSet("problems", null)?.toList()

                val settingsJson = getString("reminderSettingsJson", null)
                if (!settingsJson.isNullOrEmpty()) {
                    try {
                        val type = object : TypeToken<Map<String, Any?>>() {}.type
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
                putInt("weight", weight)
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
                        remove("reminderSettingsJson")
                    }
                } else {

                    remove("reminderSettingsJson")
                }
                putInt("unreadMessages", unreadMessages)
            }
            apply()
        }
        userLive.postVal(user)
    }

    fun clearUser() {
        val prefEditor = preference(appContext).edit()
        prefEditor.clear() // Barcha ma'lumotlarni o'chirish
        prefEditor.apply()
        user = User() // user obyektini bo'sh holatga keltirish
    }
}