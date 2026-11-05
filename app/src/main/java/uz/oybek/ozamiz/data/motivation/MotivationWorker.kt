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

        val message = MotivationRepository.getMessageForType(typeKey)
        if (message.isNotBlank()) {
            val notificationManager = MotivationNotificationManager(context)
            notificationManager.showMotivationNotification(message)
        }

        NotificationSchedular.scheduleNextForType(context, typeKey)

        return Result.success()
    }
}
