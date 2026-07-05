package uz.oybek.ozamiz.ui.utils

import android.graphics.Color
import androidx.core.content.ContextCompat
import uz.oybek.ozamiz.R
import uz.oybek.ozamiz.appContext

object VaznimColors {
    private fun getColor(resId: Int): Int = ContextCompat.getColor(appContext, resId)

    fun getPrimary(): Int = getColor(R.color.md_theme_light_primary)
    fun getOnPrimary(): Int = getColor(R.color.md_theme_light_onPrimary)
    fun getSurfaceLow(): Int = getColor(R.color.md_theme_light_surfaceContainerLow)
    fun getOutline(): Int = getColor(R.color.md_theme_light_outline)
    fun getGray(): Int = getColor(R.color.grey)
    fun getOrange(): Int = getColor(R.color.orange)
    fun getYellow(): Int = getColor(R.color.yellow)
    fun getGreen(): Int = getColor(R.color.green)
    fun getTrack(): Int = getColor(R.color.md_theme_light_surfaceContainerHigh)
    fun getOnSurface(): Int = getColor(R.color.md_theme_light_onSurface)
    fun getOnSurfaceVariant(): Int = getColor(R.color.md_theme_light_onSurfaceVariant)
    fun getSurfaceContainerHigh(): Int = getColor(R.color.md_theme_light_surfaceContainerHigh)
}
