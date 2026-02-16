package uz.oybek.ozamiz.data.lessons.model

import com.google.firebase.firestore.DocumentId

/**
 * Bo'lim modeli - darslar ro'yxatidagi katta bo'lim (header)
 */
data class Section(
    @DocumentId
    val id: String = "",
    val title: String = "",
    val order: Int = 0
) {
    // Firestore uchun no-arg constructor
    constructor() : this("", "", 0)
}
