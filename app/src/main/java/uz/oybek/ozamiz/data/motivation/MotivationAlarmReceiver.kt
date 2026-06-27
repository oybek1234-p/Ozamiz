package uz.oybek.ozamiz.data.motivation

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class MotivationAlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action == Intent.ACTION_BOOT_COMPLETED) {
            MotivationAlarmScheduler.scheduleAll(context)
            return
        }

        val message = MotivationRepository.getMessageForCurrentTime()

        if (message != null) {
            val notificationManager = MotivationNotificationManager(context)
            notificationManager.showMotivationNotification(message)
        }
        MotivationAlarmScheduler.scheduleAll(context, force = true)
    }
}
