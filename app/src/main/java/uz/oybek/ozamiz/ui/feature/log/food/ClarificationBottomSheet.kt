package uz.oybek.ozamiz.ui.feature.log.food

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.chip.Chip
import uz.oybek.ozamiz.R
import uz.oybek.ozamiz.databinding.BottomSheetClarificationBinding

class ClarificationBottomSheet(
    private val question: String,
    private val options: List<String>,
    private val onOptionSelected: (String) -> Unit
) : BottomSheetDialogFragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val binding = BottomSheetClarificationBinding.inflate(inflater, container, false)
        
        binding.textQuestion.text = question
        
        options.forEach { option ->
            val chip = inflater.inflate(
                R.layout.item_meal_type_selectable, 
                binding.chipGroupOptions, 
                false
            ) as Chip
            
            chip.text = option
            chip.setOnClickListener {
                onOptionSelected(option)
                dismiss() // Close sheet after selection
            }
            binding.chipGroupOptions.addView(chip)
        }
        
        return binding.root
    }
}
