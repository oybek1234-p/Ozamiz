package uz.oybek.ozamiz.data.foods

import androidx.annotation.Keep
import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import com.google.firebase.firestore.PropertyName // Import PropertyName

@Keep
@Parcelize
data class SimpleMealIdea(
    var id: String = "",
    var name: String = "", // Ovqat nomi/qisqa tavsifi (masalan: "Tuxum va sabzavot")
    var description: String? = null, // Batafsilroq tavsif/komponentlar (masalan: "2 ta qaynatilgan tuxum, pomidor va bodring")
    @get:PropertyName("imageUrl") @set:PropertyName("imageUrl") // Explicit naming for Firebase
    var imageUrl: String? = null,
    var mealTypeTag: String? = null, // "breakfast", "lunch", "dinner", "snack"
    var estimatedCalories: Int? = null, // Taxminiy kaloriya (AI'siz, admin kiritadi)
    var order: Int = 0 // Ko'rsatish tartibi
) : Parcelable {
    constructor() : this("", "", null, null, null, null, 0)
}