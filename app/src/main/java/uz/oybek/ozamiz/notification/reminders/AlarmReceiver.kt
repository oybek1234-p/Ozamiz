package uz.oybek.ozamiz.notification.reminders

import android.Manifest
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Build
import android.os.PowerManager
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import uz.oybek.ozamiz.Application
import uz.oybek.ozamiz.ui.MainActivity
import uz.oybek.ozamiz.R

class AlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        var wakeLock: PowerManager.WakeLock? = null
        try {
            wakeLock = powerManager.newWakeLock(
                PowerManager.PARTIAL_WAKE_LOCK, "Ozamiz::ReminderWakeLock"
            )
            wakeLock.acquire(10 * 1000L)
        } catch (e: Exception) {
            //
        }
        val notificationManager = ContextCompat.getSystemService(
            context, NotificationManager::class.java
        ) as NotificationManager

        val reminderId = intent.getIntExtra(ReminderScheduler.EXTRA_REMINDER_ID_HASH, 0)
        val reminderTitle =
            intent.getStringExtra(ReminderScheduler.EXTRA_REMINDER_TITLE) ?: "Eslatma"
        val reminderMessage =
            intent.getStringExtra(ReminderScheduler.EXTRA_REMINDER_MESSAGE) ?: "Vaqt bo'ldi!"

        val activityIntent = Intent(context, MainActivity::class.java).apply {
        }
        val activityPendingIntent = PendingIntent.getActivity(
            context,
            reminderId,
            activityIntent,
            PendingIntent.FLAG_CANCEL_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, Application.REMINDER_CHANNEL_ID)
            .setSmallIcon(R.drawable.ozamiz_logo)
            .setContentTitle(reminderTitle).setContentText(reminderMessage)
            .setStyle(NotificationCompat.BigTextStyle().bigText(reminderMessage))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setContentIntent(activityPendingIntent).setAutoCancel(true)
            .setLights(Color.GREEN, 1000, 1000)
            .setVibrate(longArrayOf(0, 250, 250, 250))
            .build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ActivityCompat.checkSelfPermission(
                    context, Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                notificationManager.notify(reminderId, notification)
            } else {
                notificationManager.notify(reminderId, notification)
            }
        } else {
            notificationManager.notify(reminderId, notification)
        }

        val originalId = intent.getStringExtra(ReminderScheduler.EXTRA_REMINDER_ID_ORIGINAL)
        val hour = intent.getIntExtra(ReminderScheduler.EXTRA_REMINDER_HOUR, -1)
        val minute = intent.getIntExtra(ReminderScheduler.EXTRA_REMINDER_MINUTE, -1)

        if (originalId != null && hour != -1 && minute != -1) {
            ReminderScheduler.scheduleDailyAlarm(
                context, originalId, reminderTitle, reminderMessage, hour, minute
            )
        }

        wakeLock?.release()
    }
}