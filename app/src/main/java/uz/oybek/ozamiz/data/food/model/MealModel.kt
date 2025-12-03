package uz.oybek.ozamiz.data.food.model

import android.os.Parcelable
import androidx.annotation.Keep
import kotlinx.parcelize.Parcelize

@Keep
@Parcelize
data class MealModel(
    var id: String = "",
    var name: String = "",
    var description: String? = null,
    var imageUrl: String? = null,
    var mealTypeTag: String? = null,
    var estimatedCalories: Int? = null,
    var order: Int = 0
) : Parcelable {
    constructor() : this("", "", null, null, null, null, 0)
}