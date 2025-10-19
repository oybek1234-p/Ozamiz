package uz.oybek.ozamiz.reminders

import android.Manifest
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import androidx.core.app.ActivityCompat
import uz.oybek.ozamiz.data.LocalUser
import uz.oybek.ozamiz.data.info.MealsType
import uz.oybek.ozamiz.ui.showToast
import java.util.Calendar

object ReminderScheduler {

    const val EXTRA_REMINDER_ID_HASH = "reminder_id_hash"
    const val EXTRA_REMINDER_ID_ORIGINAL = "reminder_id_original"
    const val EXTRA_REMINDER_TITLE = "reminder_title"
    const val EXTRA_REMINDER_MESSAGE = "reminder_message"
    const val EXTRA_REMINDER_HOUR = "reminder_hour"
    const val EXTRA_REMINDER_MINUTE = "reminder_minute"

    fun scheduleAllActiveReminders(context: Context) {
        return
        val user = LocalUser.user

        if (!user.valid || user.reminderSettings == null) {
            cancelAllReminders(context, user.reminderSettings?.keys ?: emptySet())
            return
        }

        val currentSettings = user.reminderSettings!!
        val allPossibleReminderIds = getAllPossibleReminderIds(context)

        allPossibleReminderIds.forEach { reminderId ->
            val timeString = currentSettings[reminderId]
            if (timeString != null) {
                val parts = timeString.split(":")
                if (parts.size == 2) {
                    val hour = parts[0].toIntOrNull()
                    val minute = parts[1].toIntOrNull()
                    if (hour != null && minute != null) {
                        val title = getReminderTitle(context, reminderId)
                        val message = getReminderMessage(context, reminderId, hour, minute)
                        scheduleDailyAlarm(context, reminderId, title, message, hour, minute)

                        // Schedule water reminder if this is a meal
                        if (isMealReminder(reminderId)) {
                            scheduleWaterReminderForMeal(context, reminderId, hour, minute)
                        }
                    } else {
                        cancelReminder(context, reminderId.hashCode()) // Invalid time, cancel
                        cancelReminder(context, ("water_$reminderId").hashCode()) // Cancel associated water reminder
                    }
                } else {
                    cancelReminder(context, reminderId.hashCode())
                    cancelReminder(context, ("water_$reminderId").hashCode())
                }
            } else {
                // Reminder is disabled (not in settings or timeString is null)
                cancelReminder(context, reminderId.hashCode())
                cancelReminder(context, ("water_$reminderId").hashCode())
            }
        }
    }

    private fun getAllPossibleReminderIds(context: Context): Set<String> {
        val ids = mutableSetOf<String>()
        MealsType.mealTypes.forEach { ids.add(it.id) }
        // Add exercise IDs if they are managed similarly
        // For now, assuming user.reminderSettings contains all relevant exercise IDs too
        LocalUser.user.reminderSettings?.keys?.let { ids.addAll(it) }
        return ids
    }

    private fun scheduleWaterReminderForMeal(context: Context, mealId: String, mealHour: Int, mealMinute: Int) {
        return
        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, mealHour)
            set(Calendar.MINUTE, mealMinute)
            add(Calendar.MINUTE, -20) // 20 minutes before meal
        }
        val waterHour = calendar.get(Calendar.HOUR_OF_DAY)
        val waterMinute = calendar.get(Calendar.MINUTE)
        val waterReminderId = "water_$mealId"
        val mealName = getReminderTitle(context, mealId).lowercase()
        val waterTitle = "Suv ichishni unutmang"
        val waterMessage = "2 stakan suv iching"

        scheduleDailyAlarm(context, waterReminderId, waterTitle, waterMessage, waterHour, waterMinute)
    }

    
    internal fun scheduleDailyAlarm(
        context: Context,
        reminderIdOriginal: String,
        title: String,
        message: String,
        hour: Int,
        minute: Int
    ) {
        return
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (!alarmManager.canScheduleExactAlarms()) {
                Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).also {
                    context.startActivity(it)
                }
            }
        }

        val intent = Intent(context, AlarmReceiver::class.java).apply {
            putExtra(EXTRA_REMINDER_ID_HASH, reminderIdOriginal.hashCode())
            putExtra(EXTRA_REMINDER_ID_ORIGINAL, reminderIdOriginal)
            putExtra(EXTRA_REMINDER_TITLE, title)
            putExtra(EXTRA_REMINDER_MESSAGE, message)
            putExtra(EXTRA_REMINDER_HOUR, hour)
            putExtra(EXTRA_REMINDER_MINUTE, minute)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            reminderIdOriginal.hashCode(), // Unique request code for each reminder
            intent,
            PendingIntent.FLAG_CANCEL_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val calendar = Calendar.getInstance().apply {
            timeInMillis = System.currentTimeMillis()
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)

            // If the alarm time is in the past for today, schedule it for tomorrow
            if (before(Calendar.getInstance())) {
                add(Calendar.DATE, 1)
            }
        }

        try {
            if (ActivityCompat.checkSelfPermission(context, Manifest.permission.SCHEDULE_EXACT_ALARM) == PackageManager.PERMISSION_GRANTED || Build.VERSION.SDK_INT < Build.VERSION_CODES.S || alarmManager.canScheduleExactAlarms()) {
                 alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    calendar.timeInMillis,
                    pendingIntent
                )
            } else {
                 alarmManager.setAndAllowWhileIdle( // Fallback for no exact alarm permission
                    AlarmManager.RTC_WAKEUP,
                    calendar.timeInMillis,
                    pendingIntent
                )
            }
        } catch (se: SecurityException) {
            // Handle cases where exact alarm permission might still be an issue despite checks
            // Log or fallback to inexact alarm
             alarmManager.setAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                calendar.timeInMillis,
                pendingIntent
            )
        }
    }

    private fun cancelAllReminders(context: Context, reminderIds: Set<String>) {
        reminderIds.forEach { id ->
            cancelReminder(context, id.hashCode())
            cancelReminder(context, "water_$id".hashCode())
        }
    }

    private fun cancelReminder(context: Context, reminderIdHash: Int) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, AlarmReceiver::class.java) // The same intent used for setting
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            reminderIdHash,
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE // FLAG_NO_CREATE to check if it exists
        )
        if (pendingIntent != null) {
            alarmManager.cancel(pendingIntent)
            pendingIntent.cancel()
        }
    }

    private fun getReminderTitle(context: Context, reminderId: String): String {
        return when (reminderId) {
            MealsType.Breakfast.id -> "Ertalabki energiya vaqti"
            MealsType.Lunch.id -> "Tushlik vaqti – kuch to‘plash"
            MealsType.Dinner.id -> "Kechki ovqat – yengil tuting"
            MealsType.Snack.id -> "Yengil tamaddi vaqti"
            else -> uz.oybek.ozamiz.ui.getinfo.ExerciseType.getExerciseTypeById(reminderId).name
        }
    }

    private fun getReminderMessage(context: Context, reminderId: String, hour: Int, minute: Int): String {
        val time = String.format("%02d:%02d", hour, minute)
        return when (reminderId) {
            MealsType.Breakfast.id -> "Nonushta – kunni sog‘lom boshlang! Mevali yoki oqsilli ovqat tanlang."
            MealsType.Lunch.id -> "Tushlikda ortiqcha ovqatdan saqlaning. To‘yimli va foydali tanlov qiling."
            MealsType.Dinner.id -> "Kechki ovqat yengil bo‘lsin. Kechqurun tanani tinchlantiring."
            MealsType.Snack.id -> "$time da yengil tamaddi qiling. Masalan: meva yoki yong‘oq."
            else -> {
                val exerciseName = uz.oybek.ozamiz.ui.getinfo.ExerciseType.getExerciseTypeById(reminderId).name
                "$time da '$exerciseName' mashqi – sog‘lig‘ingiz uchun kichik qadam!"
            }
        }
    }


    private fun isMealReminder(reminderId: String): Boolean {
        return MealsType.mealTypes.any { it.id == reminderId }
    }
}