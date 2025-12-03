package uz.oybek.ozamiz.ui.feature.natija

import android.view.LayoutInflater
import android.view.ViewGroup
import uz.oybek.ozamiz.databinding.NatijaFragmentBinding
import uz.oybek.ozamiz.ui.base.BaseFragment

class NatijaFragment : BaseFragment<NatijaFragmentBinding>() {

    init {
        showBottomSheet = true
    }

    override fun viewCreated(bind: NatijaFragmentBinding) {

    }

    override fun inflateBinding(
        inflater: LayoutInflater, container: ViewGroup?
    ): NatijaFragmentBinding {
        return NatijaFragmentBinding.inflate(inflater, container, false)
    }
}