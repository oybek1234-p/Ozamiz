package uz.oybek.ozamiz.workers

import android.content.Context
import androidx.work.Worker
import androidx.work.WorkerParameters
import uz.oybek.ozamiz.data.motivation.MotivationNotificationManager
import uz.oybek.ozamiz.data.motivation.MotivationRepository

class MotivationWorker(context: Context, params: WorkerParameters) : Worker(context, params) {

    override fun doWork(): Result {
        val repository = MotivationRepository
        val message = repository.getMessageForCurrentTime()

        if (message != null) {
             val notificationManager = MotivationNotificationManager(applicationContext)
             notificationManager.showMotivationNotification(message)
        }
        
        return Result.success()
    }
}
