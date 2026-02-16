package uz.oybek.ozamiz.ui.feature.onboarding.screens

import android.view.LayoutInflater
import android.view.ViewGroup
import uz.oybek.ozamiz.R
import uz.oybek.ozamiz.data.food.DietController
import uz.oybek.ozamiz.data.user.UserRepository
import uz.oybek.ozamiz.data.user.local.LocalUser
import uz.oybek.ozamiz.databinding.FragmentTargetWeightBinding
import uz.oybek.ozamiz.ui.feature.onboarding.OnBoardingChildFragment
import uz.oybek.ozamiz.ui.utils.showKeyboard

class TargetWeightFragment
    : OnBoardingChildFragment<
        FragmentTargetWeightBinding>() {
    override fun viewCreated(bind: FragmentTargetWeightBinding) {
        bind.apply {
            button.setOnClickListener {
                val inputTarget = textInput
                    .editText
                    ?.text
                    ?.toString()
                    ?.toIntOrNull()
                if (inputTarget != null && inputTarget > 40) {
                    LocalUser.update {
                        targetWeight = inputTarget.toDouble()
                    }
                    LocalUser.update {
                        targetCalories = DietController.calculateTargetCalories(this)
                        targetWaterMl = DietController.calculateTargetWaterMl(this.weight)
                    }
                    UserRepository.updateUser {}
                    moveToNextScreen()
                } else {
                    textInput.error = getString(R.string.error_weight)
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        binding.textInput.editText.showKeyboard()
    }

    override fun inflateBinding(
        inflater: LayoutInflater,
        container: ViewGroup?
    ): FragmentTargetWeightBinding {
        return FragmentTargetWeightBinding.inflate(
            inflater,
            container,
            false
        )
    }


}