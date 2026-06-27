package uz.oybek.ozamiz.data.motivation

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import uz.oybek.ozamiz.R

class MotivationNotificationManager(private val context: Context) {

    companion object {
        const val CHANNEL_ID = "motivation_channel_v2"
        const val CHANNEL_NAME = "Eslatmalar"
        const val NOTIFICATION_ID = 1001
    }

    fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val importance = NotificationManager.IMPORTANCE_HIGH // Changed to HIGH
            val channel = NotificationChannel(CHANNEL_ID, CHANNEL_NAME, importance).apply {
                description = "Channel for daily motivation messages"
                lockscreenVisibility = NotificationCompat.VISIBILITY_PUBLIC
            }
            val notificationManager: NotificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    fun showMotivationNotification(message: String) {
        // Only show if permission is granted (handled by NotificationManagerCompat mostly, but good to check)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (androidx.core.content.ContextCompat.checkSelfPermission(
                    context,
                    android.Manifest.permission.POST_NOTIFICATIONS
                ) != android.content.pm.PackageManager.PERMISSION_GRANTED
            ) {
                return
            }
        }

        val intent = context.packageManager.getLaunchIntentForPackage(context.packageName)
        val pendingIntent = android.app.PendingIntent.getActivity(
            context,
            0,
            intent,
            android.app.PendingIntent.FLAG_IMMUTABLE or android.app.PendingIntent.FLAG_UPDATE_CURRENT
        )

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.food_ic) // Ensure this resource exists
            .setContentTitle(context.getString(R.string.app_name))
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_HIGH) // Changed to HIGH
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)

        val notificationId = (System.currentTimeMillis() % 100000).toInt()
        with(NotificationManagerCompat.from(context)) {
            notify(notificationId, builder.build())
        }
    }
}
