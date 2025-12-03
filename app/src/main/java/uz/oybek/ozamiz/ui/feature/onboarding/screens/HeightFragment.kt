package uz.oybek.ozamiz.ui.feature.onboarding.screens

import android.view.LayoutInflater
import android.view.ViewGroup
import uz.oybek.ozamiz.R
import uz.oybek.ozamiz.data.user.local.LocalUser
import uz.oybek.ozamiz.databinding.FragmentHeightBinding
import uz.oybek.ozamiz.ui.feature.onboarding.OnBoardingChildFragment
import uz.oybek.ozamiz.ui.utils.showKeyboard

class HeightFragment : OnBoardingChildFragment<FragmentHeightBinding>() {

    override fun viewCreated(bind: FragmentHeightBinding) {
        bind.apply {
            button.setOnClickListener {
                val height = textInput
                    .editText
                    ?.text
                    .toString()
                    .toIntOrNull()
                if (height != null && height > 100) {
                    LocalUser.update {
                        this.height = height
                    }
                    moveToNextScreen()
                } else {
                    textInput.error =
                        getString(R.string.error_height)
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
    ): FragmentHeightBinding {
        return FragmentHeightBinding.inflate(inflater, container, false)
    }
}