package uz.oybek.ozamiz.ui.diet

import android.view.LayoutInflater
import android.view.ViewGroup
import uz.oybek.ozamiz.data.diet.DietController
import uz.oybek.ozamiz.databinding.MealAddFragmentBinding
import uz.oybek.ozamiz.ui.base.BaseFragment
import uz.oybek.ozamiz.ui.showToast

class MealAddFragment : BaseFragment<MealAddFragmentBinding>() {
    override fun viewCreated(bind: MealAddFragmentBinding) {
        bind.apply {
            toolbar.setUpBackButton(this@MealAddFragment)

            button.setOnClickListener {
                val name = nameView.editText?.text.toString()
                val description = descriptionView.editText?.text.toString()
                val calories = caloriesView.editText?.text.toString()
                val carbs = carbsView.editText?.text.toString()
                val protein = proteinView.editText?.text.toString()
                val yog = yogView.editText?.text.toString()
                if (name.isEmpty() || description.isEmpty() || calories.isEmpty() || carbs.isEmpty() || protein.isEmpty() || yog.isEmpty()) {
                    return@setOnClickListener
                }
                DietController.addMeal(
                    name, description, calories.toInt(), carbs.toInt(), protein.toInt(), yog.toInt()
                ) {
                    showToast("Qo'shildi")
                }
                closeFragment()
            }
            photoView.setOnClickListener {

            }
        }
    }

    override fun inflateBinding(
        inflater: LayoutInflater, container: ViewGroup?
    ): MealAddFragmentBinding {
        return MealAddFragmentBinding.inflate(inflater)
    }
}