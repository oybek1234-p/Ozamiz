package uz.oybek.ozamiz.ui.feature.menu

import android.view.LayoutInflater
import android.view.ViewGroup
import uz.oybek.ozamiz.databinding.FragmentLunchGuideBinding
import uz.oybek.ozamiz.ui.base.BaseFragment

class LunchGuideFragment : BaseFragment<FragmentLunchGuideBinding>() {

    override fun inflateBinding(
        inflater: LayoutInflater,
        container: ViewGroup?
    ) = FragmentLunchGuideBinding.inflate(inflater, container, false)

    override fun viewCreated(bind: FragmentLunchGuideBinding) {
        bind.btnBack.setOnClickListener {
            navigateUp()
        }
    }
}
