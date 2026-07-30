package uz.oybek.ozamiz.data.feedback

import kotlinx.coroutines.tasks.await
import uz.oybek.ozamiz.data.firebase.FirebasePaths
import uz.oybek.ozamiz.data.user.local.LocalUser

object AppFeedbackRepository {

    suspend fun saveAppFeedback(feedbackText: String, ratingOption: String) {
        val user = LocalUser.user
        val map = mapOf(
            "userId" to user.uid,
            "userName" to user.name,
            "userPhone" to user.phoneNumber,
            "feedbackText" to feedbackText,
            "ratingOption" to ratingOption,
            "timestamp" to System.currentTimeMillis()
        )
        FirebasePaths.Firestore.appFeedback().document().set(map).await()
    }
}
