package uz.oybek.ozamiz.data.motivation

import uz.oybek.ozamiz.data.motivation.NutrientWarningReceiver

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import java.util.Calendar

object MotivationAlarmScheduler {

    private val ALARM_HOURS = intArrayOf(8, 10, 13, 16, 19)
    private val ALARM_MINUTES = intArrayOf(0, 0, 0, 0, 0)

    fun scheduleAll(context: Context, force: Boolean = false) {
        val prefs = context.getSharedPreferences("motivation_prefs", Context.MODE_PRIVATE)
        val lastScheduled = prefs.getLong("last_scheduled_time", 0)
        
        if (!force && System.currentTimeMillis() - lastScheduled < 12 * 60 * 60 * 1000L) {
            return
        }
        prefs.edit().putLong("last_scheduled_time", System.currentTimeMillis()).apply()

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        for (i in ALARM_HOURS.indices) {
            val calendar = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, ALARM_HOURS[i])
                set(Calendar.MINUTE, ALARM_MINUTES[i])
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }

            if (calendar.timeInMillis <= System.currentTimeMillis()) {
                calendar.add(Calendar.DAY_OF_YEAR, 1)
            }

            scheduleAlarm(context, alarmManager, calendar.timeInMillis, 100 + i)
        }
    }

    /**
     * TEST: 1 minutdan keyin notification keladi.
     */
    fun scheduleTestIn1Minute(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val triggerTime = System.currentTimeMillis() + 60 * 1000L
        scheduleAlarm(context, alarmManager, triggerTime, 999)
    }

    private fun scheduleAlarm(context: Context, alarmManager: AlarmManager, triggerTime: Long, requestCode: Int) {
        val intent = Intent(context, MotivationAlarmReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

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

        // 1 minute later for testing (was 1 hour)
        val triggerTime = System.currentTimeMillis() + 60 * 1000L
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
