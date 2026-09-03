package uz.oybek.ozamiz.ui.feature.log.food

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.os.bundleOf
import androidx.fragment.app.setFragmentResult
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import uz.oybek.ozamiz.databinding.DialogAddIngredientBinding

import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog

import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import uz.oybek.ozamiz.data.ai.analyzer.FoodAnalyzer
import uz.oybek.ozamiz.data.utils.gson

class AddIngredientBottomSheet : BottomSheetDialogFragment() {

    private var _binding: DialogAddIngredientBinding? = null
    private val binding get() = _binding!!

    companion object {
        const val TAG = "AddIngredientSheet"
        const val REQUEST_KEY = "AddIngredientResult"
        const val RESULT_INPUT = "input"
        const val RESULT_SINGLE_JSON = "single_json"
    }

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

        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val imeInset = insets.getInsets(WindowInsetsCompat.Type.ime())
            v.setPadding(v.paddingLeft, v.paddingTop, v.paddingRight, imeInset.bottom + 20)
            insets
        }

        binding.editTextIngredientInput.postDelayed({
            if (isAdded && _binding != null) {
                binding.editTextIngredientInput.requestFocus()
                val imm = requireContext().getSystemService(android.content.Context.INPUT_METHOD_SERVICE) as? android.view.inputmethod.InputMethodManager
                imm?.showSoftInput(binding.editTextIngredientInput, android.view.inputmethod.InputMethodManager.SHOW_IMPLICIT)
            }
        }, 150)

        binding.buttonSave.setOnClickListener {
            val input = binding.editTextIngredientInput.text.toString().trim()
            if (input.isBlank()) {
                Toast.makeText(requireContext(), "Iltimos, mahsulot nomini yozing", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Hide keyboard
            val imm = requireContext().getSystemService(android.content.Context.INPUT_METHOD_SERVICE) as? android.view.inputmethod.InputMethodManager
            imm?.hideSoftInputFromWindow(binding.root.windowToken, 0)

            // Switch to loading state
            binding.layoutInputContainer.visibility = View.GONE
            binding.layoutLoadingContainer.visibility = View.VISIBLE
            isCancelable = false

            lifecycleScope.launch {
                val singleResult = withContext(Dispatchers.IO) {
                    FoodAnalyzer.analyzeSingleIngredient(input)
                }
                if (_binding != null && isAdded) {
                    val singleJson = singleResult?.let { gson?.toJson(it) }
                    setFragmentResult(REQUEST_KEY, bundleOf(
                        RESULT_INPUT to input,
                        RESULT_SINGLE_JSON to singleJson
                    ))
                    dismiss()
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
