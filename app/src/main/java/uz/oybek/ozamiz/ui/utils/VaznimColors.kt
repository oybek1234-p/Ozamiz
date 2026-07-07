package uz.oybek.ozamiz.ui.utils

import androidx.core.content.ContextCompat
import uz.oybek.ozamiz.R
import uz.oybek.ozamiz.appContext

object VaznimColors {
    private fun getColor(context: android.content.Context, resId: Int): Int = ContextCompat.getColor(context, resId)

    fun getPrimary(context: android.content.Context): Int = getColor(context, R.color.vaznim_primary)
    fun getOnPrimary(context: android.content.Context): Int = getColor(context, R.color.vaznim_on_primary)
    fun getSurfaceLow(context: android.content.Context): Int = getColor(context, R.color.vaznim_surface_low)
    fun getOutline(context: android.content.Context): Int = getColor(context, R.color.vaznim_outline)
    fun getGray(context: android.content.Context): Int = getColor(context, R.color.grey)
    fun getOrange(context: android.content.Context): Int = getColor(context, R.color.orange)
    fun getYellow(context: android.content.Context): Int = getColor(context, R.color.yellow)
    fun getGreen(context: android.content.Context): Int = getColor(context, R.color.green)

    fun getRed(context: android.content.Context): Int = getColor(context, R.color.red)
    fun getTrack(context: android.content.Context): Int = getColor(context, R.color.vaznim_track)
    fun getOnSurface(context: android.content.Context): Int = getColor(context, R.color.vaznim_on_surface)
    fun getOnSurfaceVariant(context: android.content.Context): Int = getColor(context, R.color.vaznim_on_surface_variant)
    fun getSurfaceContainerHigh(context: android.content.Context): Int = getColor(context, R.color.vaznim_surface_container_high)
}
