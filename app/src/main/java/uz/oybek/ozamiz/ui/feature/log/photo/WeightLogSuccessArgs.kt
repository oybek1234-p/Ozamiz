package uz.oybek.ozamiz.ui.feature.log.photo

import android.os.Parcelable
import androidx.annotation.Keep
import kotlinx.parcelize.Parcelize

@Keep
@Parcelize
data class WeightLogSuccessArgs(
    val hasProgress: Boolean = true,
    val isGainGoal: Boolean = false,
    val weightChangeKg: Double = 0.0,
    val totalWeightChangeKg: Double = 0.0,
    val measurementChangeCm: Double = 0.0,
    val analysisMessage: String? = null,
    val photoUri: String? = null,
    val beforePhotoUri: String? = null,
    val startWeightKg: Double = 0.0,
    val currentWeightKg: Double = 0.0,
    val daysCount: Int = 1
) : Parcelable
