package uz.oybek.ozamiz.data.model

import android.os.Parcelable
import androidx.annotation.Keep
import com.google.firebase.firestore.ServerTimestamp
import kotlinx.parcelize.Parcelize
import java.util.Date

@Keep
@Parcelize
data class WeightPhotoLog(
    var id: String = "",
    val userId: String = "",
    @ServerTimestamp
    var timestamp: Date? = null,
    var weightKg: Double = 0.0,
    var photoUrl: String = ""
) : Parcelable {
    constructor() : this("", "", null, 0.0, "")
}