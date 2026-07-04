package uz.oybek.ozamiz.ui.feature.menu

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.navigation.fragment.findNavController
import uz.oybek.ozamiz.R
import uz.oybek.ozamiz.databinding.FragmentPlateRuleBinding
import uz.oybek.ozamiz.ui.base.BaseFragment

class PlateRuleFragment : BaseFragment<FragmentPlateRuleBinding>() {

    override fun inflateBinding(
        inflater: LayoutInflater,
        container: ViewGroup?
    ) = FragmentPlateRuleBinding.inflate(inflater, container, false)

    override fun viewCreated(bind: FragmentPlateRuleBinding) {
        bind.btnBack.setOnClickListener {
            findNavController().navigateUp()
        }
        
        // Content is static in the layout
    }
}
