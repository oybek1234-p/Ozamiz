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

        // Hozirgi qiymatlarni qo'yish
        binding.editTextIngredientName.setText(item.name)
        binding.editTextIngredientCalories.setText(item.calories?.toString() ?: "")
        
        // Agar points va unit bo'lsa, ularni nomiga qo'shib ko'rsatish yoki alohida (dizaynga qarab)
        // Hozircha name o'zida hammasini tahrirlashga ruxsat beramiz
        
        binding.buttonSave.setOnClickListener {
            val newName = binding.editTextIngredientName.text.toString()
            val newCalories = binding.editTextIngredientCalories.text.toString().toIntOrNull()

            val updatedIngredient = item.copy(
                name = newName,
                calories = newCalories
                // Amount va unitni name ichidan ajratib olish logikasi murakkab bo'lishi mumkin,
                // shuning uchun AI qayta analiz qilganda o'zi to'g'irlaydi deb hisoblaymiz.
            )

            setFragmentResult(REQUEST_KEY, bundleOf(
                RESULT_INGREDIENT to Gson().toJson(updatedIngredient),
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