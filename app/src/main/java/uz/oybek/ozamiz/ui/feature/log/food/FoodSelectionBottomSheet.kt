package uz.oybek.ozamiz.ui.feature.log.food

import android.content.res.ColorStateList
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.core.widget.addTextChangedListener
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.chip.Chip
import uz.oybek.ozamiz.R
import uz.oybek.ozamiz.common.util.orZero
import uz.oybek.ozamiz.common.util.readableFormat
import uz.oybek.ozamiz.data.food.model.FoodItem
import uz.oybek.ozamiz.data.food.model.FoodTag
import uz.oybek.ozamiz.data.food.model.ServingSize
import uz.oybek.ozamiz.data.logs.model.FoodLog
import uz.oybek.ozamiz.databinding.BottomSheetFoodSelectionDetailBinding
import uz.oybek.ozamiz.ui.modelUtils.NutritionFormatter
import uz.oybek.ozamiz.ui.utils.colorRes
import uz.oybek.ozamiz.ui.utils.descRes
import uz.oybek.ozamiz.ui.utils.VaznimColors
import uz.oybek.ozamiz.ui.utils.getTitle
import uz.oybek.ozamiz.ui.utils.healthRatingInfo
import uz.oybek.ozamiz.ui.utils.showKeyboard
import uz.oybek.ozamiz.ui.utils.titleRes
import kotlin.math.roundToInt

class FoodSelectionBottomSheet(
    private val foodItem: FoodItem,
    private val mealType: String,
    private val onLogAdded: (FoodLog) -> Unit
) : BottomSheetDialogFragment() {

    private var _binding: BottomSheetFoodSelectionDetailBinding? = null
    private val binding get() = _binding!!

    private var servings: List<ServingSize> = emptyList()
    private var selectedServingSize: ServingSize? = null
    private var quantity: Double = 1.0

    private var isGramMode: Boolean = false
    private var customGrams: Double = 100.0

    private val currentWeight: Double
        get() = if (isGramMode) customGrams else (selectedServingSize?.grams ?: 100.0) * quantity

    override fun getTheme(): Int = R.style.BottomSheetDialogTheme

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = BottomSheetFoodSelectionDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onStart() {
        super.onStart()
        val dialog = dialog as? BottomSheetDialog ?: return
        val bottomSheet = dialog.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet) ?: return
        val behavior = BottomSheetBehavior.from(bottomSheet)
        behavior.state = BottomSheetBehavior.STATE_EXPANDED
        behavior.skipCollapsed = true
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupUI()
    }

    private fun setupUI() = with(binding) {
        val context = requireContext()

        // 1. Food Name
        textViewFoodNameDetail.text = foodItem.name

        // 2. Health Advice Card (Clear, readable, separated chip badge)
        val ratingInfo = foodItem.healthRatingInfo
        val gradeColor = ContextCompat.getColor(context, ratingInfo.colorRes)
        val gradeBgColor = ContextCompat.getColor(context, ratingInfo.bgColorRes)

        textHealthRatingTitle.text = getString(ratingInfo.titleRes)
        textHealthRatingTitle.setTextColor(gradeColor)
        textHealthRatingTitle.backgroundTintList = ColorStateList.valueOf(gradeBgColor)

        textConsumptionFrequency.text = getString(foodItem.consumptionFrequency.titleRes)
        textHealthRatingDesc.text = getString(ratingInfo.descRes)

        if (foodItem.advice.isNotBlank()) {
            textFoodAdvice.isVisible = true
            textFoodAdvice.text = foodItem.advice
        } else {
            textFoodAdvice.isVisible = false
        }

        // Nutrition tags (Max 3, clean text without emojis)
        val filteredNutritionTags = foodItem.tags
            .filterNot { it in EXCLUDED_FOOD_TAGS }
            .take(3)

        chipGroupFoodTags.removeAllViews()
        if (filteredNutritionTags.isNotEmpty()) {
            chipGroupFoodTags.isVisible = true
            val chipBg = VaznimColors.getSurfaceLow(context)
            val chipStroke = VaznimColors.getOutline(context)
            val chipText = VaznimColors.getOnSurface(context)

            filteredNutritionTags.forEach { tag ->
                val chip = Chip(context).apply {
                    text = tag.getTitle(context)
                    textSize = 12f
                    isClickable = false
                    isCheckable = false
                    chipBackgroundColor = ColorStateList.valueOf(chipBg)
                    chipStrokeColor = ColorStateList.valueOf(chipStroke)
                    chipStrokeWidth = 1f
                    setTextColor(chipText)
                }
                chipGroupFoodTags.addView(chip)
            }
        } else {
            chipGroupFoodTags.isVisible = false
        }

        // 3. Servings & Controls
        servings = foodItem.servingSizes.ifEmpty {
            listOf(ServingSize(getString(R.string.food_detail_serving_default), 100.0))
        }
        selectedServingSize = servings.first()
        customGrams = selectedServingSize?.grams ?: 100.0

        val adapter = ArrayAdapter(
            context,
            android.R.layout.simple_dropdown_item_1line,
            servings.map { it.formatDisplayName() }
        )
        autoCompleteServingSize.setAdapter(adapter)
        autoCompleteServingSize.setText(selectedServingSize?.formatDisplayName().orEmpty(), false)

        autoCompleteServingSize.setOnItemClickListener { _, _, position, _ ->
            selectedServingSize = servings.getOrNull(position) ?: servings.first()
            syncCustomGramsFromPortion()
            updateNutrition()
        }

        // Mode Toggle: Portion vs Grams (Getman segmented style)
        btnModePortion.setOnClickListener { switchMode(isGram = false) }
        btnModeGrams.setOnClickListener { switchMode(isGram = true) }

        // Portion Stepper
        buttonMinus.setOnClickListener {
            if (quantity > 0.5) {
                quantity = roundToOneDecimal(quantity - 0.5)
                updateQuantityText()
            }
        }

        buttonPlus.setOnClickListener {
            quantity = roundToOneDecimal(quantity + 0.5)
            updateQuantityText()
        }

        editTextQuantity.addTextChangedListener {
            val input = it?.toString()?.toDoubleOrNull().orZero()
            if (quantity != input && input > 0) {
                quantity = input
                syncCustomGramsFromPortion()
                updateNutrition()
            }
        }

        // Grams Stepper
        buttonMinusGrams.setOnClickListener {
            if (customGrams > 25.0) {
                customGrams = (customGrams - 25.0).coerceAtLeast(10.0)
                updateGramsText()
            }
        }

        buttonPlusGrams.setOnClickListener {
            customGrams += 25.0
            updateGramsText()
        }

        editTextGrams.addTextChangedListener {
            val input = it?.toString()?.toDoubleOrNull().orZero()
            if (customGrams != input && input > 0) {
                customGrams = input
                updateNutrition()
            }
        }

        // Quick Gram Chips
        val quickGramChips = listOf(
            chip50g to 50.0,
            chip100g to 100.0,
            chip150g to 150.0,
            chip200g to 200.0,
            chip250g to 250.0,
            chip300g to 300.0
        )
        quickGramChips.forEach { (chip, grams) ->
            chip.setOnClickListener { setGrams(grams) }
        }

        // Primary Log Button
        buttonLogFood.setOnClickListener {
            if (currentWeight <= 0.0) return@setOnClickListener
            if (isAdded && !isRemoving) {
                val log = createFoodLog()
                onLogAdded(log)
                dismiss()
            }
        }

        updateQuantityText()
        updateNutrition()
    }

    private fun ServingSize.formatDisplayName(): String =
        getString(R.string.food_detail_serving_item_format, description, grams.toInt())

    private fun roundToOneDecimal(value: Double): Double =
        (value * 10.0).roundToInt() / 10.0

    private fun syncCustomGramsFromPortion() {
        customGrams = (selectedServingSize?.grams ?: 100.0) * quantity
    }

    private fun switchMode(isGram: Boolean) {
        if (isGramMode == isGram) return
        isGramMode = isGram
        val context = requireContext()
        val onSurfaceColor = VaznimColors.getOnSurface(context)
        val variantColor = VaznimColors.getOnSurfaceVariant(context)

        binding.btnModePortion.apply {
            setBackgroundResource(if (!isGram) R.drawable.bg_segmented_selected else 0)
            setTextColor(if (!isGram) onSurfaceColor else variantColor)
        }
        binding.btnModeGrams.apply {
            setBackgroundResource(if (isGram) R.drawable.bg_segmented_selected else 0)
            setTextColor(if (isGram) onSurfaceColor else variantColor)
        }

        binding.layoutPortionContainer.isVisible = !isGram
        binding.layoutGramContainer.isVisible = isGram

        if (isGram) {
            updateGramsText()
            binding.editTextGrams.post {
                binding.editTextGrams.requestFocus()
                binding.editTextGrams.selectAll()
                binding.editTextGrams.showKeyboard()
            }
        } else {
            val sGrams = selectedServingSize?.grams ?: 100.0
            if (sGrams > 0) {
                val converted = customGrams / sGrams
                quantity = if (converted > 0) roundToOneDecimal(converted) else 1.0
                updateQuantityText()
            }
        }
        updateNutrition()
    }

    private fun setGrams(grams: Double) {
        customGrams = grams
        updateGramsText()
    }

    private fun updateQuantityText() {
        val text = quantity.readableFormat()
        if (binding.editTextQuantity.text?.toString() != text) {
            binding.editTextQuantity.setText(text)
            binding.editTextQuantity.setSelection(text.length)
        }
        syncCustomGramsFromPortion()
        updateNutrition()
    }

    private fun updateGramsText() {
        val text = customGrams.readableFormat()
        if (binding.editTextGrams.text?.toString() != text) {
            binding.editTextGrams.setText(text)
            binding.editTextGrams.setSelection(text.length)
        }
        updateNutrition()
    }

    private fun updateNutrition() = with(binding) {
        val weight = currentWeight
        textViewCaloriesDetail.text = NutritionFormatter.kcal(foodItem.caloriesForGrams(weight))
        textMacroProtein.text = NutritionFormatter.gr(foodItem.proteinForGrams(weight))
        textMacroFat.text = NutritionFormatter.gr(foodItem.fatForGrams(weight))
        textMacroCarbs.text = NutritionFormatter.gr(foodItem.carbsForGrams(weight))
        textMacroFiber.text = NutritionFormatter.gr(foodItem.fiberForGrams(weight))
        textMacroSugar.text = NutritionFormatter.gr(foodItem.sugarForGrams(weight))
    }

    private fun createFoodLog(): FoodLog {
        val weight = currentWeight
        val portionDescription = if (isGramMode) {
            NutritionFormatter.gr(customGrams)
        } else {
            val serving = selectedServingSize ?: servings.first()
            val displayQuantity = NutritionFormatter.portion(quantity)
            getString(R.string.food_detail_portion_quantity_format, displayQuantity, serving.description.lowercase())
        }

        return FoodLog(
            description = getString(R.string.food_detail_logged_description_format, foodItem.name, portionDescription),
            calories = foodItem.caloriesForGrams(weight),
            protein = foodItem.proteinForGrams(weight),
            fat = foodItem.fatForGrams(weight),
            carbs = foodItem.carbsForGrams(weight),
            mealType = mealType,
            aiGenerated = false,
            timestamp = System.currentTimeMillis()
        )
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        /**
         * Ko'rsatish shart bo'lmagan umumiy toifa / ovqat turi taglari.
         */
        private val EXCLUDED_FOOD_TAGS = setOf(
            FoodTag.NATIONAL_FOOD,
            FoodTag.FAST_FOOD,
            FoodTag.SOUP,
            FoodTag.MAIN_DISH,
            FoodTag.MEAT_POULTRY_FISH,
            FoodTag.SALAD_APPETIZER,
            FoodTag.DAIRY_EGG,
            FoodTag.BREAD_BAKERY,
            FoodTag.FRUITS_BERRIES,
            FoodTag.VEGETABLES_GREENS,
            FoodTag.NUTS_SEEDS,
            FoodTag.DRINKS,
            FoodTag.SWEETS_DESSERTS,
            FoodTag.SAUCES_OILS,
            FoodTag.BREAKFAST,
            FoodTag.LUNCH,
            FoodTag.DINNER,
            FoodTag.SNACK,
            FoodTag.HOME_COOKED,
            FoodTag.RAW_FRESH,
            FoodTag.QUICK_PREP,
            FoodTag.BLOOD_SUGAR_FRIENDLY
        )
    }
}