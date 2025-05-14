package uz.oybek.ozamiz.data

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import uz.oybek.ozamiz.appContext // appContext importi
import uz.oybek.ozamiz.data.foods.FoodItem
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object SharedPrefsHelper {

    private const val PREFS_NAME = "daily_food_plan_prefs"
    private const val KEY_PLAN_DATE = "plan_date"
    private const val KEY_PLAN_DATA = "plan_data"

    private val gson = Gson()

    init {
        getPreferences().edit().clear().apply()
    }

    private fun getPreferences(): SharedPreferences {
        return appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    private fun getTodayDateString(): String {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        return dateFormat.format(Date())
    }

    fun saveDailyPlan(plan: Map<String, FoodItem>) {
        val editor = getPreferences().edit()
        val jsonPlan = gson.toJson(plan)
        editor.putString(KEY_PLAN_DATE, getTodayDateString())
        editor.putString(KEY_PLAN_DATA, jsonPlan)
        editor.apply()
    }

    fun loadDailyPlan(): Map<String, FoodItem>? {
        val prefs = getPreferences()
        val savedDate = prefs.getString(KEY_PLAN_DATE, null)
        val todayDate = getTodayDateString()

        if (savedDate == todayDate) {
            val jsonPlan = prefs.getString(KEY_PLAN_DATA, null)
            if (jsonPlan != null) {
                try {
                    val type = object : TypeToken<Map<String, FoodItem>>() {}.type
                    return gson.fromJson(jsonPlan, type)
                } catch (e: Exception) {
                    // Handle parsing error if needed
                    clearDailyPlan() // Clear corrupted data
                }
            }
        } else {
            // Eski sana, rejani tozalash
            clearDailyPlan()
        }
        return null
    }

    /**
     * Saqlangan kunlik rejani tozalaydi.
     */
    fun clearDailyPlan() {
        val editor = getPreferences().edit()
        editor.remove(KEY_PLAN_DATE)
        editor.remove(KEY_PLAN_DATA)
        editor.apply()
    }
}