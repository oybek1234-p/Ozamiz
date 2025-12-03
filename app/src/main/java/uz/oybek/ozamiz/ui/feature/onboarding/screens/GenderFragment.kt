package uz.oybek.ozamiz.ui.feature.onboarding.screens

import android.view.LayoutInflater
import android.view.ViewGroup
import uz.oybek.ozamiz.data.user.local.LocalUser
import uz.oybek.ozamiz.data.user.model.info.Gender
import uz.oybek.ozamiz.databinding.FragmentGenderBinding
import uz.oybek.ozamiz.ui.feature.onboarding.OnBoardingChildFragment

class GenderFragment : OnBoardingChildFragment<FragmentGenderBinding>() {

    override fun viewCreated(bind: FragmentGenderBinding) {
        bind.apply {
            maleCard.setOnClickListener {
                selected(Gender.MALE)
            }
            femaleCard.setOnClickListener {
                selected(Gender.FEMALE)
            }
        }
    }

    private fun selected(gender: Int) {
        LocalUser.update {
            this.gender = gender
        }
        moveToNextScreen()
    }

    override fun inflateBinding(
        inflater: LayoutInflater,
        container: ViewGroup?
    ): FragmentGenderBinding {
        return FragmentGenderBinding.inflate(
            inflater,
            container,
            false
        )
    }
}