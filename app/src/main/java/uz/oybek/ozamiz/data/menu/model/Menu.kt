package uz.oybek.ozamiz.data.menu.model

import android.os.Parcelable
import androidx.annotation.Keep
import com.google.firebase.firestore.ServerTimestamp
import kotlinx.parcelize.Parcelize
import java.util.Date

@Keep
@Parcelize
data class Menu(
    var id: String = "",
    var name: String = "",
    var description: String = "",
    var ingredients: List<String> = emptyList(),
    var imageUrl: String? = null,
    var categoryIds: List<String> = emptyList(),
    var calories: Int = 0,
    var protein: Double = 0.0,
    var fat: Double = 0.0,
    var carbs: Double = 0.0,
    var fiber: Double = 0.0,
    var sugar: Double = 0.0,
    var salt: Double = 0.0,
    var searchKeywords: List<String> = emptyList(),
    var type: String = "menu",
    var preparationMethod: List<String> = emptyList(),
    var about: String = "",
    var richIn: String = "",
    var consumptionTime: String = "",
    var preparationTime: Int = 15,
    var difficulty: String = "Oson",
    @ServerTimestamp
    var createdAt: Date? = null
) : Parcelable
