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

        val isPremium = uz.oybek.ozamiz.data.user.local.LocalUser.user.premium
        if (!isPremium) {
            bind.premiumOverlay.visibility = android.view.View.VISIBLE
            bind.btnGetPremium.setOnClickListener {
                findNavController().navigate(uz.oybek.ozamiz.R.id.paymentGetCheckFragment)
            }
        } else {
            bind.premiumOverlay.visibility = android.view.View.GONE
        }
        bind.btnSeeReadyMenus.setOnClickListener {
            findNavController().navigate(BreakfastGuideFragmentDirections.actionBreakfastGuideFragmentToReadyMenusFragment())
        }
    }
}
