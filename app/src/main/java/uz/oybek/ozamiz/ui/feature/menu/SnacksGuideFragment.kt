package uz.oybek.ozamiz.ui.feature.menu

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.navigation.fragment.findNavController
import uz.oybek.ozamiz.databinding.FragmentSnacksGuideBinding
import uz.oybek.ozamiz.ui.base.BaseFragment

class SnacksGuideFragment : BaseFragment<FragmentSnacksGuideBinding>() {

    override fun inflateBinding(
        inflater: LayoutInflater,
        container: ViewGroup?
    ) = FragmentSnacksGuideBinding.inflate(inflater, container, false)

    override fun viewCreated(bind: FragmentSnacksGuideBinding) {
        bind.btnBack.setOnClickListener {
            findNavController().navigateUp()
        }
    }
}
