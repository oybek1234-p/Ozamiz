package uz.oybek.ozamiz.ui.feature.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import uz.oybek.ozamiz.databinding.BottomSheetMealSelectionBinding

class MealSelectionBottomSheet : BottomSheetDialogFragment() {

    private var _binding: BottomSheetMealSelectionBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = BottomSheetMealSelectionBinding.inflate(inflater, container, false)
        return binding.root
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnBreakfast.setOnClickListener {
            navigateToChat("Nonushtaga nima yesam bo'ladi?")
        }

        binding.btnLunch.setOnClickListener {
            navigateToChat("Tushlikka nima yesam bo'ladi?")
        }

        binding.btnDinner.setOnClickListener {
            navigateToChat("Kechki ovqatga nima yesam bo'ladi?")
        }

        binding.btnSnack.setOnClickListener {
            navigateToChat("Snek yoki choyga nima yesam bo'ladi?")
        }

        binding.btnFullDay.setOnClickListener {
            navigateToChat("Bir kunlik menyuga nima yesam bo'ladi?")
        }
    }

    private fun navigateToChat(prompt: String) {
        val bundle = Bundle().apply {
            putString("initialPrompt", prompt)
        }
        parentFragmentManager.setFragmentResult("navigate_to_ai_chat", bundle)
        dismiss()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        const val TAG = "MealSelectionBottomSheet"
        fun newInstance() = MealSelectionBottomSheet()
    }
}
