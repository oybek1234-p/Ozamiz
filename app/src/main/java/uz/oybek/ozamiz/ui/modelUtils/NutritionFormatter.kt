package uz.oybek.ozamiz.ui.modelUtils

import uz.oybek.ozamiz.R
import uz.oybek.ozamiz.appContext
import uz.oybek.ozamiz.common.util.orZero
import uz.oybek.ozamiz.common.util.readableFormat
import uz.oybek.ozamiz.common.util.round
import kotlin.math.abs

object NutritionFormatter {

    /**
     * Vazn qiymatini "kg" bilan qaytaradi.
     *
     * Example: "15 kg", "65.4 kg"
     */
    fun kg(
        weight: Double?,
        default: String? = null
    ): String {
        val safeWeight = weight.orZero().round()
        if (safeWeight <= 0.0) {
            return default ?: appContext.getString(R.string.unit_weight_default)
        }
        return appContext.getString(R.string.unit_weight_kg_format, safeWeight.readableFormat())
    }

    /**
     * Vazn o'zgarishini ishora (+ / -) va "kg" bilan qaytaradi.
     *
     * Example: "+1.5 kg", "-0.8 kg", "0 kg"
     */
    fun kgChange(diff: Double?): String {
        val safeDiff = diff.orZero().round()
        val absVal = abs(safeDiff)
        if (absVal < 0.01) {
            return appContext.getString(R.string.unit_weight_kg_format, "0")
        }

        return if (safeDiff > 0) {
            appContext.getString(R.string.unit_weight_change_positive_format, absVal.readableFormat())
        } else {
            appContext.getString(R.string.unit_weight_change_negative_format, absVal.readableFormat())
        }
    }

    /**
     * Minimal va maksimal vazn oralig'ini qaytaradi.
     *
     * Example: "60 kg - 65 kg"
     */
    fun kgRange(
        min: Double?,
        max: Double?
    ): String {
        val minStr = kg(min, "--")
        val maxStr = kg(max, "--")
        return appContext.getString(R.string.unit_weight_range_format, minStr, maxStr)
    }

    // ==========================================
    // KALORIYA (CALORIES)
    // ==========================================

    /**
     * Kaloriya qiymatini "kkal" bilan qaytaradi.
     *
     * Example: "1200 kkal"
     */
    fun kcal(calories: Int?): String =
        appContext.getString(R.string.unit_nutrition_calories_format, calories.orZero())

    /**
     * Kaloriya progressini qaytaradi.
     *
     * Example: "1200 / 2000 kkal"
     */
    fun kcalProgress(current: Int?, target: Int?): String = appContext.getString(
        R.string.unit_nutrition_calorie_progress_format,
        current.orZero(),
        target.orZero()
    )

    // ==========================================
    // MAKROLAR (MACROS)
    // ==========================================

    /**
     * Makro element (oqsil, yog', uglevod) qiymatini "gr" bilan qaytaradi.
     *
     * Example: "12 gr", "45.5 gr"
     */
    fun gr(grams: Double?): String = appContext.getString(
        R.string.unit_nutrition_grams_format,
        grams.readableFormat()
    )

    /**
     * Qabul qilingan va maqsad qilingan gramm progressini qaytaradi.
     *
     * Example: "45 / 90 gr"
     */
    fun grProgress(consumed: Int?, target: Int?): String = appContext.getString(
        R.string.unit_nutrition_macro_progress_format,
        consumed.orZero(),
        target.orZero()
    )

    // ==========================================
    // PORSIYA VA MASALLIQLAR
    // ==========================================

    /**
     * Porsiya sonini qaytaradi.
     *
     * Example: "1", "1.5", "2"
     */
    fun portion(portion: Double?): String = portion.readableFormat(default = "1")

    /**
     * Masalliq miqdori va uning o'lchov birligini birlashtirib qaytaradi.
     *
     * Example: "150 gr", "2 dona", "200 ml", "12.5 gr"
     */
    fun ingredientAmount(amount: Double?, unit: String?): String {
        val a = amount.readableFormat(default = "")
        val u = unit.orEmpty()
        return "$a $u".trim()
    }

    // ==========================================
    // SUV (WATER)
    // ==========================================

    /**
     * Suv miqdorini millilitrlarda ("ml") qaytaradi.
     *
     * Example: "1500 ml"
     */
    fun waterMl(ml: Int?): String =
        appContext.getString(R.string.unit_nutrition_water_ml_format, ml.orZero())

    /**
     * Ichilgan va maqsad qilingan suv progressini "ml" da qaytaradi.
     *
     * Example: "1500 / 2500 ml"
     */
    fun waterProgress(consumedMl: Int?, targetMl: Int?): String = appContext.getString(
        R.string.unit_nutrition_water_progress_format,
        consumedMl.orZero(),
        targetMl.orZero()
    )

    /**
     * Suv miqdorini litrlarda ("L") qaytaradi.
     *
     * Example: "1.5 L", "2 L"
     */
    fun waterLiters(ml: Int?): String {
        val liters = ml.orZero() / 1000.0
        return appContext.getString(R.string.unit_nutrition_water_liters_format, liters.readableFormat())
    }
}