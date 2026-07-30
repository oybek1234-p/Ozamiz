package uz.oybek.ozamiz.data.motivation

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters

class MotivationWorker(
    private val context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        val typeKey = inputData.getString("ALARM_TYPE") ?: return Result.failure()

        if (typeKey == "night_fasting") {
            val todaySummary = uz.oybek.ozamiz.data.logs.LogRepository.getDailySummarySync(java.util.Date())
            val user = uz.oybek.ozamiz.data.user.local.LocalUser.user
            val targetCals = if (user.targetCalories > 0) user.targetCalories else 2000
            val isGoodDay = todaySummary.consumedCalories in 1..(targetCals * 1.1).toInt() || todaySummary.consumedWaterMl >= 1000
            if (isGoodDay) {
                val message = "Kechki taomlanish tugadimi? Ajoyib! Endi oshqozonimiz dam oladi va tanangiz faol yog' yoqish rejimiga o'tadi. Xayrli tun! 🌙"
                val notificationManager = MotivationNotificationManager(context)
                notificationManager.showMotivationNotification(message)
            }
        } else {
            val message = MotivationRepository.getMessageForType(typeKey)
            val notificationManager = MotivationNotificationManager(context)
            notificationManager.showMotivationNotification(message)
        }

        NotificationSchedular.scheduleNextForType(context, typeKey)

        return Result.success()
    }
}
