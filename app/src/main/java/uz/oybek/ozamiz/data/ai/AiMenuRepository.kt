package uz.oybek.ozamiz.data.ai

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import uz.oybek.ozamiz.data.utils.dateApiFormat
import java.util.Date

object AiMenuRepository {
    private const val PREFS_NAME = "ai_menu_prefs"
    private const val KEY_MENU_TEXT = "menu_text"
    private const val KEY_MENU_DATE = "menu_date"

    private val _currentMenu = MutableStateFlow<String?>(null)
    val currentMenu: StateFlow<String?> = _currentMenu.asStateFlow()

    private var initialized = false

    fun init(context: Context) {
        if (initialized) return
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val date = prefs.getString(KEY_MENU_DATE, "")
        val today = Date().dateApiFormat()

        if (date == today) {
            _currentMenu.value = prefs.getString(KEY_MENU_TEXT, null)
        } else {
            clearMenu(context)
        }
        initialized = true
    }

    suspend fun saveMenu(context: Context, text: String) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val today = Date().dateApiFormat()
        prefs.edit()
            .putString(KEY_MENU_TEXT, text)
            .putString(KEY_MENU_DATE, today)
            .apply()
        _currentMenu.value = text
    }

    suspend fun getLastMenus(limit: Int = 3): List<String> {
        return emptyList()
    }

    fun clearMenu(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().clear().apply()
        _currentMenu.value = null
    }
}
