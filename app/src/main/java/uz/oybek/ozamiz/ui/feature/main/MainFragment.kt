package uz.oybek.ozamiz.ui.feature.main

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import uz.oybek.ozamiz.databinding.MainFragmentBinding
import uz.oybek.ozamiz.ui.base.BaseFragment
import uz.oybek.ozamiz.ui.feature.log.food.LogFoodFragment

class MainFragment : BaseFragment<MainFragmentBinding>() {

    init {
        showBottomSheet = true
    }

    override fun inflateBinding(
        inflater: LayoutInflater,
        container: ViewGroup?
    ): MainFragmentBinding {
        return MainFragmentBinding.inflate(inflater, container, false)
    }

    override fun viewCreated(bind: MainFragmentBinding) {
        val adapter = MealCategoryAdapter { mealTime ->
            LogFoodFragment.navigate(
                navController = findNavController(),
                meal = mealTime.id
            )
        }
        bind.categories.adapter = adapter
        bind.categories.layoutManager =
            LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)

        bind.btnOpenMenus.setOnClickListener {
            findNavController().navigate(uz.oybek.ozamiz.R.id.action_mainFragment_to_menuListFragment)
        }
    }
}
