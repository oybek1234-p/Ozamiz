package uz.oybek.ozamiz.ui.feature.menu.list

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.navigation.fragment.findNavController
import uz.oybek.ozamiz.R
import uz.oybek.ozamiz.data.user.local.LocalUser
import uz.oybek.ozamiz.databinding.FragmentMenuListBinding
import uz.oybek.ozamiz.ui.base.BaseFragment
import uz.oybek.ozamiz.ui.feature.premium.PremiumBottomSheetDialog
import uz.oybek.ozamiz.ui.feature.premium.isPremium

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
        setupPaymentCheckListener()
    }

    private fun checkPremiumAndNavigate(actionId: Int) {
        val user = LocalUser.user
        if (user.isPremium()) {
            navigate(actionId)
        } else {
            PremiumBottomSheetDialog().show(
                childFragmentManager,
                PremiumBottomSheetDialog.TAG
            )
        }
    }

    private fun setupPaymentCheckListener() {
        childFragmentManager.setFragmentResultListener(
            PremiumBottomSheetDialog.REQUEST_KEY,
            viewLifecycleOwner
        ) { _, bundle ->
            val result = bundle.getString(PremiumBottomSheetDialog.RESULT_ACTION)
            if (result == "go_to_payment") {
                val price = bundle.getLong("selected_price", 29000L)
                val isYearly = bundle.getBoolean("is_yearly", false)
                val args = bundleOf("price" to price, "is_yearly" to isYearly)
                findNavController().navigate(
                    R.id.paymentGetCheckFragment,
                    args
                )
            }
        }
    }

    private fun setupListeners(bind: FragmentMenuListBinding) {
        bind.itemBreakfast.setOnClickListener {
            checkPremiumAndNavigate(R.id.action_menuListFragment_to_breakfastGuideFragment)
        }
        bind.itemLunch.setOnClickListener {
            checkPremiumAndNavigate(R.id.action_menuListFragment_to_lunchGuideFragment)
        }
        bind.itemDinner.setOnClickListener {
            checkPremiumAndNavigate(R.id.action_menuListFragment_to_dinnerGuideFragment)
        }
        bind.itemSnacks.setOnClickListener {
            checkPremiumAndNavigate(R.id.action_menuListFragment_to_snacksGuideFragment)
        }
        bind.itemProtein.setOnClickListener {
            checkPremiumAndNavigate(R.id.action_menuListFragment_to_proteinGuideFragment)
        }
        bind.itemCarbs.setOnClickListener {
            checkPremiumAndNavigate(R.id.action_menuListFragment_to_carbsGuideFragment)
        }
        bind.itemLipids.setOnClickListener {
            checkPremiumAndNavigate(R.id.action_menuListFragment_to_lipidsGuideFragment)
        }
    }
}