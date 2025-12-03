package uz.oybek.ozamiz.data.food.model

import androidx.annotation.Keep
import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

@Keep
data class RecipeModel(
    var id: String = "",
    var name: String = "",
    var imageUrl: String? = null,
    var categoryIds: List<String>? = null,
    var calories: Int? = null,
    var protein: Double? = null,
    var fat: Double? = null,
    var carbs: Double? = null,
    var ingredients: List<String>? = null,
    var preparationSteps: List<String>? = null,
    var preparationTimeMin: Int? = null,
    var videoUrl: String? = null,
    @ServerTimestamp
    val createdAt: Date? = null
) {
    constructor() : this(
        "", "",
        null, null, null, null, null,
        null, null, null
    )
}