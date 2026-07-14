package uz.oybek.ozamiz.ui.feature.home

import android.content.res.ColorStateList
import android.os.Bundle
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.fragment.findNavController
import uz.oybek.ozamiz.R
import uz.oybek.ozamiz.databinding.FragmentDailyAnalysisBinding
import uz.oybek.ozamiz.databinding.ItemAnalysisStatBinding
import uz.oybek.ozamiz.ui.base.BaseFragment

class DailyAnalysisFragment : BaseFragment<FragmentDailyAnalysisBinding>() {

    override fun inflateBinding(
        inflater: LayoutInflater,
        container: ViewGroup?
    ): FragmentDailyAnalysisBinding {
        return FragmentDailyAnalysisBinding.inflate(inflater, container, false)
    }
    
    override fun viewCreated(bind: FragmentDailyAnalysisBinding) {
        val type = arguments?.getString(ARG_TYPE) ?: TYPE_DAILY
        val foodCount = arguments?.getInt(ARG_FOOD_COUNT) ?: 0
        val healthyFoodCount = arguments?.getInt(ARG_HEALTHY_FOOD_COUNT) ?: 0
        val points = arguments?.getInt(ARG_POINTS) ?: 0
        val waterMl = arguments?.getInt(ARG_WATER_ML) ?: 0
        val hasExercise = arguments?.getBoolean(ARG_HAS_EXERCISE) ?: false
        val hasProteinGoal = arguments?.getBoolean(ARG_HAS_PROTEIN) ?: false
        val caloriesBurned = arguments?.getInt(ARG_CALORIES_BURNED) ?: 0

        bind.containerDailyStats.removeAllViews()

        fun addStatItem(
            emoji: String,
            textVal: String
        ) {
            val itemBinding = ItemAnalysisStatBinding.inflate(
                layoutInflater,
                bind.containerDailyStats,
                false
            )
            itemBinding.textEmoji.text = emoji

            val bgTypedValue = TypedValue()
            requireContext().theme.resolveAttribute(
                com.google.android.material.R.attr.colorPrimaryContainer,
                bgTypedValue,
                true
            )
            itemBinding.cardLayout.backgroundTintList = ColorStateList.valueOf(bgTypedValue.data)

            val textTypedValue = TypedValue()
            requireContext().theme.resolveAttribute(
                com.google.android.material.R.attr.colorOnPrimaryContainer,
                textTypedValue,
                true
            )
            itemBinding.textDescription.setTextColor(textTypedValue.data)

            val cleanDescription = textVal.removePrefix(emoji).trim()
            itemBinding.textDescription.text = cleanDescription
            bind.containerDailyStats.addView(itemBinding.root)
        }


        when (type) {
            TYPE_WATER_BENEFITS -> {
                bind.textAccentHeader.text = getString(R.string.sog_liq_bahosi).trim().uppercase()
                bind.textTitle.text = getString(R.string.water_benefits_title)
                bind.lottieCelebrate.setAnimation(R.raw.water_filling)
                bind.containerDailyStats.visibility = View.VISIBLE
                bind.layoutPoints.visibility = View.VISIBLE
                bind.textContent.visibility = View.GONE
                addStatItem("💧", getString(R.string.water_benefit_item_1))
                addStatItem("🔥", getString(R.string.water_benefit_item_2))
                addStatItem("✨", getString(R.string.water_benefit_item_3))
                addStatItem("🧠", getString(R.string.water_benefit_item_4))

                bind.textPoints.text = getString(R.string.plus_5_ball)
                bind.btnDone.text = getString(R.string.water_benefits_ajoyib)
            }

            TYPE_WATER_60 -> {
                val waterLiters = waterMl / 1000f
                val speedupMultiplier = waterLiters
                bind.textAccentHeader.text = getString(R.string.sog_liq_bahosi).trim().uppercase()
                bind.textTitle.text =
                    getString(R.string.water_60_title_dynamic, waterLiters, speedupMultiplier)
                bind.lottieCelebrate.setAnimation(R.raw.water_filling)
                bind.containerDailyStats.visibility = View.VISIBLE
                bind.layoutPoints.visibility = View.VISIBLE
                bind.textContent.visibility = View.GONE

                addStatItem("💧", getString(R.string.water_benefit_item_1))
                addStatItem("🔥", getString(R.string.water_benefit_item_2))
                addStatItem("✨", getString(R.string.water_benefit_item_3))
                addStatItem("🧠", getString(R.string.water_benefit_item_4))

                bind.textPoints.text = getString(R.string.plus_5_ball)
                bind.btnDone.text = getString(R.string.water_60_ajoyib)
            }

            TYPE_EXERCISE_BENEFITS -> {
                bind.textAccentHeader.text = getString(R.string.sog_liq_bahosi).trim().uppercase()
                bind.textTitle.text = getString(R.string.exercise_benefits_title)
                bind.lottieCelebrate.setAnimation(R.raw.cat_happy)
                bind.containerDailyStats.visibility = View.VISIBLE
                bind.layoutPoints.visibility = View.VISIBLE
                bind.textContent.visibility = View.GONE

                addStatItem("⚡", getString(R.string.exercise_benefit_item_1))
                addStatItem("💪", getString(R.string.exercise_benefit_item_2))
                addStatItem("❤️", getString(R.string.exercise_benefit_item_3))
                addStatItem("🔋", getString(R.string.exercise_benefit_item_4))
                val caloriesText = caloriesEquivalentText(caloriesBurned)
                if (caloriesText.isNotEmpty()) addStatItem("🔥", caloriesText)

                bind.textPoints.text = getString(R.string.plus_5_ball)
                bind.btnDone.text = getString(R.string.exercise_benefits_ajoyib)
            }

            TYPE_EXERCISE_50 -> {
                bind.textAccentHeader.text = getString(R.string.sog_liq_bahosi).trim().uppercase()
                bind.textTitle.text = getString(R.string.exercise_50_title)
                bind.lottieCelebrate.setAnimation(R.raw.cat_happy)
                bind.containerDailyStats.visibility = View.VISIBLE
                bind.layoutPoints.visibility = View.VISIBLE
                bind.textContent.visibility = View.GONE

                val caloriesText = caloriesEquivalentText(caloriesBurned)
                if (caloriesText.isNotEmpty()) addStatItem("🔥", caloriesText)
                addStatItem("⚡", getString(R.string.exercise_benefit_item_1))
                addStatItem("💪", getString(R.string.exercise_benefit_item_2))
                addStatItem("❤️", getString(R.string.exercise_benefit_item_3))
                addStatItem("🔋", getString(R.string.exercise_benefit_item_4))

                bind.textPoints.text = getString(R.string.plus_5_ball)
                bind.btnDone.text = getString(R.string.exercise_50_ajoyib)
            }

            else -> {
                bind.textAccentHeader.text = getString(R.string.ajoyib_title).uppercase()
                bind.textTitle.text = getString(R.string.daily_analysis_title)
                bind.lottieCelebrate.setAnimation(R.raw.water_melon)
                bind.containerDailyStats.visibility = View.VISIBLE
                bind.layoutPoints.visibility = View.VISIBLE
                bind.textContent.visibility = View.GONE

                addStatItem("🥗", getString(R.string.analysis_food_count_format, foodCount))
                addStatItem("🍏", getString(R.string.analysis_healthy_food_format, healthyFoodCount))
                addStatItem("💧", getString(R.string.analysis_water_text_format, (waterMl / 1000f)))

                if (hasExercise) {
                    addStatItem("🏃", getString(R.string.analysis_exercise_done))
                }
                if (hasProteinGoal) {
                    addStatItem("🧬", getString(R.string.analysis_protein_done))
                }

                bind.textPoints.text = getString(R.string.analysis_points_format, points)
                bind.btnDone.text = getString(R.string.food_analysis_btn)
            }
        }

        bind.btnBack.setOnClickListener {
            findNavController().popBackStack()
        }

        bind.btnDone.setOnClickListener {
            findNavController().popBackStack()
        }
    }

    val caloriesFoods = listOf(
        20 to (R.string.food_honey_spoon to "🍯"),
        52 to (R.string.food_apple to "🍎"),
        60 to (R.string.food_milk_glass to "🥛"),
        70 to (R.string.food_boiled_egg to "🥚"),
        89 to (R.string.food_banana to "🍌"),
        90 to (R.string.food_yogurt to "🥛"),
        100 to (R.string.food_salad_plate to "🥗"),
        105 to (R.string.food_cookie to "🍪"),
        120 to (R.string.food_slice_bread to "🍞"),
        120 to (R.string.food_oatmeal to "🥣"),
        130 to (R.string.food_shorva to "🍲"),
        135 to (R.string.food_cola_glass to "🥤"),
        150 to (R.string.food_cheese_slice to "🧀"),
        160 to (R.string.food_almonds to "🌰"),
        170 to (R.string.food_green_tea to "🍵"),
        200 to (R.string.food_lavash to "🫓"),
        210 to (R.string.food_hot_dog to "🌭"),
        220 to (R.string.food_lagman_bowl to "🍜"),
        230 to (R.string.food_ice_cream to "🍦"),
        240 to (R.string.food_manti to "🥟"),
        250 to (R.string.food_kebab_100g to "🍢"),
        260 to (R.string.food_donut to "🍩"),
        270 to (R.string.food_pasta_plate to "🍝"),
        280 to (R.string.food_somsa to "🥐"),
        300 to (R.string.food_shashlik to "🍖"),
        320 to (R.string.food_fries to "🍟"),
        350 to (R.string.food_snickers to "🍫"),
        400 to (R.string.food_plov_bowl to "🍚"),
        450 to (R.string.food_pizza_slice to "🍕"),
        500 to (R.string.food_hamburger to "🍔")
    )

    fun caloriesEquivalentText(kcal: Int): String {
        if (kcal <= 0) return ""
        val match = caloriesFoods.minByOrNull { kotlin.math.abs(it.first - kcal) } ?: return ""
        val foodName = getString(match.second.first)
        val emoji = match.second.second
        return getString(R.string.exercise_calories_equal_format, kcal, emoji, foodName)

    }

    companion object {
        private const val ARG_FOOD_COUNT = "food_count"
        private const val ARG_HEALTHY_FOOD_COUNT = "healthy_food_count"
        private const val ARG_POINTS = "points"
        private const val ARG_WATER_ML = "water_ml"
        private const val ARG_HAS_EXERCISE = "has_exercise"
        private const val ARG_HAS_PROTEIN = "has_protein"
        private const val ARG_NO_SWEETS = "no_sweets"
        private const val ARG_NO_BREAD = "no_bread"
        private const val ARG_TYPE = "type"
        private const val ARG_CALORIES_BURNED = "calories_burned"

        const val TYPE_DAILY = "DAILY"
        const val TYPE_WATER_BENEFITS = "WATER_BENEFITS"
        const val TYPE_WATER_60 = "WATER_60"
        const val TYPE_EXERCISE_BENEFITS = "EXERCISE_BENEFITS"
        const val TYPE_EXERCISE_50 = "EXERCISE_50"

        fun createArgs(
            foodCount: Int = 0,
            healthyFoodCount: Int = 0,
            points: Int = 0,
            waterMl: Int = 0,
            hasExercise: Boolean = false,
            hasProteinGoal: Boolean = false,
            noSweets: Boolean = false,
            noBread: Boolean = false,
            type: String = TYPE_DAILY,
            caloriesBurned: Int = 0
        ): Bundle {
            return Bundle().apply {
                putInt(ARG_FOOD_COUNT, foodCount)
                putInt(ARG_HEALTHY_FOOD_COUNT, healthyFoodCount)
                putInt(ARG_POINTS, points)
                putInt(ARG_WATER_ML, waterMl)
                putBoolean(ARG_HAS_EXERCISE, hasExercise)
                putBoolean(ARG_HAS_PROTEIN, hasProteinGoal)
                putBoolean(ARG_NO_SWEETS, noSweets)
                putBoolean(ARG_NO_BREAD, noBread)
                putString(ARG_TYPE, type)
                putInt(ARG_CALORIES_BURNED, caloriesBurned)
            }
        }
    }
}

