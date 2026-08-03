package uz.oybek.ozamiz.ui.feature.log.food

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.fragment.app.setFragmentResult
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.gson.Gson
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
        binding.editTextIngredientUnit.setText(item.unit ?: "")
        binding.editTextIngredientCalories.setText(item.calories?.toString() ?: "")

        binding.buttonSave.setOnClickListener {
            val newName = binding.editTextIngredientName.text.toString().trim()
            val newAmount = binding.editTextIngredientAmount.text.toString().toDoubleOrNull()
            val newUnit = binding.editTextIngredientUnit.text.toString().trim()
            val newCalories = binding.editTextIngredientCalories.text.toString().toIntOrNull()

            val updatedIngredient = item.copy(
                name = newName.ifEmpty { item.name },
                amount = newAmount,
                unit = newUnit.ifEmpty { null },
                calories = newCalories
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

        binding.buttonCancel.setOnClickListener { dismiss() }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}