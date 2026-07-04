package uz.oybek.ozamiz.data.motivation

import uz.oybek.ozamiz.data.motivation.NutrientWarningReceiver

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import java.util.Calendar

object MotivationAlarmScheduler {

    private val ALARM_HOURS = intArrayOf(8, 10, 13, 16, 19, 21)
    private val ALARM_TYPES = arrayOf("breakfast", "tip", "lunch", "tip", "dinner", "praise")

    fun rescheduleToday(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        
        for (i in 0..10) {
            val intent = Intent(context, MotivationAlarmReceiver::class.java)
            val pendingIntent = PendingIntent.getBroadcast(
                context, 100 + i, intent, 
                PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
            )
            pendingIntent?.let {
                alarmManager.cancel(it)
                it.cancel()
            }
        }

        val calendar = Calendar.getInstance()
        val currentHour = calendar.get(Calendar.HOUR_OF_DAY)

        // Only schedule for the remaining part of today
        for (i in ALARM_HOURS.indices) {
            val scheduledHour = ALARM_HOURS[i]
            if (scheduledHour <= currentHour) continue

            val targetTime = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, scheduledHour)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }

            val intent = Intent(context, MotivationAlarmReceiver::class.java).apply {
                putExtra("ALARM_TYPE", ALARM_TYPES[i])
            }
            val pendingIntent = PendingIntent.getBroadcast(
                context, 100 + i, intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            scheduleAlarm(context, alarmManager, targetTime.timeInMillis, pendingIntent)
        }
    }

    private fun scheduleAlarm(context: Context, alarmManager: AlarmManager, triggerTime: Long, pendingIntent: PendingIntent) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                // Android 12+ - check exact alarm permission
                if (alarmManager.canScheduleExactAlarms()) {
                    alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)
                } else {
                    alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)
                }
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)
            } else {
                alarmManager.set(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)
            }
        } catch (e: SecurityException) {
            // Fallback if exact alarm not permitted
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)
        }
    }

    fun scheduleNutrientWarning(context: Context, exceededTypes: List<String>) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, NutrientWarningReceiver::class.java).apply {
            putStringArrayListExtra("EXCEEDED_TYPES", ArrayList(exceededTypes))
        }

        // 1 hour later (was 1 minute for testing)
        val triggerTime = System.currentTimeMillis() + 60 * 60 * 1000L
        val requestCode = 200 // Different from motivation alarms

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (alarmManager.canScheduleExactAlarms()) {
                    alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)
                } else {
                    alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)
                }
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)
            } else {
                alarmManager.set(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)
            }
        } catch (e: SecurityException) {
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)
        }
    }
}
