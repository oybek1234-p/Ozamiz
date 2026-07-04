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

    fun isSecondOnboardingSheetSeen(): Boolean = appPrefs.getBoolean("second_onboarding_sheet_seen", false)
    fun setSecondOnboardingSheetSeen(value: Boolean) = appPrefs.edit().putBoolean("second_onboarding_sheet_seen", value).apply()

    fun isFirstLogAdded(): Boolean = appPrefs.getBoolean("is_first_log_added", false)
    fun setFirstLogAdded(value: Boolean) = appPrefs.edit().putBoolean("is_first_log_added", value).apply()

    // App Review
    fun getFirstLaunchTime(): Long {
        val time = appPrefs.getLong("first_launch_time", 0L)
        if (time == 0L) {
            val now = System.currentTimeMillis()
            appPrefs.edit().putLong("first_launch_time", now).apply()
            return now
        }
        return time
    }

    fun isAppReviewShown(): Boolean = appPrefs.getBoolean("app_review_shown", false)
    fun setAppReviewShown(value: Boolean = true) = appPrefs.edit().putBoolean("app_review_shown", value).apply()

    // Daily Tips
    fun getLastDismissedDate(): String? = dailyTipPrefs.getString("last_dismissed_date", "")
    fun setLastDismissedDate(date: String) = dailyTipPrefs.edit().putString("last_dismissed_date", date).apply()

    fun getCurrentTipIndex(): Int = dailyTipPrefs.getInt("current_tip_index", -1)
    fun setCurrentTipIndex(index: Int) = dailyTipPrefs.edit().putInt("current_tip_index", index).apply()

    // Achievements
    fun getPersistentDateStr(key: String): String? = achievementPrefs.getString(key, null)
    fun savePersistentDateStr(key: String, date: String) = achievementPrefs.edit().putString(key, date).apply()

    fun getHasUnreadChat(): Boolean = aiChatPrefs.getBoolean("has_unread_chat", false)
    fun setHasUnreadChat(value: Boolean) = aiChatPrefs.edit().putBoolean("has_unread_chat", value).apply()

    fun getMealRecommendationKey(): String? = aiChatPrefs.getString("meal_rec_key", null)
    fun setMealRecommendationKey(key: String) = aiChatPrefs.edit().putString("meal_rec_key", key).apply()

    fun getMealRecommendationText(): String? = aiChatPrefs.getString("meal_rec_text", null)
    fun setMealRecommendationText(text: String?) = aiChatPrefs.edit().putString("meal_rec_text", text).apply()

    fun getDismissedMealRecommendation(): String? = appPrefs.getString("dismissed_meal_rec", null)
    fun setDismissedMealRecommendation(value: String?) = appPrefs.edit().putString("dismissed_meal_rec", value).apply()

    fun getLastAiEventTimestamp(): Long = aiChatPrefs.getLong("last_ai_event_ts", 0L)
    fun setLastAiEventTimestamp(ts: Long) = aiChatPrefs.edit().putLong("last_ai_event_ts", ts).apply()

    fun getCalorieExceededDate(): String? = aiChatPrefs.getString("calorie_exceeded_date", null)
    fun setCalorieExceededDate(date: String) = aiChatPrefs.edit().putString("calorie_exceeded_date", date).apply()

    private fun checkAndClearExpiredViolationGates(todayStr: String) {
        val savedDate = aiChatPrefs.getString("violation_gate_date", null)
        if (savedDate != todayStr) {
            val editor = aiChatPrefs.edit()
            editor.putString("violation_gate_date", todayStr)
            aiChatPrefs.all.keys.forEach { key ->
                if (key.startsWith("violation_gate_")) {
                    editor.remove(key)
                }
            }
            editor.apply()
        }
    }

    private fun checkAndClearExpiredManualAiUsage(todayStr: String) {
        val savedDate = appPrefs.getString("manual_ai_usage_date", null)
        if (savedDate != todayStr) {
            val editor = appPrefs.edit()
            editor.putString("manual_ai_usage_date", todayStr)
            appPrefs.all.keys.forEach { key ->
                if (key.startsWith("manual_ai_usage_")) {
                    editor.remove(key)
                }
            }
            editor.apply()
        }
    }

    fun isViolationGated(key: String): Boolean {
        val dateStr = key.substringBefore("_")
        if (dateStr.isNotBlank() && dateStr.contains("-")) {
            checkAndClearExpiredViolationGates(dateStr)
        }
        return aiChatPrefs.getBoolean("violation_gate_$key", false)
    }

    fun setViolationGated(key: String, value: Boolean = true) {
        val dateStr = key.substringBefore("_")
        if (dateStr.isNotBlank() && dateStr.contains("-")) {
            checkAndClearExpiredViolationGates(dateStr)
        }
        aiChatPrefs.edit().putBoolean("violation_gate_$key", value).apply()
    }

    fun isNewUserAdviceShown(): Boolean = aiChatPrefs.getBoolean("new_user_advice_shown", false)
    fun setNewUserAdviceShown(value: Boolean = true) = aiChatPrefs.edit().putBoolean("new_user_advice_shown", value).apply()

    fun getManualAiUsageCount(dateStr: String): Int {
        checkAndClearExpiredManualAiUsage(dateStr)
        return appPrefs.getInt("manual_ai_usage_$dateStr", 0)
    }

    fun incrementManualAiUsageCount(dateStr: String) {
        checkAndClearExpiredManualAiUsage(dateStr)
        val current = getManualAiUsageCount(dateStr)
        appPrefs.edit().putInt("manual_ai_usage_$dateStr", current + 1).apply()
    }

    fun saveTodayCache(
        dateStr: String,
        calories: Int,
        water: Int,
        exercise: Int,
        protein: Int,
        fat: Int,
        carbs: Int
    ) {
        appPrefs.edit()
            .putString("cached_summary_date", dateStr)
            .putInt("cached_calories", calories)
            .putInt("cached_water", water)
            .putInt("cached_exercise", exercise)
            .putInt("cached_protein", protein)
            .putInt("cached_fat", fat)
            .putInt("cached_carbs", carbs)
            .apply()
    }

    fun getTodayCache(dateStr: String): TodayCache? {
        val savedDate = appPrefs.getString("cached_summary_date", null)
        if (savedDate != dateStr) return null
        return TodayCache(
            calories = appPrefs.getInt("cached_calories", 0),
            water = appPrefs.getInt("cached_water", 0),
            exercise = appPrefs.getInt("cached_exercise", 0),
            protein = appPrefs.getInt("cached_protein", 0),
            fat = appPrefs.getInt("cached_fat", 0),
            carbs = appPrefs.getInt("cached_carbs", 0)
        )
    }

    fun getDailyAiMessageCount(dateStr: String): Int {
        return aiChatPrefs.getInt("daily_ai_messages_$dateStr", 0)
    }

    fun incrementDailyAiMessageCount(dateStr: String) {
        val current = getDailyAiMessageCount(dateStr)
        aiChatPrefs.edit().putInt("daily_ai_messages_$dateStr", current + 1).apply()
    }

    fun getAiLoggedDates(): Set<String> {
        return appPrefs.getStringSet("ai_logged_dates", null) ?: emptySet()
    }

    fun addAiLoggedDate(dateStr: String) {
        val current = getAiLoggedDates().toMutableSet()
        current.add(dateStr)
        appPrefs.edit().putStringSet("ai_logged_dates", current).apply()
    }

    fun getDailyAiLogCount(dateStr: String): Int {
        return appPrefs.getInt("daily_ai_logs_$dateStr", 0)
    }

    fun incrementDailyAiLogCount(dateStr: String) {
        val current = getDailyAiLogCount(dateStr)
        appPrefs.edit().putInt("daily_ai_logs_$dateStr", current + 1).apply()
    }

    data class TodayCache(
        val calories: Int,
        val water: Int,
        val exercise: Int,
        val protein: Int,
        val fat: Int,
        val carbs: Int
    )
}

