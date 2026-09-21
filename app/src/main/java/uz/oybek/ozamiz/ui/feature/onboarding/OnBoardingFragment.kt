package uz.oybek.ozamiz.ui.feature.onboarding

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.fragment.findNavController
import dagger.hilt.android.AndroidEntryPoint
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.launch
import uz.oybek.ozamiz.R
import uz.oybek.ozamiz.databinding.FragmentOnboardingBinding
import uz.oybek.ozamiz.ui.base.BaseFragment

@AndroidEntryPoint
class OnBoardingFragment : BaseFragment<FragmentOnboardingBinding>() {

    private val viewModel: OnBoardingViewModel by viewModels()
    private var navController: NavController? = null

    override fun viewCreated(bind: FragmentOnboardingBinding) {
        val fragment = childFragmentManager.findFragmentById(
            R.id.container_child
        ) as NavHostFragment
        navController = fragment.findNavController()

        bind.backButton.setOnClickListener {
            viewModel.moveBack()
        }

        observeViewModel(bind)
    }

    private fun observeViewModel(bind: FragmentOnboardingBinding) {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.uiState.collect { state ->
                        bind.backButton.isEnabled = !state.isSaving
                        bind.progressBar.setProgress(
                            state.progress.toInt(),
                            true
                        )
                    }
                }
                launch {
                    viewModel.effect.collect { effect ->
                        handleEffect(effect)
                    }
                }
            }
        }
    }

    private fun handleEffect(effect: OnBoardingEffect) {
        when (effect) {
            is OnBoardingEffect.NavigateToScreen -> {
                navController?.navigate(
                    resId = effect.resId,
                    args = effect.args,
                    navOptions = animNavOptions
                )
            }

            is OnBoardingEffect.NavigateBack -> {
                navController?.popBackStack()
            }

            is OnBoardingEffect.SaveSuccess -> {
                navigate(
                    R.id.home,
                    null,
                    androidx.navigation.navOptions {
                        popUpTo(R.id.main_navigation) { inclusive = true }
                    }
                )
            }

            is OnBoardingEffect.ShowError -> {
                showRetryDialog()
            }
        }
    }

    private fun showRetryDialog() {
        if (!isAdded || isDetached) return
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.onboarding_plan_error_title)
            .setMessage(R.string.onboarding_plan_error_message)
            .setPositiveButton(R.string.onboarding_plan_retry) { dialog, _ ->
                dialog.dismiss()
                viewModel.saveOnboardingInfo()
            }
            .setNegativeButton(R.string.cancel, null)
            .setCancelable(false)
            .show()
    }

    override fun inflateBinding(
        inflater: LayoutInflater,
        container: ViewGroup?
    ): FragmentOnboardingBinding {
        return FragmentOnboardingBinding.inflate(inflater)
    }
}