package uz.oybek.ozamiz.ui.feature.menu

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.navigation.fragment.findNavController
import uz.oybek.ozamiz.databinding.FragmentBreakfastGuideBinding
import uz.oybek.ozamiz.ui.base.BaseFragment

class BreakfastGuideFragment : BaseFragment<FragmentBreakfastGuideBinding>() {

    override fun inflateBinding(
        inflater: LayoutInflater,
        container: ViewGroup?
    ) = FragmentBreakfastGuideBinding.inflate(inflater, container, false)

    override fun viewCreated(bind: FragmentBreakfastGuideBinding) {
        bind.btnBack.setOnClickListener {
            findNavController().navigateUp()
        }
    }
}
