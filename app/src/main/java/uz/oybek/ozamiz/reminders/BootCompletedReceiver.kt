package uz.oybek.ozamiz.reminders

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import uz.oybek.ozamiz.data.LocalUser

class BootCompletedReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            // Re-initialize LocalUser if necessary or ensure it's loaded
            LocalUser.getUser() // Ensure user data is loaded
            ReminderScheduler.scheduleAllActiveReminders(context)
        }
    }
}