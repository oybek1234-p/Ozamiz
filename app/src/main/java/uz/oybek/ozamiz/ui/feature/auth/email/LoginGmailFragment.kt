package uz.oybek.ozamiz.ui.feature.auth.email

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import uz.oybek.ozamiz.R
import uz.oybek.ozamiz.databinding.LoginGmailFragmentBinding
import uz.oybek.ozamiz.ui.base.BaseFragment
import uz.oybek.ozamiz.ui.feature.auth.AuthUiEffect
import uz.oybek.ozamiz.ui.feature.auth.AuthViewModel
import uz.oybek.ozamiz.ui.utils.getMessage
import uz.oybek.ozamiz.ui.utils.showKeyboard
import uz.oybek.ozamiz.ui.utils.showToast

@AndroidEntryPoint
class LoginGmailFragment : BaseFragment<LoginGmailFragmentBinding>() {

    private val viewModel: AuthViewModel by viewModels()

    override fun inflateBinding(
        inflater: LayoutInflater, container: ViewGroup?
    ): LoginGmailFragmentBinding {
        return LoginGmailFragmentBinding.inflate(inflater)
    }

    override fun onResume() {
        super.onResume()
        binding.gmailView.editText?.showKeyboard()
    }

    override fun viewCreated(bind: LoginGmailFragmentBinding) {
        bind.apply {
            toolbar.setUpBackButton(this@LoginGmailFragment)
            binding.boglanishButton.isVisible = false

            boglanishButton.setOnClickListener {
                mainActivity()?.showSupportSheet()
            }

            continueButton.setOnClickListener {
                val email = gmailView.editText?.text?.toString()?.trim()
                val password = passwordView.editText?.text?.toString()?.trim()
                if (email.isNullOrEmpty()) {
                    showToast(getString(R.string.insert_email))
                    return@setOnClickListener
                }
                if (password.isNullOrEmpty()) {
                    showToast(getString(R.string.insert_password))
                    return@setOnClickListener
                }
                if (password.length < 6) {
                    showToast(getString(R.string.invalid_password))
                    return@setOnClickListener
                }
                viewModel.signInEmail(email, password)
            }
        }

        observeUi()
    }

    private fun observeUi() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.uiState.collect { state ->
                        binding.continueButton.isEnabled = !state.isLoading
                        binding.gmailView.isEnabled = !state.isLoading
                        binding.passwordView.isEnabled = !state.isLoading
                        binding.progressBar.isVisible = state.isLoading
                    }
                }

                launch {
                    viewModel.uiEffect.collect { effect ->
                        when (effect) {
                            is AuthUiEffect.ShowError -> {
                                handleError(effect.error)
                            }
                        }
                    }
                }
            }
        }
    }

    private fun handleError(exception: Exception?) {
        binding.boglanishButton.isVisible = true
        showToast(exception.getMessage(requireContext()))
    }
}