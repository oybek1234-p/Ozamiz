package uz.oybek.ozamiz.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.media.RingtoneManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import uz.oybek.ozamiz.Application
import uz.oybek.ozamiz.ui.MainActivity
import uz.oybek.ozamiz.R
import uz.oybek.ozamiz.data.user.UserRepository
import uz.oybek.ozamiz.data.user.local.LocalUser
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

object AppMessagingService : FirebaseMessagingService() {

    private val TAG = "MyFirebaseMsgService"

    suspend fun loadToken() = suspendCoroutine { sus ->
        FirebaseMessaging.getInstance().token.addOnSuccessListener {
            sus.resume(it)
        }.addOnFailureListener {
            sus.resume(null)
        }
    }

    override fun onNewToken(token: String) {
        sendRegistrationToServer(token)
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        remoteMessage.data.isNotEmpty().let {
        }

        remoteMessage.notification?.let {
            sendNotification(it.title, it.body, remoteMessage.data)
        }
    }

    private fun sendRegistrationToServer(token: String?) {
        if (!token.isNullOrEmpty() && LocalUser.user.valid) {
            UserRepository.updatePushToken(token)
        } else {
            Log.w(TAG, "Cannot update push token: token is null/empty or user is not valid.")
        }
    }

    private fun sendNotification(
        messageTitle: String?,
        messageBody: String?,
        data: Map<String, String>
    ) {
        val intent = Intent(this, MainActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)

        val pendingIntentFlags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE
        } else {
            PendingIntent.FLAG_ONE_SHOT
        }
        val pendingIntent = PendingIntent.getActivity(
            this, 0 /* Request code */, intent,
            pendingIntentFlags
        )

        val channelId =
            Application.Companion.REMINDER_CHANNEL_ID // Sizning notification channel ID'ngiz
        val defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        val notificationBuilder = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.ozamiz_logo) // Ilova ikonchangiz
            .setContentTitle(messageTitle ?: getString(R.string.app_name))
            .setContentText(messageBody)
            .setAutoCancel(true)
            .setSound(defaultSoundUri)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_HIGH)

        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager

        val channel = NotificationChannel(
            channelId,
            "Umumiy Xabarnomalar",
            NotificationManager.IMPORTANCE_HIGH
        )
        notificationManager.createNotificationChannel(channel)

        val notificationId = System.currentTimeMillis().toInt()
        notificationManager.notify(notificationId, notificationBuilder.build())
    }
}