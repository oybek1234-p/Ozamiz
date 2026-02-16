package uz.oybek.ozamiz.data.lessons.model

import com.google.firebase.firestore.DocumentId

/**
 * Ekran/Kontent modeli - mavzu ichidagi bitta sahifa
 */
data class Screen(
    @DocumentId
    val id: String = "",
    val topicId: String = "",
    val title: String = "",
    val imageUrl: String = "",
    val mainText: String = "",
    val keyInfo: String = "",
    val order: Int = 0
) {
    constructor() : this("", "", "", "", "", "", 0)
}
