package uz.oybek.ozamiz.notification.reminders

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import uz.oybek.ozamiz.data.user.local.LocalUser

class BootCompletedReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            LocalUser.getUser()
            ReminderScheduler.scheduleAllActiveReminders(context)
        }
    }
}