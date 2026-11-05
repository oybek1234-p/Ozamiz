package uz.oybek.ozamiz.data.feedback.model

import androidx.annotation.Keep

@Keep
data class AppFeedback(
    val userId: String = "",
    val userName: String = "",
    val userPhone: String = "",
    val feedbackText: String = "",
    val ratingOption: String = "",
    val timestamp: Long = System.currentTimeMillis()
)
