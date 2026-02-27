package uz.oybek.ozamiz.data.motivation

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class MotivationAlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        // On boot completed, reschedule all alarms
        if (intent?.action == Intent.ACTION_BOOT_COMPLETED) {
            MotivationAlarmScheduler.scheduleAll(context)
            return
        }

        val message = MotivationRepository.getMessageForCurrentTime()

        if (message != null) {
            val notificationManager = MotivationNotificationManager(context)
            notificationManager.showMotivationNotification(message)
        }

        // Reschedule all alarms (exact alarms are one-shot, so must reschedule)
        MotivationAlarmScheduler.scheduleAll(context)
    }
}
