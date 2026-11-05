package uz.oybek.ozamiz.ui.feature.community.add

import android.os.Parcelable
import androidx.annotation.Keep
import kotlinx.parcelize.Parcelize

@Keep
@Parcelize
data class AddPostArgs(
    val imageUri: String? = null,
    val beforeImageUri: String? = null,
    val initialDescription: String? = null,
    val isWeightProgressPost: Boolean = false
) : Parcelable
