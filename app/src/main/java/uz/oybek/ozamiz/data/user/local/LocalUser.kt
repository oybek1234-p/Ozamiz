package uz.oybek.ozamiz.data.user.local

import android.content.Context
import androidx.core.content.edit
import androidx.lifecycle.MutableLiveData
import com.google.gson.Gson
import uz.oybek.ozamiz.appContext
import uz.oybek.ozamiz.data.user.model.User
import uz.oybek.ozamiz.ui.utils.postVal

object LocalUser {

    private const val PREF_NAME = "local_user"
    private const val KEY_USER_DATA = "user_full_data"
    private val gson = Gson()
    val userLive = MutableLiveData<User>()
    var user = User()

    private fun preference(context: Context) =
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)

    fun getUser() {
        val json = preference(appContext)
            .getString(KEY_USER_DATA, null) ?: return
        user = try {
            gson.fromJson(json, User::class.java)
        } finally {
        }
        userLive.postVal(user)
    }

    fun saveUser() {
        val json = gson.toJson(user)
        preference(appContext).edit { putString(KEY_USER_DATA, json) }
        userLive.postVal(user)
    }

    fun update(block: User.() -> Unit) {
        user.block()
        saveUser()
    }
}

val user: User get() = LocalUser.user