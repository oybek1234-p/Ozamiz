package uz.oybek.ozamiz.data.foods

import androidx.annotation.Keep
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.PropertyName
import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

@Keep
data class Recipe(
    var id: String = "",
    var name: String = "",
    @get:PropertyName("imageUrl") @set:PropertyName("imageUrl")
    var imageUrl: String? = null,
    var categoryIds: List<String>? = null,
    var calories: Int? = null,
    var protein: Double? = null, // gram
    var fat: Double? = null, // gram
    var carbs: Double? = null, // gram
    var ingredients: List<String>? = null, // Masalliqlar ro'yxati
    var preparationSteps: List<String>? = null, // Tayyorlash qadamlari
    var preparationTimeMin: Int? = null, // Tayyorlash vaqti (daqiqa)
    var videoUrl: String? = null, // Video linki (ixtiyoriy)
    var tags: List<String>? = null, // Qo'shimcha teglar (masalan, "Tez tayyorlanadigan")

    @ServerTimestamp
    val createdAt: Date? = null,
    @get:PropertyName("isPopular") @set:PropertyName("isPopular")
    var isPopular: Boolean = false, // Mashhur retseptlar uchun
    var order: Int = 0 // Ko'rsatish tartibi (ixtiyoriy)
) {
    constructor() : this(
        "", "", null, null, null, null, null,
        null, null, null)
}