package uz.oybek.ozamiz.ui.feature.log.food

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.fragment.app.setFragmentResult
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.gson.Gson
import kotlin.math.roundToInt
import uz.oybek.ozamiz.data.ai.analyzer.AnalyzedIngredient
import uz.oybek.ozamiz.databinding.DialogEditIngredientBinding

class EditIngredientBottomSheet : BottomSheetDialogFragment() {

    private var _binding: DialogEditIngredientBinding? = null
    private val binding get() = _binding!!
    private var ingredient: AnalyzedIngredient? = null
    private var position: Int = -1

    companion object {
        const val TAG = "EditIngredientSheet"
        const val REQUEST_KEY = "EditIngredientResult"
        const val RESULT_INGREDIENT = "ingredient"
        const val RESULT_POSITION = "position"
        const val RESULT_ACTION = "action"
        const val ACTION_SAVE = "save"
        const val ACTION_DELETE = "delete"

        private const val ARG_INGREDIENT_JSON = "arg_ingredient"
        private const val ARG_POSITION = "arg_position"

        fun newInstance(ingredient: AnalyzedIngredient, position: Int): EditIngredientBottomSheet {
            return EditIngredientBottomSheet().apply {
                arguments = bundleOf(
                    ARG_INGREDIENT_JSON to Gson().toJson(ingredient),
                    ARG_POSITION to position
                )
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            val json = it.getString(ARG_INGREDIENT_JSON)
            ingredient = Gson().fromJson(json, AnalyzedIngredient::class.java)
            position = it.getInt(ARG_POSITION, -1)
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = DialogEditIngredientBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val item = ingredient ?: return dismiss()

        binding.editTextIngredientName.setText(item.name)
        binding.editTextIngredientAmount.setText(
            item.amount?.let { if (it % 1.0 == 0.0) it.toInt().toString() else it.toString() } ?: ""
        )
        binding.editTextIngredientCalories.setText(item.calories?.toString() ?: "")

        binding.buttonClose.setOnClickListener { dismiss() }

        val initialAmount = item.amount ?: 100.0
        val initialCalories = item.calories?.toDouble() ?: 0.0
        val calPerUnit = if (initialAmount > 0) initialCalories / initialAmount else 0.0

        binding.editTextIngredientAmount.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: android.text.Editable?) {
                if (binding.editTextIngredientAmount.hasFocus()) {
                    val newAmount = s?.toString()?.toDoubleOrNull() ?: 0.0
                    if (calPerUnit > 0) {
                        val newCal = (calPerUnit * newAmount).roundToInt()
                        binding.editTextIngredientCalories.setText(newCal.toString())
                    }
                }
            }
        })
        binding.editTextIngredientAmount.postDelayed({
            if (isAdded && _binding != null) {
                binding.editTextIngredientAmount.requestFocus()
                val textLength = binding.editTextIngredientAmount.text?.length ?: 0
                binding.editTextIngredientAmount.setSelection(textLength)
                val imm = requireContext().getSystemService(android.content.Context.INPUT_METHOD_SERVICE) as? android.view.inputmethod.InputMethodManager
                imm?.showSoftInput(binding.editTextIngredientAmount, android.view.inputmethod.InputMethodManager.SHOW_IMPLICIT)
            }
        }, 150)

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

            setFragmentResult(REQUEST_KEY, bundleOf(
                RESULT_ACTION to ACTION_SAVE,
                RESULT_INGREDIENT to Gson().toJson(updatedIngredient),
                RESULT_POSITION to position
            ))
            dismiss()
        }

        binding.buttonDelete.setOnClickListener {
            setFragmentResult(REQUEST_KEY, bundleOf(
                RESULT_ACTION to ACTION_DELETE,
                RESULT_POSITION to position
            ))
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