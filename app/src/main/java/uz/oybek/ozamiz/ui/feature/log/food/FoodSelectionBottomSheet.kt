package uz.oybek.ozamiz.ui.feature.log.food

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.core.widget.addTextChangedListener
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import uz.oybek.ozamiz.common.util.orZero
import uz.oybek.ozamiz.common.util.readableFormat
import uz.oybek.ozamiz.R
import uz.oybek.ozamiz.data.food.model.FoodItem
import uz.oybek.ozamiz.data.food.model.ServingSize
import uz.oybek.ozamiz.data.logs.model.FoodLog
import uz.oybek.ozamiz.databinding.BottomSheetFoodSelectionDetailBinding
import uz.oybek.ozamiz.ui.modelUtils.NutritionFormatter
import uz.oybek.ozamiz.ui.utils.showKeyboard
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

    override fun getTheme(): Int = R.style.BottomSheetDialogTheme

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = BottomSheetFoodSelectionDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupUI()
    }

    private fun setupUI() = with(binding) {
        textViewFoodNameDetail.text = foodItem.name

        servings = foodItem.servingSizes.ifEmpty {
            listOf(ServingSize(getString(R.string.food_detail_serving_default), 100.0))
        }
        selectedServingSize = servings.first()
        customGrams = selectedServingSize?.grams ?: 100.0

        val adapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_dropdown_item_1line,
            servings.map {
                getString(R.string.food_detail_serving_item_format, it.description, it.grams.toInt())
            }
        )
        autoCompleteServingSize.setAdapter(adapter)
        val initialServing = selectedServingSize?.let {
            getString(R.string.food_detail_serving_item_format, it.description, it.grams.toInt())
        }.orEmpty()
        autoCompleteServingSize.setText(initialServing, false)

        autoCompleteServingSize.setOnItemClickListener { _, _, position, _ ->
            selectedServingSize = servings.getOrNull(position) ?: servings.first()
            customGrams = (selectedServingSize?.grams ?: 100.0) * quantity
            updateNutrition()
        }

        // Mode Toggle: Portion vs Grams
        toggleGroupUnitMode.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (isChecked) {
                if (checkedId == R.id.btn_mode_grams) {
                    isGramMode = true
                    layoutPortionContainer.visibility = View.GONE
                    layoutGramContainer.visibility = View.VISIBLE
                    updateGramsText()
                    editTextGrams.post {
                        editTextGrams.requestFocus()
                        editTextGrams.selectAll()
                        editTextGrams.showKeyboard()
                    }
                } else {
                    isGramMode = false
                    layoutGramContainer.visibility = View.GONE
                    layoutPortionContainer.visibility = View.VISIBLE
                    val sGrams = selectedServingSize?.grams ?: 100.0
                    if (sGrams > 0) {
                        val converted = customGrams / sGrams
                        quantity = if (converted > 0) ((converted * 10.0).roundToInt() / 10.0) else 1.0
                        updateQuantityText()
                    }
                }
                updateNutrition()
            }
        }

        // Portion Stepper
        buttonMinus.setOnClickListener {
            if (quantity > 0.5) {
                quantity = ((quantity - 0.5) * 10.0).roundToInt() / 10.0
                updateQuantityText()
            }
        }

        buttonPlus.setOnClickListener {
            quantity = ((quantity + 0.5) * 10.0).roundToInt() / 10.0
            updateQuantityText()
        }

        editTextQuantity.addTextChangedListener {
            val input = it?.toString()?.toDoubleOrNull().orZero()
            if (quantity != input && input > 0) {
                quantity = input
                customGrams = (selectedServingSize?.grams ?: 100.0) * quantity
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
        chip50g.setOnClickListener { setGrams(50.0) }
        chip100g.setOnClickListener { setGrams(100.0) }
        chip150g.setOnClickListener { setGrams(150.0) }
        chip200g.setOnClickListener { setGrams(200.0) }
        chip250g.setOnClickListener { setGrams(250.0) }
        chip300g.setOnClickListener { setGrams(300.0) }

        buttonLogFood.setOnClickListener {
            val currentWeight = if (isGramMode) customGrams else (selectedServingSize?.grams ?: 100.0) * quantity
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
        customGrams = (selectedServingSize?.grams ?: 100.0) * quantity
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

    private fun updateNutrition() {
        val weight = if (isGramMode) {
            customGrams
        } else {
            (selectedServingSize?.grams ?: 100.0) * quantity
        }

        val calories = foodItem.caloriesForGrams(weight)
        binding.textViewCaloriesDetail.text = NutritionFormatter.kcal(calories)
    }

    private fun createFoodLog(): FoodLog {
        val weight = if (isGramMode) {
            customGrams
        } else {
            (selectedServingSize?.grams ?: 100.0) * quantity
        }

        val calories = foodItem.caloriesForGrams(weight)
        val carbs = foodItem.carbsForGrams(weight)
        val protein = foodItem.proteinForGrams(weight)
        val fat = foodItem.fatForGrams(weight)

        val portionDescription = if (isGramMode) {
            NutritionFormatter.gr(customGrams)
        } else {
            val serving = selectedServingSize ?: servings.first()
            val displayQuantity = NutritionFormatter.portion(quantity)
            getString(R.string.food_detail_portion_quantity_format, displayQuantity, serving.description.lowercase())
        }

        return FoodLog(
            description = getString(R.string.food_detail_logged_description_format, foodItem.name, portionDescription),
            calories = calories,
            protein = protein,
            fat = fat,
            carbs = carbs,
            mealType = mealType,
            aiGenerated = false,
            timestamp = System.currentTimeMillis()
        )
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}