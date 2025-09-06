package uz.oybek.ozamiz.data.lessons

import androidx.annotation.Keep
import com.google.firebase.firestore.PropertyName

data class CourseModel(
    val id: String,
    var lessonId: String,
    var order: Int,
    var title: String,
    var about: String,
    var voiceUrl: String,
    var imageUrl: String
) {
    constructor() : this("", "", 0, "", "", "", "")
}

@Keep
data class CourseTestModel(
    val id: String = "",
    val lessonId: String = "",
    var order: Int = 0,
    val question: String = "",
    val imageUrl: String? = null, // Savol uchun rasm (ixtiyoriy)
    val answers: List<AnswerModel> = emptyList(),
    val rightAnswerHint: String = "", // To'g'ri javob uchun izoh
    val wrongAnswerHint: String = "" // Noto'g'ri javob uchun izoh
) {
    constructor() : this("", "", 0, "", null, emptyList(), "", "")
}

@Keep
data class AnswerModel(
    val id: String = "",
    val answerText: String = "",
    val imageUrl: String? = null, // Javob uchun rasm (ixtiyoriy)
    @get:PropertyName("correct")
    val correct: Boolean = false
) {
    constructor() : this("", "", null, false)
}