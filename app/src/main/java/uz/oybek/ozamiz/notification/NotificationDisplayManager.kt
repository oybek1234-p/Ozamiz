package uz.oybek.ozamiz.notification

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.core.content.IntentCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import uz.oybek.ozamiz.R
import uz.oybek.ozamiz.data.notification.model.NotificationPayload
import uz.oybek.ozamiz.data.notification.model.NotificationType
import uz.oybek.ozamiz.ui.MainActivity
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NotificationDisplayManager @Inject constructor(
    @param:ApplicationContext private val context: Context
) {

    companion object {

        const val CHANNEL_ID = "ozamiz_notifications"
        const val EXTRA_PAYLOAD = "extra_notification_payload"

        fun getPayload(intent: Intent?): NotificationPayload? {
            if (intent == null) return null
            return IntentCompat.getParcelableExtra(
                intent, EXTRA_PAYLOAD, NotificationPayload::class.java
            )
        }
    }

    init {
        createNotificationChannel()
    }

    private fun generateNotificationId(): Int = (System.currentTimeMillis() % 100000).toInt()

    fun showNotification(
        title: String,
        body: String,
        type: NotificationType = NotificationType.GENERAL,
        targetId: String? = null,
        actionUrl: String? = null,
        notificationId: Int = generateNotificationId()
    ) {
        showNotification(
            payload = NotificationPayload(
                title = title, body = body, type = type, targetId = targetId, actionUrl = actionUrl
            ), notificationId = notificationId
        )
    }

    fun showNotification(
        payload: NotificationPayload, notificationId: Int = generateNotificationId()
    ) {
        val title = payload.title?.takeIf { it.isNotBlank() }
            ?: context.getString(R.string.notification_default_title)
        val body = payload.body.orEmpty()
        if (title.isBlank() && body.isBlank()) return

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_PAYLOAD, payload)
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            notificationId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        createNotificationChannel()

        val notification =
            NotificationCompat.Builder(context, CHANNEL_ID).setSmallIcon(R.drawable.new_logo)
                .setContentTitle(title).setContentText(body)
                .setStyle(NotificationCompat.BigTextStyle().bigText(body)).setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setDefaults(NotificationCompat.DEFAULT_ALL)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC).setContentIntent(pendingIntent)
                .build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    context, Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                return
            }
        }

        try {
            NotificationManagerCompat.from(context).notify(notificationId, notification)
        } catch (_: SecurityException) {
        }
    }

    private fun createNotificationChannel() {
        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val channel = NotificationChannel(
            CHANNEL_ID,
            context.getString(R.string.notification_default_title),
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            enableVibration(true)
        }

        notificationManager.createNotificationChannel(channel)
    }
}