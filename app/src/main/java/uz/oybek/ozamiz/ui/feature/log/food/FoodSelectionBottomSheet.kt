package uz.oybek.ozamiz.ui.feature.log.food

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.core.widget.addTextChangedListener
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import uz.oybek.ozamiz.data.food.model.FoodItem
import uz.oybek.ozamiz.data.food.model.ServingSize
import uz.oybek.ozamiz.data.logs.model.FoodLog
import uz.oybek.ozamiz.databinding.BottomSheetFoodSelectionDetailBinding
import uz.oybek.ozamiz.ui.utils.showKeyboard
import java.util.Locale
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

    private fun setupUI() {
        binding.textViewFoodNameDetail.text = foodItem.name

        servings = foodItem.servingSizes.ifEmpty {
            listOf(ServingSize("Porsiya", 100.0))
        }
        selectedServingSize = servings.first()
        customGrams = selectedServingSize?.grams ?: 100.0

        val adapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_dropdown_item_1line,
            servings.map { it.description }
        )
        binding.autoCompleteServingSize.setAdapter(adapter)
        binding.autoCompleteServingSize.setText(selectedServingSize?.description, false)

        binding.autoCompleteServingSize.setOnItemClickListener { _, _, position, _ ->
            selectedServingSize = servings[position]
            customGrams = (selectedServingSize?.grams ?: 100.0) * quantity
            updateNutrition()
        }

        binding.editTextQuantity.addTextChangedListener {
            val input = it.toString().toDoubleOrNull() ?: 0.0
            if (quantity != input) {
                quantity = input
                customGrams = (selectedServingSize?.grams ?: 100.0) * quantity
                updateNutrition()
            }
        }

        binding.editTextGrams.addTextChangedListener {
            val input = it.toString().toDoubleOrNull() ?: 0.0
            if (customGrams != input) {
                customGrams = input
                updateNutrition()
            }
        }

        // Plus (+) tugmasi
        binding.buttonPlus.setOnClickListener {
            quantity += 1.0
            updateQuantityText()
        }

        // Minus (-) tugmasi
        binding.buttonMinus.setOnClickListener {
            if (quantity > 1.0) {
                quantity -= 1.0
                updateQuantityText()
            }
        }

        // Gramni kiritish / Porsiya bo'yicha rejim almashtirish
        binding.buttonToggleMode.setOnClickListener {
            if (!isGramMode) {
                // Gram rejimiga o'tish
                isGramMode = true
                binding.layoutPortionContainer.visibility = View.GONE
                binding.layoutGramContainer.visibility = View.VISIBLE
                binding.buttonToggleMode.text = "Porsiya bo'yicha tanlash"

                val gramsText = if (customGrams % 1.0 == 0.0) customGrams.toInt().toString() else customGrams.toString()
                binding.editTextGrams.setText(gramsText)
                binding.editTextGrams.setSelection(gramsText.length)
                binding.editTextGrams.requestFocus()
                binding.editTextGrams.showKeyboard()
            } else {
                // Porsiya rejimiga qaytish
                isGramMode = false
                binding.layoutGramContainer.visibility = View.GONE
                binding.layoutPortionContainer.visibility = View.VISIBLE
                binding.buttonToggleMode.text = "Gramni kiritish"

                val sGrams = selectedServingSize?.grams ?: 100.0
                if (sGrams > 0) {
                    val converted = customGrams / sGrams
                    quantity = if (converted > 0) (Math.round(converted * 10.0) / 10.0) else 1.0
                    updateQuantityText()
                }
            }
            updateNutrition()
        }

        binding.buttonLogFood.setOnClickListener {
            val currentWeight = if (isGramMode) customGrams else (selectedServingSize?.grams ?: 100.0) * quantity
            if (currentWeight <= 0.0) return@setOnClickListener
            if (isAdded && !isRemoving) {
                val log = createFoodLog()
                onLogAdded(log)
                dismiss()
            }
        }

        updateNutrition()
    }

    private fun updateQuantityText() {
        val text = if (quantity % 1.0 == 0.0) quantity.toInt().toString() else quantity.toString()
        binding.editTextQuantity.setText(text)
        binding.editTextQuantity.setSelection(text.length)
        customGrams = (selectedServingSize?.grams ?: 100.0) * quantity
        updateNutrition()
    }

    private fun updateNutrition() {
        val weight = if (isGramMode) {
            customGrams
        } else {
            (selectedServingSize?.grams ?: 100.0) * quantity
        }

        val calories = (foodItem.caloriesPer100g * weight / 100).roundToInt()
        val carbs = (foodItem.carbsPer100g * weight / 100)
        val protein = (foodItem.proteinPer100g * weight / 100)
        val fat = (foodItem.fatPer100g * weight / 100)

        binding.textViewCaloriesDetail.text = "$calories kkal"

        binding.rowCalories.textViewNutrientLabel.text = "Kaloriya"
        binding.rowCalories.textViewNutrientValue.text = "$calories kkal"

        binding.rowCarbs.textViewNutrientLabel.text = "Uglevod"
        binding.rowCarbs.textViewNutrientValue.text = String.format(Locale.US, "%.1f g", carbs)

        binding.rowProtein.textViewNutrientLabel.text = "Oqsil"
        binding.rowProtein.textViewNutrientValue.text = String.format(Locale.US, "%.1f g", protein)

        binding.rowFat.textViewNutrientLabel.text = "Yog'"
        binding.rowFat.textViewNutrientValue.text = String.format(Locale.US, "%.1f g", fat)
    }

    private fun createFoodLog(): FoodLog {
        val weight = if (isGramMode) {
            customGrams
        } else {
            (selectedServingSize?.grams ?: 100.0) * quantity
        }

        val calories = (foodItem.caloriesPer100g * weight / 100).roundToInt()
        val carbs = (foodItem.carbsPer100g * weight / 100)
        val protein = (foodItem.proteinPer100g * weight / 100)
        val fat = (foodItem.fatPer100g * weight / 100)

        val portionDescription = if (isGramMode) {
            val gramsFormatted = if (customGrams % 1.0 == 0.0) customGrams.toInt().toString() else customGrams.toString()
            "${gramsFormatted} g"
        } else {
            val serving = selectedServingSize ?: servings.first()
            val displayQuantity = if (quantity % 1.0 == 0.0) quantity.toInt().toString() else quantity.toString()
            "$displayQuantity ${serving.description.lowercase()}"
        }

        return FoodLog(
            description = "${foodItem.name} ($portionDescription)",
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

    companion object {
        const val TAG = "FoodSelectionBottomSheet"
    }
}