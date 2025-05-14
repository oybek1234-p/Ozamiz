package uz.oybek.ozamiz.data.foods

import androidx.annotation.Keep
import com.google.firebase.firestore.PropertyName // Import PropertyName

@Keep
data class FoodAdvice(
    var id: String = "",
    var title: String? = null, // Maslahat bloki sarlavhasi (Masalan: "Non haqida")
    var content: String = "", // Maslahat matni
    var order: Int = 0, // Ko'rsatish tartibi
    var tag: String? = null // Filtr/identifikatsiya uchun tag (masalan: "non", "nonushta_umumiy")
) {
    constructor() : this("", "","", 0, null)
}