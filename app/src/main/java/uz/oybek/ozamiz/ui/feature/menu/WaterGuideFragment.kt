package uz.oybek.ozamiz.ui.feature.menu

import android.view.LayoutInflater
import android.view.ViewGroup
import dagger.hilt.android.AndroidEntryPoint
import uz.oybek.ozamiz.databinding.FragmentWaterGuideBinding
import uz.oybek.ozamiz.ui.base.BaseFragment

@AndroidEntryPoint
class WaterGuideFragment : BaseFragment<FragmentWaterGuideBinding>() {

    override fun inflateBinding(
        inflater: LayoutInflater,
        container: ViewGroup?
    ) = FragmentWaterGuideBinding.inflate(inflater, container, false)

    override fun viewCreated(bind: FragmentWaterGuideBinding) {
        bind.btnBack.setOnClickListener {
            navigateUp()
        }
    }
}
