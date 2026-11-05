package uz.oybek.ozamiz.data.feedback.impl

import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import uz.oybek.ozamiz.common.result.BaseResult
import uz.oybek.ozamiz.data.feedback.api.AppFeedbackRepo
import uz.oybek.ozamiz.data.feedback.model.AppFeedback
import uz.oybek.ozamiz.data.profile.api.ProfileRepo
import javax.inject.Inject

class AppFeedbackRepoImpl @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val profileRepo: ProfileRepo
) : AppFeedbackRepo {

    override suspend fun saveAppFeedback(
        feedbackText: String,
        ratingOption: String
    ): BaseResult<Unit, Exception?> {
        return try {
            val currentUser = profileRepo.currentUser
            val feedback = AppFeedback(
                userId = currentUser.uid,
                userName = currentUser.name,
                userPhone = currentUser.phoneNumber,
                feedbackText = feedbackText,
                ratingOption = ratingOption,
                timestamp = System.currentTimeMillis()
            )
            firestore.collection("app_feedback").document().set(feedback).await()
            BaseResult.Success(Unit)
        } catch (e: Exception) {
            BaseResult.Failure(e)
        }
    }
}
