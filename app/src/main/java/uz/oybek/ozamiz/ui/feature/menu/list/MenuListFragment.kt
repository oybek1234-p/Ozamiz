package uz.oybek.ozamiz.ui.feature.menu.list

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import uz.oybek.ozamiz.R
import uz.oybek.ozamiz.databinding.FragmentMenuListBinding
import uz.oybek.ozamiz.ui.base.BaseFragment

class MenuListFragment : BaseFragment<FragmentMenuListBinding>() {

    init {
        showBottomSheet = true
    }

    override fun inflateBinding(
        inflater: LayoutInflater,
        container: ViewGroup?
    ) = FragmentMenuListBinding.inflate(inflater, container, false)

    override fun viewCreated(bind: FragmentMenuListBinding) {
        setupListeners(bind)
    }

    private fun setupListeners(bind: FragmentMenuListBinding) {
        bind.itemBreakfast.setOnClickListener {
            navigate(R.id.action_menuListFragment_to_breakfastGuideFragment)
        }
        bind.itemLunch.setOnClickListener {
            navigate(R.id.action_menuListFragment_to_lunchGuideFragment)
        }
        bind.itemDinner.setOnClickListener {
            navigate(R.id.action_menuListFragment_to_dinnerGuideFragment)
        }
        bind.itemSnacks.setOnClickListener {
            navigate(R.id.action_menuListFragment_to_snacksGuideFragment)
        }
        bind.itemProtein.setOnClickListener {
            navigate(R.id.action_menuListFragment_to_proteinGuideFragment)
        }

        bind.itemCarbs.setOnClickListener {
            navigate(R.id.action_menuListFragment_to_carbsGuideFragment)
        }
        bind.itemLipids.setOnClickListener {
            navigate(R.id.action_menuListFragment_to_lipidsGuideFragment)
        }
    }
}
