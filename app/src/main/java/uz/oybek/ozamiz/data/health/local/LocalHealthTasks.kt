package uz.oybek.ozamiz.data.health.local

import android.content.Context
import androidx.core.content.edit
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import uz.oybek.ozamiz.appContext
import uz.oybek.ozamiz.data.health.HealthTaskRepository
import uz.oybek.ozamiz.data.health.model.HealthTaskStatus
import uz.oybek.ozamiz.data.utils.DateUtils
import java.util.Date
import kotlin.random.Random

object LocalHealthTasks {

    private const val PREFS_NAME = "local_health_tasks"
    private const val KEY_START_DATE = "start_date"
    private const val KEY_TODAY_TASKS = "today_tasks"
    private const val KEY_TODAY_DATE = "today_date"
    private const val KEY_COMPLETED_IDS = "completed_ids"
    private const val KEY_LAST_ONBOARDING_DATE = "last_onboarding_date"

    private val gson = Gson()
    private val prefs = appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun getTodayTasks(): List<HealthTaskStatus> {
        val savedDate = prefs.getString(KEY_TODAY_DATE, "")
        val todayDate = DateUtils.getFormattedDateString(Date(), "yyyy-MM-dd")

        if (savedDate == todayDate) {
            val json = prefs.getString(KEY_TODAY_TASKS, null)
            if (json != null) {
                val type = object : TypeToken<List<String>>() {}.type
                val ids: List<String> = gson.fromJson(json, type)
                val completedIds = getCompletedIdsForToday()

                return ids.mapNotNull { id ->
                    HealthTaskRepository.getTaskById(id)?.let { task ->
                        HealthTaskStatus(
                            task = task,
                            isCompleted = completedIds.contains(id),
                            totalCompletions = HealthTaskDatabase.getTaskCount(id)
                        )
                    }
                }
            }
        }
        val uncompletedIds = if (!savedDate.isNullOrEmpty()) {
            val lastDayTasksJson = prefs.getString(KEY_TODAY_TASKS, null)
            if (lastDayTasksJson != null) {
                val type = object : TypeToken<List<String>>() {}.type
                val oldIds: List<String> = gson.fromJson(lastDayTasksJson, type) ?: emptyList()
                val oldCompletedIds = getCompletedIdsForDate(savedDate!!)
                oldIds.filter { !oldCompletedIds.contains(it) }
            } else emptyList()
        } else emptyList()

        // Generate tasks for today including carry-over
        return generateNewTasks(todayDate, uncompletedIds)
    }

    private fun generateNewTasks(
        todayDate: String,
        inheritedIds: List<String>
    ): List<HealthTaskStatus> {
        var startDate = prefs.getLong(KEY_START_DATE, 0L)
        if (startDate == 0L) {
            startDate = System.currentTimeMillis()
            prefs.edit { putLong(KEY_START_DATE, startDate) }
        }

        val daysSinceStart =
            ((System.currentTimeMillis() - startDate) / (1000 * 60 * 60 * 24)).toInt()
        val currentLevel = (daysSinceStart / 2) + 1

        val availableTasks = HealthTaskRepository.getTasksForLevel(currentLevel, Int.MAX_VALUE)

        // Exclude tasks already in inheritedIds
        val potentialNewTasks = availableTasks.filter { task -> !inheritedIds.contains(task.id) }

        // Sort by how many times they have been completed (least completed first)
        val sortedNewTasks = potentialNewTasks.sortedBy { HealthTaskDatabase.getTaskCount(it.id) }

        // Target number of new tasks to add
        val random = Random(todayDate.hashCode().toLong())
        val desiredTotal = random.nextInt(3, 5) // Aim for 3-4 tasks total
        val countToAdd = maxOf(0, desiredTotal - inheritedIds.size)

        val newlySelectedTasks = sortedNewTasks.take(countToAdd)
        val finalIds = (inheritedIds + newlySelectedTasks.map { it.id }).distinct()
            .take(5) // Absolute cap at 5

        prefs.edit {
            putString(KEY_TODAY_DATE, todayDate)
            putString(KEY_TODAY_TASKS, gson.toJson(finalIds))
        }
        
        return finalIds.mapNotNull { id ->
            HealthTaskRepository.getTaskById(id)?.let { HealthTaskStatus(it, false) }
        }
    }

    fun toggleTaskCompletion(taskId: String, isCompleted: Boolean) {
        val todayDate = DateUtils.getFormattedDateString(Date(), "yyyy-MM-dd")
        val completedIds = getCompletedIdsForToday().toMutableSet()

        if (isCompleted) {
            if (completedIds.add(taskId)) {
                HealthTaskDatabase.incrementTaskCount(taskId)
            }
        } else {
            if (completedIds.remove(taskId)) {
                HealthTaskDatabase.decrementTaskCount(taskId)
            }
        }

        prefs.edit {
            putString("${KEY_COMPLETED_IDS}_$todayDate", gson.toJson(completedIds))
        }
    }

    private fun getCompletedIdsForToday(): Set<String> {
        val todayDate = DateUtils.getFormattedDateString(Date(), "yyyy-MM-dd")
        return getCompletedIdsForDate(todayDate)
    }

    private fun getCompletedIdsForDate(date: String): Set<String> {
        val json = prefs.getString("${KEY_COMPLETED_IDS}_$date", null) ?: return emptySet()
        val type = object : TypeToken<Set<String>>() {}.type
        return gson.fromJson(json, type)
    }

    fun getCompletionSummary(): String {
        val tasks = getTodayTasks()
        if (tasks.isEmpty()) return "Vazifalar yo'q"
        val completed = tasks.count { it.isCompleted }
        val total = tasks.size
        return if (completed == total) {
            "Barcha vazifalar bajarildi! 🎉"
        } else {
            "$total tadan $completed tasi bajarildi"
        }
    }

    fun shouldShowDailyIntro(): Boolean {
        val todayDate = DateUtils.getFormattedDateString(Date(), "yyyy-MM-dd")
        val lastDate = prefs.getString(KEY_LAST_ONBOARDING_DATE, "")
        return (lastDate != todayDate)
    }

    fun markDailyIntroAsDone() {
        val todayDate = DateUtils.getFormattedDateString(Date(), "yyyy-MM-dd")
        prefs.edit { putString(KEY_LAST_ONBOARDING_DATE, todayDate) }
    }

    fun resetDailyIntro() {
        prefs.edit { putString(KEY_LAST_ONBOARDING_DATE, "") }
    }

}
