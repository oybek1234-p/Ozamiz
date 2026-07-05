package uz.oybek.ozamiz.ui.feature.menu

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.navigation.fragment.findNavController
import uz.oybek.ozamiz.databinding.FragmentLipidsGuideBinding
import uz.oybek.ozamiz.ui.base.BaseFragment

class LipidsGuideFragment : BaseFragment<FragmentLipidsGuideBinding>() {

    override fun inflateBinding(
        inflater: LayoutInflater,
        container: ViewGroup?
    ) = FragmentLipidsGuideBinding.inflate(inflater, container, false)

    override fun viewCreated(bind: FragmentLipidsGuideBinding) {
        bind.btnBack.setOnClickListener {
            findNavController().navigateUp()
        }
    }
}
