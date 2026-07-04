package uz.oybek.ozamiz.ui.feature.menu

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.navigation.fragment.findNavController
import uz.oybek.ozamiz.databinding.FragmentDinnerGuideBinding
import uz.oybek.ozamiz.ui.base.BaseFragment

class DinnerGuideFragment : BaseFragment<FragmentDinnerGuideBinding>() {

    override fun inflateBinding(
        inflater: LayoutInflater,
        container: ViewGroup?
    ) = FragmentDinnerGuideBinding.inflate(inflater, container, false)

    override fun viewCreated(bind: FragmentDinnerGuideBinding) {
        bind.btnBack.setOnClickListener {
            findNavController().navigateUp()
        }
    }
}
