package uz.oybek.ozamiz.ui.utils

import android.content.Context
import android.util.TypedValue
import androidx.core.content.ContextCompat
import uz.oybek.ozamiz.R
import uz.oybek.ozamiz.data.analyzer.model.Macronutrient

object VaznimColors {
    private fun getColor(context: Context, resId: Int): Int = ContextCompat.getColor(context, resId)

    private fun getThemeColor(context: Context, attrRes: Int): Int {
        val typedValue = TypedValue()
        return if (context.theme.resolveAttribute(attrRes, typedValue, true)) {
            typedValue.data
        } else {
            0
        }
    }

    fun getPrimary(context: Context): Int = getColor(context, R.color.vaznim_primary)
    fun getOnPrimary(context: Context): Int = getColor(context, R.color.vaznim_on_primary)
    fun getSurfaceLow(context: Context): Int = getColor(context, R.color.vaznim_surface_low)
    fun getSurfaceContainer(context: Context): Int = getColor(context, R.color.md_theme_light_surfaceContainer)
    fun getOutline(context: Context): Int = getColor(context, R.color.vaznim_outline)
    fun getOutlineVariant(context: Context): Int = getColor(context, R.color.md_theme_light_surfaceVariant)
    fun getGray(context: Context): Int = getColor(context, R.color.grey)
    fun getOrange(context: Context): Int = getColor(context, R.color.orange)
    fun getYellow(context: Context): Int = getColor(context, R.color.yellow)
    fun getGreen(context: Context): Int = getColor(context, R.color.green)

    fun getRed(context: Context): Int = getColor(context, R.color.red)
    fun getTrack(context: Context): Int = getColor(context, R.color.vaznim_track)
    fun getOnSurface(context: Context): Int = getColor(context, R.color.vaznim_on_surface)
    fun getOnSurfaceVariant(context: Context): Int = getColor(context, R.color.vaznim_on_surface_variant)
    fun getSurfaceContainerHigh(context: Context): Int = getColor(context, R.color.vaznim_surface_container_high)

    // Semantic Food Macro Grade Colors (Good / Moderate / Bad)
    fun getMacroGood(context: Context): Int =
        getThemeColor(context, R.attr.colorMacroGood).takeIf { it != 0 } ?: getColor(context, R.color.macro_grade_good)

    fun getMacroModerate(context: Context): Int =
        getThemeColor(context, R.attr.colorMacroModerate).takeIf { it != 0 } ?: getColor(context, R.color.macro_grade_moderate)

    fun getMacroBad(context: Context): Int =
        getThemeColor(context, R.attr.colorMacroBad).takeIf { it != 0 } ?: getColor(context, R.color.macro_grade_bad)

    fun getMacroColor(context: Context, macro: Macronutrient, isGainWeight: Boolean = false): Int? {
        if (macro == Macronutrient.UNKNOWN) return null
        return getOnSurfaceVariant(context)
    }
}
