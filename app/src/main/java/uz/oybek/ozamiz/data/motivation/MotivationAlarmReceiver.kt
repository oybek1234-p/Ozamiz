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
            MotivationAlarmScheduler.rescheduleToday(context)
            return
        }

        val type = intent?.getStringExtra("ALARM_TYPE") ?: return
        
        CoroutineScope(Dispatchers.IO).launch {
            val shouldShow = when (type) {
                "breakfast" -> checkMealNotLogged("Breakfast")
                "lunch" -> checkMealNotLogged("Lunch")
                "dinner" -> checkMealNotLogged("Dinner")
                "praise" -> checkAllMealsLogged()
                else -> true // Tips always show
            }

            if (shouldShow) {
                val message = MotivationRepository.getMessageForType(type)
                val notificationManager = MotivationNotificationManager(context)
                notificationManager.showMotivationNotification(message)
            }
        }
    }

    private suspend fun checkMealNotLogged(mealType: String): Boolean {
        val logs = FoodLogRepository.getAllLogs(Date()).first()
        return logs.none { it.mealType.equals(mealType, ignoreCase = true) }
    }

    private suspend fun checkAllMealsLogged(): Boolean {
        val logs = FoodLogRepository.getAllLogs(Date()).first()
        val types = logs.map { it.mealType.lowercase() }.toSet()
        return types.contains("breakfast") && types.contains("lunch") && types.contains("dinner")
    }
}
