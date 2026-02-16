package uz.oybek.ozamiz.data.lessons.model

import com.google.firebase.firestore.DocumentId

/**
 * Test savoli modeli
 */
data class Quiz(
    @DocumentId
    val id: String = "",
    val topicId: String = "",
    val question: String = "",
    val options: List<String> = emptyList(),
    val correctIndex: Int = 0
) {
    constructor() : this("", "", "", emptyList(), 0)
}
