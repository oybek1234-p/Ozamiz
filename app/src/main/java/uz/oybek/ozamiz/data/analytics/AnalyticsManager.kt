package uz.oybek.ozamiz.data.analytics

import android.os.Bundle
import android.util.Log
import com.google.firebase.BuildConfig
import com.google.firebase.Firebase
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.analytics


object AnalyticsManager {

    private const val TAG = "AnalyticsManager"

    private val analytics: FirebaseAnalytics? by lazy {
        try {
            Firebase.analytics
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize FirebaseAnalytics", e)
            null
        }
    }

    // --- Screen Tracking ---

    fun logScreenView(screenName: String, screenClass: String? = null) {
        val bundle = Bundle().apply {
            putString(FirebaseAnalytics.Param.SCREEN_NAME, screenName)
            if (screenClass != null) {
                putString(FirebaseAnalytics.Param.SCREEN_CLASS, screenClass)
            }
        }
        logEvent(FirebaseAnalytics.Event.SCREEN_VIEW, bundle)
    }

    fun logScreenTimeSpent(screenName: String, durationSeconds: Long) {
        if (durationSeconds <= 0) return
        val bundle = Bundle().apply {
            putString("screen_name", screenName)
            putLong("duration_seconds", durationSeconds)
        }
        logEvent("screen_time_spent", bundle)
    }

    // --- Feature & User Action Tracking ---

    fun logFeatureUsed(featureName: String, extraParams: Map<String, Any>? = null) {
        val bundle = Bundle().apply {
            putString("feature_name", featureName)
            extraParams?.forEach { (key, value) ->
                when (value) {
                    is String -> putString(key, value)
                    is Int -> putInt(key, value)
                    is Long -> putLong(key, value)
                    is Double -> putDouble(key, value)
                    is Boolean -> putBoolean(key, value)
                    else -> putString(key, value.toString())
                }
            }
        }
        logEvent("feature_used", bundle)
    }

    fun logButtonClick(screenName: String, buttonName: String) {
        val bundle = Bundle().apply {
            putString("screen_name", screenName)
            putString("button_name", buttonName)
        }
        logEvent("button_click", bundle)
    }

    // --- Custom Events ---

    fun logEvent(eventName: String, params: Bundle? = null) {
        try {
            analytics?.logEvent(eventName, params)
            if (BuildConfig.DEBUG) {
                val paramsStr = params?.keySet()?.joinToString(", ") { key ->
                    "$key=${params.get(key)}"
                } ?: "no params"
                Log.d(TAG, "Event logged: [$eventName] -> {$paramsStr}")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error logging event: $eventName", e)
        }
    }

    // --- User Properties ---

    fun setUserProperty(name: String, value: String?) {
        try {
            analytics?.setUserProperty(name, value)
            if (BuildConfig.DEBUG) {
                Log.d(TAG, "User property set: [$name] = $value")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error setting user property: $name", e)
        }
    }

    fun setUserId(userId: String?) {
        try {
            analytics?.setUserId(userId)
        } catch (e: Exception) {
            Log.e(TAG, "Error setting userId", e)
        }
    }
}
