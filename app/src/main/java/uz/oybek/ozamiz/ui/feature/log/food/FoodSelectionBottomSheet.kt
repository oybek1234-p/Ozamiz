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
import java.util.Locale
import kotlin.math.roundToInt

class FoodSelectionBottomSheet(
    private val foodItem: FoodItem,
    private val mealType: String,
    private val onLogAdded: (FoodLog) -> Unit
) : BottomSheetDialogFragment() {

    private var _binding: BottomSheetFoodSelectionDetailBinding? = null
    private val binding get() = _binding!!

    private var selectedServingSize: ServingSize? = null
    private var quantity: Double = 1.0

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

        val servings = foodItem.servingSizes.ifEmpty {
            listOf(ServingSize("Gramm", 1.0), ServingSize("100g", 100.0))
        }
        selectedServingSize = servings.first()

        val adapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_dropdown_item_1line,
            servings.map { it.description }
        )
        binding.autoCompleteServingSize.setAdapter(adapter)
        binding.autoCompleteServingSize.setText(selectedServingSize?.description, false)

        binding.autoCompleteServingSize.setOnItemClickListener { _, _, position, _ ->
            selectedServingSize = servings[position]
            updateNutrition()
        }

        binding.editTextQuantity.addTextChangedListener {
            val input = it.toString().toDoubleOrNull() ?: 0.0
            if (quantity != input) {
                quantity = input
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

        binding.buttonLogFood.setOnClickListener {
            if (quantity <= 0.0) return@setOnClickListener
            val log = createFoodLog()
            onLogAdded(log)
            dismiss()
        }

        updateNutrition()
    }

    private fun updateQuantityText() {
        val text = if (quantity % 1.0 == 0.0) quantity.toInt().toString() else quantity.toString()
        binding.editTextQuantity.setText(text)
        binding.editTextQuantity.setSelection(text.length)
        updateNutrition()
    }

    private fun updateNutrition() {
        val serving = selectedServingSize ?: return
        val weight = serving.grams * quantity

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
        val serving = selectedServingSize!!
        val weight = serving.grams * quantity

        val calories = (foodItem.caloriesPer100g * weight / 100).roundToInt()
        val carbs = (foodItem.carbsPer100g * weight / 100)
        val protein = (foodItem.proteinPer100g * weight / 100)
        val fat = (foodItem.fatPer100g * weight / 100)

        val displayQuantity = if (quantity % 1.0 == 0.0) quantity.toInt().toString() else quantity.toString()

        return FoodLog(
            description = "${foodItem.name} ($displayQuantity ${serving.description})",
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