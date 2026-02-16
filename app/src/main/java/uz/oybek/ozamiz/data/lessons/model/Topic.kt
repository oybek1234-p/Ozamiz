package uz.oybek.ozamiz.data.lessons.model

import com.google.firebase.firestore.DocumentId

/**
 * Mavzu modeli - bo'lim ichidagi bitta mavzu
 */
data class Topic(
    @DocumentId
    val id: String = "",
    val sectionId: String = "",
    val title: String = "",
    val imageUrl: String = "",
    val order: Int = 0,
    val isLocked: Boolean = true
) {
    constructor() : this("", "", "", "", 0, true)
}
