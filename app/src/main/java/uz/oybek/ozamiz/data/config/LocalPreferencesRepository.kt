package uz.oybek.ozamiz.data.config

import android.content.Context
import android.content.SharedPreferences
import uz.oybek.ozamiz.appContext

object LocalPreferencesRepository {

    private val appPrefs: SharedPreferences by lazy {
        appContext.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
    }

    private val dailyTipPrefs: SharedPreferences by lazy {
        appContext.getSharedPreferences("daily_tip_prefs", Context.MODE_PRIVATE)
    }

    private val achievementPrefs: SharedPreferences by lazy {
        appContext.getSharedPreferences("achievement_prefs", Context.MODE_PRIVATE)
    }

    private val aiChatPrefs: SharedPreferences by lazy {
        appContext.getSharedPreferences("ai_chat_cache", Context.MODE_PRIVATE)
    }

    // App Onboarding / Tours
    fun isFirstRegistration(): Boolean = appPrefs.getBoolean("is_first_registration", false)
    fun setFirstRegistration(value: Boolean) = appPrefs.edit().putBoolean("is_first_registration", value).apply()

    fun getAppOnboardingStep(): Int = appPrefs.getInt("app_onboarding_step", 0)
    fun setAppOnboardingStep(step: Int) = appPrefs.edit().putInt("app_onboarding_step", step).apply()

    fun isFoodTourSeen(): Boolean = appPrefs.getBoolean("food_tour_seen", false)
    fun setFoodTourSeen(value: Boolean) = appPrefs.edit().putBoolean("food_tour_seen", value).apply()

    fun isHomeTourSeen(): Boolean = appPrefs.getBoolean("home_tour_seen", false)
    fun setHomeTourSeen(value: Boolean) = appPrefs.edit().putBoolean("home_tour_seen", value).apply()

    // Daily Tips
    fun getLastDismissedDate(): String? = dailyTipPrefs.getString("last_dismissed_date", "")
    fun setLastDismissedDate(date: String) = dailyTipPrefs.edit().putString("last_dismissed_date", date).apply()

    fun getCurrentTipIndex(): Int = dailyTipPrefs.getInt("current_tip_index", -1)
    fun setCurrentTipIndex(index: Int) = dailyTipPrefs.edit().putInt("current_tip_index", index).apply()

    // Achievements
    fun getPersistentDateStr(key: String): String? = achievementPrefs.getString(key, null)
    fun savePersistentDateStr(key: String, date: String) = achievementPrefs.edit().putString(key, date).apply()

    // AI Chat Cache
    fun getAiChatData(dateStr: String): String? = aiChatPrefs.getString("chat_data_$dateStr", null)
    fun setAiChatData(dateStr: String, data: String) = aiChatPrefs.edit().putString("chat_data_$dateStr", data).apply()

    // AI Meal Cache
    fun getAiMealCache(key: String): String? = aiChatPrefs.getString(key, null)
    fun setAiMealCache(key: String, data: String) = aiChatPrefs.edit().putString(key, data).apply()

    fun getHasUnreadChat(): Boolean = aiChatPrefs.getBoolean("has_unread_chat", false)
    fun setHasUnreadChat(value: Boolean) = aiChatPrefs.edit().putBoolean("has_unread_chat", value).apply()
}
