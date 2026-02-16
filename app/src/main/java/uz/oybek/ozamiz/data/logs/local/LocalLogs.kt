package uz.oybek.ozamiz.data.logs.local

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import uz.oybek.ozamiz.appContext
import uz.oybek.ozamiz.data.logs.model.WeightLog

object LocalLogs {
    private const val PREF_NAME = "local_logs_pref"
    private const val KEY_WEIGHT_LOGS = "weight_logs"
    private val gson = Gson()

    private fun preference(context: Context) =
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)

    fun saveLogs(logs: List<WeightLog>) {
        val json = gson.toJson(logs)
        preference(appContext).edit().putString(KEY_WEIGHT_LOGS, json).apply()
    }

    fun getLogs(): List<WeightLog> {
        val json = preference(appContext).getString(KEY_WEIGHT_LOGS, null) ?: return emptyList()
        val type = object : TypeToken<List<WeightLog>>() {}.type
        return try {
            gson.fromJson(json, type) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }
}
