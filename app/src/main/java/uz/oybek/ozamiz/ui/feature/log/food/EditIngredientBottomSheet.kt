package uz.oybek.ozamiz.ui.feature.log.food

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import uz.oybek.ozamiz.common.util.orZero
import uz.oybek.ozamiz.common.util.readableFormat
import uz.oybek.ozamiz.data.analyzer.model.AnalyzedIngredient
import uz.oybek.ozamiz.databinding.DialogEditIngredientBinding
import uz.oybek.ozamiz.ui.utils.showKeyboard
import kotlin.math.roundToInt

class EditIngredientBottomSheet(
    private val args: EditIngredientArgs? = null,
    private val onSave: ((AnalyzedIngredient) -> Unit)? = null,
    private val onDelete: (() -> Unit)? = null
) : BottomSheetDialogFragment() {

    private var _binding: DialogEditIngredientBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DialogEditIngredientBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val item = args?.ingredient ?: return dismiss()

        binding.editTextIngredientName.setText(item.name)
        binding.editTextIngredientAmount.setText(item.amount.readableFormat(default = ""))
        binding.editTextIngredientCalories.setText(item.calories?.toString() ?: "")

        binding.buttonClose.setOnClickListener { dismiss() }

        val initialAmount = item.amount ?: 100.0
        val initialCalories = item.calories?.toDouble().orZero()
        val calPerUnit = if (initialAmount > 0) initialCalories / initialAmount else 0.0

        binding.editTextIngredientAmount.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: android.text.Editable?) {
                if (binding.editTextIngredientAmount.hasFocus()) {
                    val newAmount = s?.toString()?.toDoubleOrNull().orZero()
                    if (calPerUnit > 0) {
                        val newCal = (calPerUnit * newAmount).roundToInt()
                        binding.editTextIngredientCalories.setText(newCal.toString())
                    }
                }
            }
        })
        val textLength = binding.editTextIngredientAmount.text?.length.orZero()
        binding.editTextIngredientAmount.setSelection(textLength)
        binding.editTextIngredientAmount.showKeyboard(150)

        binding.buttonSave.setOnClickListener {
            val newName = binding.editTextIngredientName.text.toString().trim()
            val newAmount = binding.editTextIngredientAmount.text.toString().toDoubleOrNull()
            val newCalories = binding.editTextIngredientCalories.text.toString().toIntOrNull()
            val ratio = if (initialAmount > 0 && newAmount != null) newAmount / initialAmount else 1.0

            val updatedIngredient = item.copy(
                name = newName.ifEmpty { item.name },
                amount = newAmount,
                unit = "g",
                calories = newCalories,
                protein = item.protein?.let { it * ratio },
                fat = item.fat?.let { it * ratio },
                carbs = item.carbs?.let { it * ratio }
            )

            onSave?.invoke(updatedIngredient)
            dismiss()
        }

        binding.buttonDelete.setOnClickListener {
            onDelete?.invoke()
            dismiss()
        }
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)
            ?.setBackgroundResource(android.R.color.transparent)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}