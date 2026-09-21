package uz.oybek.ozamiz.ui.feature.onboarding.screens

import android.view.LayoutInflater
import android.view.ViewGroup
import uz.oybek.ozamiz.R
import uz.oybek.ozamiz.databinding.FragmentTargetWeightBinding
import uz.oybek.ozamiz.ui.feature.onboarding.OnBoardingChildFragment
import uz.oybek.ozamiz.ui.utils.showKeyboard

class TargetWeightFragment
    : OnBoardingChildFragment<
        FragmentTargetWeightBinding>() {
    override fun viewCreated(bind: FragmentTargetWeightBinding) {
        val (low, high) = viewModel.uiState.value.idealWeight
        bind.tvIdealWeight.text =
            getString(
                R.string.onboarding_target_weight_ideal_format,
                low,
                high
            )

        viewModel.uiState.value.user.targetWeight?.let {
            if (it > 0.0) {
                val targetStr = it.toInt().toString()
                bind.textInput.editText?.setText(targetStr)
                bind.textInput.editText?.setSelection(targetStr.length)
            }
        }

        bind.button.setOnClickListener {
            val inputTarget = bind.textInput
                .editText
                ?.text
                ?.toString()
                ?.toDoubleOrNull()

            if (inputTarget != null && inputTarget >= 40) {
                viewModel.updateUser { it.copy(targetWeight = inputTarget) }
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