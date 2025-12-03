package uz.oybek.ozamiz.ui.feature.onboarding.screens

import android.view.LayoutInflater
import android.view.ViewGroup
import uz.oybek.ozamiz.databinding.FragmentSeeEffectBinding
import uz.oybek.ozamiz.ui.feature.onboarding.OnBoardingChildFragment

class SeeEffectFragment
    : OnBoardingChildFragment<FragmentSeeEffectBinding>() {

    override fun viewCreated(bind: FragmentSeeEffectBinding) {
        bind.apply {
            button.setOnClickListener {
                moveToNextScreen()
            }
        }
    }

    override fun inflateBinding(
        inflater: LayoutInflater,
        container: ViewGroup?
    ): FragmentSeeEffectBinding {
        return FragmentSeeEffectBinding.inflate(inflater, container, false)
    }


}