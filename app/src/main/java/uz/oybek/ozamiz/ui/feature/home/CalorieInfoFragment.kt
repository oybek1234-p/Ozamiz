package uz.oybek.ozamiz.ui.feature.home

import android.view.LayoutInflater
import android.view.ViewGroup
import uz.oybek.ozamiz.databinding.FragmentCalorieInfoBinding
import uz.oybek.ozamiz.ui.base.BaseFragment

class CalorieInfoFragment : BaseFragment<FragmentCalorieInfoBinding>() {

    override fun inflateBinding(
        inflater: LayoutInflater,
        container: ViewGroup?
    ): FragmentCalorieInfoBinding {
        return FragmentCalorieInfoBinding.inflate(inflater, container, false)
    }

    override fun viewCreated(bind: FragmentCalorieInfoBinding) {
        setupToolbar(bind)
        setupButtons(bind)
    }

    private fun setupToolbar(bind: FragmentCalorieInfoBinding) {
        bind.toolbar.setNavigationOnClickListener {
            closeFragment()
        }
    }

    private fun setupButtons(bind: FragmentCalorieInfoBinding) {
        bind.btnGotIt.setOnClickListener {
            closeFragment()
        }
    }
}
