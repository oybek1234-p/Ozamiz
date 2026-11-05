package uz.oybek.ozamiz.data.feedback.api

import uz.oybek.ozamiz.common.result.BaseResult

interface AppFeedbackRepo {
    suspend fun saveAppFeedback(feedbackText: String, ratingOption: String): BaseResult<Unit, Exception?>
}
