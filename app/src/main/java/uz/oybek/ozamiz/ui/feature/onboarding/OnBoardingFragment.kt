package uz.oybek.ozamiz.ui.feature.onboarding

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.fragment.findNavController
import uz.oybek.ozamiz.R
import uz.oybek.ozamiz.databinding.FragmentOnboardingBinding
import uz.oybek.ozamiz.ui.base.BaseFragment
import uz.oybek.ozamiz.ui.feature.onboarding.screens.MultipleAnswerFragment

class OnBoardingFragment : BaseFragment<FragmentOnboardingBinding>() {

    private val viewModel: OnBoardingViewModel by viewModels()
    private var navController: NavController? = null

    override fun viewCreated(bind: FragmentOnboardingBinding) {
        bind.apply {
            backButton.setOnClickListener {
                viewModel.moveBack()
            }
            setupProgress()
            setupNavigation()
        }
    }

    private fun setupProgress() {
        viewModel.progressLive.observe(viewLifecycleOwner) {
            bindingT?.progressBar?.setProgress(
                it.toInt(),
                true
            )
        }
    }

    private fun setupNavigation() {
        val fragment = childFragmentManager.findFragmentById(
            R.id.container_child
        ) as NavHostFragment
        navController = fragment.findNavController()
        viewModel
            .currentScreenDirection
            .observe(viewLifecycleOwner) {
                when (it) {
                    is OnBoardingDirection.Finished -> {
                        mainActivity()?.recreateUi()
                    }

                    is OnBoardingDirection.Back -> {
                        navController?.popBackStack()
                    }

                    is OnBoardingDirection.SingleScreen -> {
                        navController?.navigate(
                            resId = it.id,
                            args = null,
                            navOptions = animNavOptions
                        )
                    }

                    is OnBoardingDirection.MultipleAnswer -> {
                        navController?.navigate(
                            R.id.multipleAnswerFragment,
                            MultipleAnswerFragment
                                .getBundle(it.type),
                            navOptions = animNavOptions
                        )
                    }
                }
            }
    }

    override fun inflateBinding(
        inflater: LayoutInflater,
        container: ViewGroup?
    ): FragmentOnboardingBinding {
        return FragmentOnboardingBinding.inflate(inflater)
    }

}