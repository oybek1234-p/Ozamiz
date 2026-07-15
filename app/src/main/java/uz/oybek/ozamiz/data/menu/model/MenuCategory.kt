package uz.oybek.ozamiz.data.menu.model

import android.os.Parcelable
import androidx.annotation.Keep
import kotlinx.parcelize.Parcelize

@Keep
@Parcelize
data class MenuCategory(
    var id: String = "",
    var name: String = "",
    var imageUrl: String = "",
    var meal: Boolean = true
) : Parcelable
