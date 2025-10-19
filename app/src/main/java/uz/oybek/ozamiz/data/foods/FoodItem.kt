package uz.oybek.ozamiz.data.foods

import androidx.annotation.Keep
import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Keep
@Parcelize
data class FoodItem(
    var id: String = "",
    var name: String = "",
    var description: String? = null,
    var imageUrl: String? = null,
    var calories: Int? = null,
    var protein: Double? = null,
    var fat: Double? = null,
    var carbs: Double? = null,
    var mealTypeTag: String? = null,
    var repeatInterval: Int = 7,
    var excludedIfRecent: List<String>? = null,
    var suitableFor: List<String>? = null,
    var order: Int = 0
) : Parcelable {
    constructor() : this(
        "", "", null, null, null, null, null, null, null, 7, null, null, 0
    )
}