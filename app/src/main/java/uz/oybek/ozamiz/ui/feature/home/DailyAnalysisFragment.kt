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
import uz.oybek.ozamiz.ui.utils.ExerciseCalorieConverter
import uz.oybek.ozamiz.ui.utils.playSuccessSound

class DailyAnalysisFragment : BaseFragment<FragmentDailyAnalysisBinding>() {

    override fun inflateBinding(
        inflater: LayoutInflater,
        container: ViewGroup?
    ): FragmentDailyAnalysisBinding {
        return FragmentDailyAnalysisBinding.inflate(inflater, container, false)
    }
    
    override fun viewCreated(bind: FragmentDailyAnalysisBinding) {
        val type = arguments?.getString(ARG_TYPE) ?: TYPE_DAILY
        if (type == TYPE_DAILY || type == TYPE_WATER_60 || type == TYPE_EXERCISE_50) {
            playSuccessSound()
        }
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
                com.google.android.material.R.attr.colorSurfaceContainerLow,
                bgTypedValue,
                true
            )
            itemBinding.cardLayout.backgroundTintList = ColorStateList.valueOf(bgTypedValue.data)

            val textTypedValue = TypedValue()
            requireContext().theme.resolveAttribute(
                com.google.android.material.R.attr.colorOnSurface,
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
                bind.textAccentHeader.visibility = View.GONE
                val waterTitles = listOf(
                    R.string.water_congrats_title_1,
                    R.string.water_congrats_title_2,
                    R.string.water_congrats_title_3,
                    R.string.water_congrats_title_4,
                    R.string.water_congrats_title_5
                )
                bind.textTitle.text = getString(waterTitles.random())
                bind.lottieCelebrate.setAnimation(R.raw.water_filling)
                bind.containerDailyStats.visibility = View.VISIBLE
                bind.layoutPoints.visibility = View.VISIBLE
                
                val liters = waterMl / 1000f
                val waterPersonalMsg = when {
                    liters < 1.0f -> getString(R.string.water_personal_status_low, liters)
                    liters <= 2.0f -> getString(R.string.water_personal_status_medium, liters)
                    else -> getString(R.string.water_personal_status_high, liters)
                }
                bind.textContent.text = waterPersonalMsg
                bind.textContent.visibility = View.VISIBLE

                val waterBenefits = listOf(
                    R.string.water_benefit_item_1, R.string.water_benefit_item_2, R.string.water_benefit_item_3,
                    R.string.water_benefit_item_4, R.string.water_benefit_item_5, R.string.water_benefit_item_6,
                    R.string.water_benefit_item_7, R.string.water_benefit_item_8, R.string.water_benefit_item_9,
                    R.string.water_benefit_item_10
                )
                val shuffled = waterBenefits.shuffled().take(3)
                for (benefitRes in shuffled) {
                    addStatItem("", getString(benefitRes))
                }

                bind.textPoints.text = getString(R.string.plus_5_ball)
                bind.btnDone.text = getString(R.string.water_benefits_ajoyib)
            }

            TYPE_WATER_60 -> {
                bind.textAccentHeader.visibility = View.GONE
                val waterTitles = listOf(
                    R.string.water_congrats_title_1,
                    R.string.water_congrats_title_2,
                    R.string.water_congrats_title_3,
                    R.string.water_congrats_title_4,
                    R.string.water_congrats_title_5
                )
                bind.textTitle.text = getString(waterTitles.random())
                bind.lottieCelebrate.setAnimation(R.raw.water_filling)
                bind.containerDailyStats.visibility = View.VISIBLE
                bind.layoutPoints.visibility = View.VISIBLE
                
                val liters = waterMl / 1000f
                val waterPersonalMsg = when {
                    liters < 1.0f -> getString(R.string.water_personal_status_low, liters)
                    liters <= 2.0f -> getString(R.string.water_personal_status_medium, liters)
                    else -> getString(R.string.water_personal_status_high, liters)
                }
                bind.textContent.text = waterPersonalMsg
                bind.textContent.visibility = View.VISIBLE

                val waterBenefits = listOf(
                    R.string.water_benefit_item_1, R.string.water_benefit_item_2, R.string.water_benefit_item_3,
                    R.string.water_benefit_item_4, R.string.water_benefit_item_5, R.string.water_benefit_item_6,
                    R.string.water_benefit_item_7, R.string.water_benefit_item_8, R.string.water_benefit_item_9,
                    R.string.water_benefit_item_10
                )
                val shuffled = waterBenefits.shuffled().take(3)
                for (benefitRes in shuffled) {
                    addStatItem("", getString(benefitRes))
                }

                bind.textPoints.text = getString(R.string.plus_5_ball)
                bind.btnDone.text = getString(R.string.water_60_ajoyib)
            }

            TYPE_EXERCISE_BENEFITS -> {
                bind.textAccentHeader.visibility = View.GONE
                val exerciseTitles = listOf(
                    R.string.exercise_congrats_title_1,
                    R.string.exercise_congrats_title_2,
                    R.string.exercise_congrats_title_3,
                    R.string.exercise_congrats_title_4,
                    R.string.exercise_congrats_title_5
                )
                bind.textTitle.text = getString(exerciseTitles.random())
                bind.lottieCelebrate.setAnimation(R.raw.cat_happy)
                bind.containerDailyStats.visibility = View.VISIBLE
                bind.layoutPoints.visibility = View.VISIBLE
                bind.textContent.visibility = View.GONE

                val caloriesText = getString(R.string.exercise_summary_calories_format, caloriesBurned)
                addStatItem("🔥", caloriesText)

                val fatBurned = ExerciseCalorieConverter.getFatBurnedGram(caloriesBurned)
                val fatText = getString(R.string.exercise_summary_fat_format, fatBurned)
                addStatItem("💧", fatText)

                val foodEquiv = ExerciseCalorieConverter.getEquivalentFood(requireContext(), caloriesBurned)
                val foodText = getString(R.string.exercise_summary_food_format, foodEquiv)
                addStatItem("🍎", foodText)

                bind.textPoints.text = getString(R.string.plus_5_ball)
                bind.btnDone.text = getString(R.string.exercise_benefits_ajoyib)
            }

            TYPE_EXERCISE_50 -> {
                bind.textAccentHeader.visibility = View.GONE
                val exerciseTitles = listOf(
                    R.string.exercise_congrats_title_1,
                    R.string.exercise_congrats_title_2,
                    R.string.exercise_congrats_title_3,
                    R.string.exercise_congrats_title_4,
                    R.string.exercise_congrats_title_5
                )
                bind.textTitle.text = getString(exerciseTitles.random())
                bind.lottieCelebrate.setAnimation(R.raw.cat_happy)
                bind.containerDailyStats.visibility = View.VISIBLE
                bind.layoutPoints.visibility = View.VISIBLE
                bind.textContent.visibility = View.GONE

                val caloriesText = getString(R.string.exercise_summary_calories_format, caloriesBurned)
                addStatItem("🔥", caloriesText)

                val fatBurned = ExerciseCalorieConverter.getFatBurnedGram(caloriesBurned)
                val fatText = getString(R.string.exercise_summary_fat_format, fatBurned)
                addStatItem("💧", fatText)

                val foodEquiv = ExerciseCalorieConverter.getEquivalentFood(requireContext(), caloriesBurned)
                val foodText = getString(R.string.exercise_summary_food_format, foodEquiv)
                addStatItem("🍎", foodText)

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

    fun caloriesEquivalentText(kcal: Int): String {
        return ""
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

