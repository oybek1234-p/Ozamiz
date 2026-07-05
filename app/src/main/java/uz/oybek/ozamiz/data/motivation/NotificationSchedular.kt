package uz.oybek.ozamiz.data.motivation

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import java.util.Calendar

import androidx.core.app.NotificationManagerCompat

object NotificationSchedular {

    enum class MotivationAlarmType(val id: Int, val hour: Int, val typeKey: String) {
        BREAKFAST(101, 8, "breakfast"),
        TIP(102, 10, "tip"),
        LUNCH(103, 13, "lunch"),
        DINNER(105, 19, "dinner"),
        INACTIVITY(107, 24, "inactivity")
    }

    fun rescheduleToday(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        try {
            NotificationManagerCompat.from(context).cancelAll()
        } catch (_: Exception) {}

        for (type in MotivationAlarmType.entries) {
            val intent = Intent(context, MotivationAlarmReceiver::class.java)
            val pendingIntent = PendingIntent.getBroadcast(
                context, type.id, intent,
                PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
            )
            if (pendingIntent != null) {
                alarmManager.cancel(pendingIntent)
                pendingIntent.cancel()
            }
        }

        val calendar = Calendar.getInstance()

        for (type in MotivationAlarmType.entries) {
            val targetTime = Calendar.getInstance().apply {
                if (type == MotivationAlarmType.INACTIVITY) {
                    add(Calendar.DAY_OF_YEAR, 1)
                } else {
                    set(Calendar.HOUR_OF_DAY, type.hour)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                    if (before(calendar)) {
                        add(Calendar.DAY_OF_YEAR, 1)
                    }
                }
            }
            val intent = Intent(context, MotivationAlarmReceiver::class.java).apply {
                putExtra("ALARM_TYPE", type.typeKey)
            }
            val pendingIntent = PendingIntent.getBroadcast(
                context, type.id, intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            scheduleAlarm(
                alarmManager,
                targetTime.timeInMillis,
                pendingIntent
            )
        }
    }

    private fun scheduleAlarm(
        alarmManager: AlarmManager,
        triggerTime: Long,
        pendingIntent: PendingIntent
    ) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (alarmManager.canScheduleExactAlarms()) {
                    alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        triggerTime,
                        pendingIntent
                    )
                } else {
                    alarmManager.setAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        triggerTime,
                        pendingIntent
                    )
                }
            } else
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerTime,
                    pendingIntent
                )
        } catch (_: SecurityException) {
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)
        }
    }
}
