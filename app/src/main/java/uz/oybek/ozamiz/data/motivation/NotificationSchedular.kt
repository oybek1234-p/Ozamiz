package uz.oybek.ozamiz.data.motivation

import android.content.Context
import androidx.core.app.NotificationManagerCompat
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import java.util.Calendar
import java.util.concurrent.TimeUnit

object NotificationSchedular {

    enum class MotivationAlarmType(val id: Int, val hour: Int, val minute: Int, val typeKey: String) {
        BREAKFAST(101, 8, 0, "breakfast"),
        TIP(102, 10, 0, "tip"),
        LUNCH(103, 13, 0, "lunch"),
        DINNER(105, 19, 0, "dinner"),
        NIGHT_FASTING(106, 20, 45, "night_fasting"),
        INACTIVITY(107, 24, 0, "inactivity")
    }

    fun rescheduleToday(context: Context) {
        try {
            NotificationManagerCompat.from(context).cancelAll()
            WorkManager.getInstance(context).cancelUniqueWork("motivation_work_test")
        } catch (_: Exception) {}

        val calendar = Calendar.getInstance()

        for (type in MotivationAlarmType.entries) {
            val delayMillis = calculateDelayForType(type, calendar)
            enqueueWorkForType(context, type.typeKey, delayMillis)
        }
    }

    fun scheduleNextForType(context: Context, typeKey: String) {
        val type = MotivationAlarmType.entries.find { it.typeKey == typeKey } ?: return
        val calendar = Calendar.getInstance()
        val delayMillis = calculateDelayForType(type, calendar)
        enqueueWorkForType(context, typeKey, delayMillis)
    }

    private fun calculateDelayForType(type: MotivationAlarmType, now: Calendar): Long {
        return if (type == MotivationAlarmType.INACTIVITY) {
            TimeUnit.HOURS.toMillis(24)
        } else {
            val targetTime = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, type.hour)
                set(Calendar.MINUTE, type.minute)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
                if (before(now) || equals(now)) {
                    add(Calendar.DAY_OF_YEAR, 1)
                }
            }
            targetTime.timeInMillis - now.timeInMillis
        }
    }

    private fun enqueueWorkForType(context: Context, typeKey: String, delayMillis: Long) {
        val workRequest = OneTimeWorkRequestBuilder<MotivationWorker>()
            .setInitialDelay(delayMillis, TimeUnit.MILLISECONDS)
            .setInputData(workDataOf("ALARM_TYPE" to typeKey))
            .build()

        WorkManager.getInstance(context).enqueueUniqueWork(
            "motivation_work_$typeKey",
            ExistingWorkPolicy.REPLACE,
            workRequest
        )
    }
}
