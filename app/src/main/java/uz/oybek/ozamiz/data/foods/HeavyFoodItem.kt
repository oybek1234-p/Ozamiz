package uz.oybek.ozamiz.data.foods

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class HeavyFoodItem(
    val id: String,
    val name: String,
    val qachon: String, // Qachon, Qancha, Mahal
    val qancha: String? = null,
    val mahal: String? = null,
    val estimatedCalories: Int? = null // Taxminiy kaloriya
) : Parcelable