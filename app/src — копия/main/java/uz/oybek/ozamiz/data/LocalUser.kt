package uz.oybek.ozamiz.data

import android.content.Context
import android.content.SharedPreferences
import androidx.lifecycle.MutableLiveData
import uz.oybek.ozamiz.appContext
import uz.oybek.ozamiz.ui.postVal
import uz.oybek.ozamiz.ui.showToast

object LocalUser {

    var user = User()
    val userLive = MutableLiveData(user)

    private fun preference(context: Context): SharedPreferences =
        (context.getSharedPreferences("localUser", Context.MODE_PRIVATE))

    fun getUser() {
        val pref = preference(appContext)
        with(pref) {
            user.apply {
                uid = getString("uid", "") ?: ""
                name = getString("name", "") ?: ""
                phoneNumber = getString("phone", "") ?: ""
                lastSeenTime = getLong("lastSeenTime", 0L)
                 premium = getBoolean("premium", false)
                premiumDate = getLong("premiumDate", 0)
                pushToken = getString("pushToken", "") ?: ""
                gender = getInt("gender", -1)
                age = getInt("age", -1)
                height = getInt("height", -1)
                weight = getInt("weight", -1)
                problems = getStringSet("problems", setOf()) ?: setOf()
            }
        }
    }

    fun saveUser() {
        val pref = preference(appContext).edit()
        with(pref) {
            user.apply {
                putString("uid", uid)
                putString("name", name)
                putString("phone", phoneNumber)
                putLong("lastSeenTime", lastSeenTime)
                putBoolean("premium", premium)
                putLong("premiumDate", premiumDate)
                putString("pushToken", pushToken)
                putInt("gender", gender)
                putInt("age", age)
                putInt("height", height)
                putInt("weight", weight)
                putStringSet("problems", problems?.toSet())
                commit()
            }
        }
        userLive.postVal(user)
    }
}