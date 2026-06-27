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
        val user = LocalUser.user
        val height = user.height.toDouble()

        val (low, high) = DietController.getIdealWeight(height)
        bind.tvIdealWeight.text = "Siz uchun ideal vazn: $low - $high kg"

        user.targetWeight?.let {
            bind.textInput.editText?.setText(it.toInt().toString())
        }

        bind.button.setOnClickListener {
            val inputTarget = bind.textInput
                .editText
                ?.text
                ?.toString()
                ?.toDoubleOrNull()

            if (inputTarget != null && inputTarget >= 40) {
                LocalUser.update {
                    targetWeight = inputTarget
                    // Centralized calculation for calories, macros, and water
                    DietController.updateTargetValues(this)
                }
                UserRepository.updateUser {}
                moveToNextScreen()
            } else {
                bind.textInput.error = getString(R.string.error_weight)
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