package uz.oybek.ozamiz.ui.feature.menu

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.navigation.fragment.findNavController
import uz.oybek.ozamiz.R
import uz.oybek.ozamiz.data.menu.CategoryType
import uz.oybek.ozamiz.data.menu.FilterId
import uz.oybek.ozamiz.databinding.FragmentPlateRuleBinding
import uz.oybek.ozamiz.ui.base.BaseFragment

class PlateRuleFragment : BaseFragment<FragmentPlateRuleBinding>() {

    init {
        showBottomSheet = false
    }

    override fun inflateBinding(
        inflater: LayoutInflater,
        container: ViewGroup?
    ) = FragmentPlateRuleBinding.inflate(inflater, container, false)

    override fun viewCreated(bind: FragmentPlateRuleBinding) {
        bind.btnBack.setOnClickListener {
            findNavController().navigateUp()
        }

        bind.btnViewVegRecipes.setOnClickListener {
            navigateToSection("VEGETABLES")
        }

        bind.btnViewProteinRecipes.setOnClickListener {
            navigateToSection("PROTEINS")
        }

        bind.btnViewCarbRecipes.setOnClickListener {
            navigateToSection("CARBS")
        }
    }

    private fun navigateToSection(sectionType: String) {
        val action = PlateRuleFragmentDirections.actionPlateRuleFragmentToPlateSectionDetailFragment(
            sectionType = sectionType
        )
        findNavController().navigate(action)
    }
}
