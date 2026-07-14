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

        bind.btnSeeReadyMenus.setOnClickListener {
            findNavController().navigate(SnacksGuideFragmentDirections.actionSnacksGuideFragmentToReadyMenusFragment())
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
    }
}
