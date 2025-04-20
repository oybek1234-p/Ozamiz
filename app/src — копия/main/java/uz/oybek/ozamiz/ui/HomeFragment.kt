package uz.oybek.ozamiz.ui

import android.view.LayoutInflater
import android.view.ViewGroup
import uz.oybek.ozamiz.databinding.HomeFragmentBinding
import uz.oybek.ozamiz.ui.base.BaseFragment

class HomeFragment : BaseFragment<HomeFragmentBinding>() {

    init {
        showBottomSheet = true
    }

    override fun inflateBinding(
        inflater: LayoutInflater,
        container: ViewGroup?
    ): HomeFragmentBinding {
        return HomeFragmentBinding.inflate(layoutInflater)
    }

    private fun updateNutritionView() {

    }

    override fun viewCreated(bind: HomeFragmentBinding) {
        bind.apply {

            updateNutritionView()
        }
    }

}