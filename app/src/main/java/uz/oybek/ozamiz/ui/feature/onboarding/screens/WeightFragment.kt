package uz.oybek.ozamiz.ui.feature.onboarding.screens

import android.view.LayoutInflater
import android.view.ViewGroup
import uz.oybek.ozamiz.R
import uz.oybek.ozamiz.databinding.FragmentWeightBinding
import uz.oybek.ozamiz.ui.feature.onboarding.OnBoardingChildFragment
import uz.oybek.ozamiz.ui.utils.showKeyboard

class WeightFragment
    : OnBoardingChildFragment<FragmentWeightBinding>() {

    override fun viewCreated(bind: FragmentWeightBinding) {
        val currentWeight = viewModel.uiState.value.user.weight
        if (currentWeight > 0.0) {
            val weightStr = currentWeight.toInt().toString()
            bind.textInput.editText?.setText(weightStr)
            bind.textInput.editText?.setSelection(weightStr.length)
        }

        bind.apply {
            button.setOnClickListener {
                val inputWeight = textInput
                    .editText
                    ?.text
                    ?.toString()
                    ?.toIntOrNull()
                if (inputWeight != null && inputWeight > 40) {
                    val w = inputWeight.toDouble()
                    viewModel.setWeight(w)
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
    ): FragmentWeightBinding {
        return FragmentWeightBinding.inflate(
            inflater,
            container,
            false
        )
    }

}