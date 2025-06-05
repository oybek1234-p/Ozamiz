package uz.oybek.ozamiz.data.foods

import androidx.annotation.Keep
import com.google.firebase.firestore.PropertyName // Import PropertyName
import java.util.UUID

@Keep
data class FoodCategory(
    @get:PropertyName("id") @set:PropertyName("id")
    var id: String,
    var name: String = "",
    @get:PropertyName("imageUrl") @set:PropertyName("imageUrl") // Explicit naming for Firebase
    var imageUrl: String? = null,
    var order: Int = 0
) {
    constructor() : this("", "", null, 0)
}