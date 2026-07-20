package uz.oybek.ozamiz.data.logs

import kotlinx.coroutines.tasks.await
import uz.oybek.ozamiz.data.firebase.FirebasePaths
import uz.oybek.ozamiz.data.firebase.currentUserId

object FoodLogFeedbackRepository {

    suspend fun saveFeedback(dateStr: String, option: String) {
        val userId = currentUserId
        if (userId.isBlank()) return
        val map = mapOf(
            "userId" to userId,
            "date" to dateStr,
            "option" to option,
            "timestamp" to System.currentTimeMillis()
        )
        FirebasePaths.Firestore.foodLogFeedback().document().set(map).await()
    }
}
