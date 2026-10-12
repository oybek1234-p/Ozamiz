package uz.oybek.ozamiz.ui.feature.log.food

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import uz.oybek.ozamiz.R
import uz.oybek.ozamiz.data.analyzer.api.FoodAnalyzerRepo
import uz.oybek.ozamiz.data.analyzer.model.AnalyzedIngredient
import uz.oybek.ozamiz.databinding.DialogAddIngredientBinding
import uz.oybek.ozamiz.ui.utils.hideKeyboard
import uz.oybek.ozamiz.ui.utils.showKeyboard
import uz.oybek.ozamiz.ui.utils.showToast
import javax.inject.Inject

@AndroidEntryPoint
class AddIngredientBottomSheet(
    private val onIngredientAdded: ((AnalyzedIngredient) -> Unit)? = null
) : BottomSheetDialogFragment() {

    constructor() : this(null)

    @Inject
    lateinit var foodAnalyzerRepo: FoodAnalyzerRepo

    private var _binding: DialogAddIngredientBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DialogAddIngredientBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.buttonClose.setOnClickListener { dismiss() }
        binding.editTextIngredientInput.showKeyboard(150)

        binding.buttonSave.setOnClickListener {
            val input = binding.editTextIngredientInput.text.toString().trim()
            if (input.isBlank()) {
                showToast(getString(R.string.analyze_add_ingredient_empty_error), requireContext())
                return@setOnClickListener
            }

            binding.root.hideKeyboard()

            binding.layoutInputContainer.visibility = View.GONE
            binding.layoutLoadingContainer.visibility = View.VISIBLE
            isCancelable = false

            lifecycleScope.launch {
                val ingredient = foodAnalyzerRepo.analyzeIngredient(input)
                if (_binding != null && isAdded) {
                    if (ingredient != null) {
                        onIngredientAdded?.invoke(ingredient)
                        dismiss()
                    } else {
                        Toast.makeText(
                            requireContext(),
                            getString(R.string.analyze_add_ingredient_not_found),
                            Toast.LENGTH_SHORT
                        ).show()
                        binding.layoutLoadingContainer.visibility = View.GONE
                        binding.layoutInputContainer.visibility = View.VISIBLE
                        isCancelable = true
                    }
                }
            }
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
