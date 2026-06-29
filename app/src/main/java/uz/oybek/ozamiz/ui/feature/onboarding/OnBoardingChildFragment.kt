package uz.oybek.ozamiz.ui.feature.onboarding

import androidx.fragment.app.viewModels
import androidx.viewbinding.ViewBinding
import uz.oybek.ozamiz.ui.base.BaseFragment

abstract class OnBoardingChildFragment<T : ViewBinding> : BaseFragment<T>() {

    val viewModel
            : OnBoardingViewModel by viewModels(
        ownerProducer = {
            requireParentFragment().requireParentFragment()
        }
    )

    fun moveToNextScreen() {
        viewModel.moveToNextScreen()
    }
}