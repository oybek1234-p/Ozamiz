package uz.oybek.ozamiz.ui.feature.menu

import android.view.LayoutInflater
import android.view.ViewGroup
import uz.oybek.ozamiz.R
import uz.oybek.ozamiz.data.menu.CategoryType
import uz.oybek.ozamiz.data.menu.FilterId
import uz.oybek.ozamiz.databinding.FragmentPlateRuleBinding
import uz.oybek.ozamiz.ui.base.BaseFragment
import uz.oybek.ozamiz.ui.utils.toBundle

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
            navigateUp()
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
        val args = PlateSectionDetailArgs(sectionType = sectionType)
        navigate(R.id.plateSectionDetailFragment, args.toBundle())
    }
}
