package uz.oybek.ozamiz.data.motivation

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import uz.oybek.ozamiz.data.logs.FoodLogRepository
import java.util.Date

class MotivationAlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action == Intent.ACTION_BOOT_COMPLETED) {
            NotificationSchedular.rescheduleToday(context)
            return
        }

        val type = intent?.getStringExtra("ALARM_TYPE") ?: return

        NotificationSchedular.rescheduleToday(context)

        val message = MotivationRepository.getMessageForType(type)
        val notificationManager = MotivationNotificationManager(context)
        notificationManager.showMotivationNotification(message)
    }
}
