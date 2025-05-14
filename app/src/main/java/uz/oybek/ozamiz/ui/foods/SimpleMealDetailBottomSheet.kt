package uz.oybek.ozamiz.ui.foods

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.gson.Gson
import uz.oybek.ozamiz.R
import uz.oybek.ozamiz.data.foods.SimpleMealIdea
import uz.oybek.ozamiz.databinding.DialogSimpleMealDetailBinding
import uz.oybek.ozamiz.handleException
import uz.oybek.ozamiz.ui.loadPhoto
import uz.oybek.ozamiz.ui.showToast

class SimpleMealDetailBottomSheet : BottomSheetDialogFragment() {

    private var _binding: DialogSimpleMealDetailBinding? = null
    private val binding get() = _binding!!

    companion object {
        const val TAG = "SimpleMealDetailBottomSheet"
        const val ARG_MEAL_IDEA_JSON = "mealIdeaJson"

        fun newInstance(mealIdea: SimpleMealIdea): SimpleMealDetailBottomSheet {
            val fragment = SimpleMealDetailBottomSheet()
            val args = Bundle()
            args.putString(ARG_MEAL_IDEA_JSON, Gson().toJson(mealIdea))
            fragment.arguments = args
            return fragment
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DialogSimpleMealDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = super.onCreateDialog(savedInstanceState) as BottomSheetDialog
        dialog.setOnShowListener {
            val bottomSheet = dialog.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)
            if (bottomSheet != null) {
                val behavior = BottomSheetBehavior.from(bottomSheet)
                behavior.state = BottomSheetBehavior.STATE_EXPANDED
                behavior.skipCollapsed = true
            }
        }
        return dialog
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val mealIdeaJson = arguments?.getString(ARG_MEAL_IDEA_JSON)
        if (mealIdeaJson == null) {
            showToast("Ma'lumot topilmadi")
            dismiss()
            return
        }

        try {
            val mealIdea = Gson().fromJson(mealIdeaJson, SimpleMealIdea::class.java)
            displayMealIdea(mealIdea)
        } catch (e: Exception) {
            handleException(e, "SimpleMealJsonParseError")
            showToast("Ma'lumotni o'qishda xatolik")
            dismiss()
        }

        binding.buttonCloseDialog.setOnClickListener {
            dismiss()
        }
    }

    private fun displayMealIdea(mealIdea: SimpleMealIdea) {
        binding.textViewMealNameDialog.text = mealIdea.name
        binding.textViewMealDescriptionDialog.text = mealIdea.description ?: "Tavsif mavjud emas."
        binding.textViewMealDescriptionDialog.visibility = if (mealIdea.description.isNullOrBlank()) View.GONE else View.VISIBLE

        if (mealIdea.estimatedCalories != null && mealIdea.estimatedCalories!! > 0) {
            binding.textViewMealCaloriesDialog.text = getString(R.string.calories_format_approx, mealIdea.estimatedCalories)
            binding.textViewMealCaloriesDialog.visibility = View.VISIBLE
        } else {
            binding.textViewMealCaloriesDialog.visibility = View.GONE
        }

    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}