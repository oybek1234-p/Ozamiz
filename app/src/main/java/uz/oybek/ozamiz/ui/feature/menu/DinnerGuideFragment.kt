package uz.oybek.ozamiz.ui.feature.menu

import android.view.LayoutInflater
import android.view.ViewGroup
import dagger.hilt.android.AndroidEntryPoint
import uz.oybek.ozamiz.data.profile.api.ProfileRepo
import uz.oybek.ozamiz.databinding.FragmentDinnerGuideBinding
import uz.oybek.ozamiz.ui.base.BaseFragment
import javax.inject.Inject

@AndroidEntryPoint
class DinnerGuideFragment : BaseFragment<FragmentDinnerGuideBinding>() {

    @Inject
    lateinit var profileRepo: ProfileRepo

    override fun inflateBinding(
        inflater: LayoutInflater,
        container: ViewGroup?
    ) = FragmentDinnerGuideBinding.inflate(inflater, container, false)

    override fun viewCreated(bind: FragmentDinnerGuideBinding) {
        bind.btnBack.setOnClickListener {
            navigateUp()
        }

        val isPremium = profileRepo.currentUser.premium
        if (!isPremium) {
            bind.premiumOverlay.visibility = android.view.View.VISIBLE
            bind.btnGetPremium.setOnClickListener {
                navigate(uz.oybek.ozamiz.R.id.paymentGetCheckFragment)
            }
        } else {
            bind.premiumOverlay.visibility = android.view.View.GONE
        }
    }
}
