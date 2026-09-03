package uz.oybek.ozamiz.ui.feature.menu

import android.view.LayoutInflater
import android.view.ViewGroup
import uz.oybek.ozamiz.databinding.FragmentProteinGuideBinding
import uz.oybek.ozamiz.ui.base.BaseFragment

class ProteinGuideFragment : BaseFragment<FragmentProteinGuideBinding>() {

    override fun inflateBinding(
        inflater: LayoutInflater,
        container: ViewGroup?
    ) = FragmentProteinGuideBinding.inflate(inflater, container, false)

    override fun viewCreated(bind: FragmentProteinGuideBinding) {
        bind.btnBack.setOnClickListener {
            navigateUp()
        }
    }
}
