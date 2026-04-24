package uz.oybek.ozamiz.data.health.local

import android.content.Context
import androidx.core.content.edit
import uz.oybek.ozamiz.appContext

object HealthTaskDatabase {

    private const val PREFS_NAME = "health_task_database"
    private val prefs = appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    /**
     * Increments the completion count for a specific task.
     * Returns the new count.
     */
    fun incrementTaskCount(taskId: String): Int {
        val currentCount = getTaskCount(taskId)
        val newCount = currentCount + 1
        prefs.edit { putInt("count_$taskId", newCount) }
        return newCount
    }

    /**
     * Returns the number of times a task has been completed in the past.
     */
    fun getTaskCount(taskId: String): Int {
        return prefs.getInt("count_$taskId", 0)
    }

    /**
     * Decrements the completion count (useful if user un-checks a task on the same day).
     */
    fun decrementTaskCount(taskId: String): Int {
        val currentCount = getTaskCount(taskId)
        if (currentCount <= 0) return 0
        val newCount = currentCount - 1
        prefs.edit { putInt("count_$taskId", newCount) }
        return newCount
    }

    /**
     * Returns a map of all task IDs tracking their completion count.
     */
    fun getAllTaskCounts(): Map<String, Int> {
        val map = mutableMapOf<String, Int>()
        prefs.all.forEach { (key, value) ->
            if (key.startsWith("count_") && value is Int) {
                val id = key.removePrefix("count_")
                map[id] = value
            }
        }
        return map
    }
}
