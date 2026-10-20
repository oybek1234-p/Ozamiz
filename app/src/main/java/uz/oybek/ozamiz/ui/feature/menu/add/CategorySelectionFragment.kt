package uz.oybek.ozamiz.ui.feature.menu.add

import android.view.LayoutInflater
import android.view.ViewGroup
import uz.oybek.ozamiz.databinding.FragmentCategorySelectBinding
import uz.oybek.ozamiz.ui.base.BaseFragment

class CategorySelectionFragment : BaseFragment<FragmentCategorySelectBinding>() {

    override fun inflateBinding(
        inflater: LayoutInflater,
        container: ViewGroup?
    ) = FragmentCategorySelectBinding.inflate(inflater, container, false)

    override fun viewCreated(bind: FragmentCategorySelectBinding) {
        bind.btnBack.setOnClickListener { popBackStack() }
        bind.btnDone.setOnClickListener { popBackStack() }
    }
}
