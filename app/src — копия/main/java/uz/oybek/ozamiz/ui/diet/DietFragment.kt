package uz.oybek.ozamiz.ui.diet

import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import uz.oybek.ozamiz.R
import uz.oybek.ozamiz.data.diet.DietController
import uz.oybek.ozamiz.data.diet.MealTypes
import uz.oybek.ozamiz.databinding.DietFragmentBinding
import uz.oybek.ozamiz.databinding.FoodFeedFragmentBinding
import uz.oybek.ozamiz.ui.base.BaseFragment

class DietFragment : BaseFragment<FoodFeedFragmentBinding>() {
    
    init {
        showBottomSheet = true
    }

    override fun viewCreated(bind: FoodFeedFragmentBinding) {
        bind.apply {
            breakfast.setOnClickListener {
                openMeals(MealTypes.BREAKFAST)
            }
            lunch.setOnClickListener {
                openMeals(MealTypes.LUNCH)
            }
            dinner.setOnClickListener {
                openMeals(MealTypes.DINNER)
            }
            snack.setOnClickListener {
                openMeals(MealTypes.SNACK)
            }
        }
    }

    private fun openMeals(mealType: String) {
        val bundle = Bundle()
        bundle.putString("mealType", mealType)
        navigate(R.id.mealsFragment, bundle)
    }

    override fun inflateBinding(
        inflater: LayoutInflater, container: ViewGroup?
    ): FoodFeedFragmentBinding {
        return FoodFeedFragmentBinding.inflate(inflater)
    }
}