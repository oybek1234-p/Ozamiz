package uz.oybek.ozamiz.ui.feature.auth.email

import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import uz.oybek.ozamiz.R
import uz.oybek.ozamiz.appContext
import uz.oybek.ozamiz.databinding.LoginGmailFragmentBinding
import uz.oybek.ozamiz.ui.base.BaseFragment
import uz.oybek.ozamiz.ui.feature.auth.AuthViewModel
import uz.oybek.ozamiz.ui.utils.postVal
import uz.oybek.ozamiz.ui.utils.showKeyboard
import uz.oybek.ozamiz.ui.utils.showToast

class LoginGmailFragment :
    BaseFragment<LoginGmailFragmentBinding>() {

    private val viewModel: AuthViewModel by viewModels()

    override fun inflateBinding(
        inflater: LayoutInflater, container: ViewGroup?
    ): LoginGmailFragmentBinding {
        return LoginGmailFragmentBinding.inflate(inflater)
    }

    private var loading = MutableLiveData(false)

    override fun onResume() {
        super.onResume()
        binding.gmailView.editText?.showKeyboard()
    }

    private fun signInUser(email: String, password: String) {
        if (loading.value == true) return
        loading.postVal(true)
        viewModel.signInEmail(email, password) { result ->
            // Fragment view may have been destroyed while the network call was in-flight
            if (bindingT == null) return@signInEmail
            if (result.isSuccess) {
                signedIn()
            } else {
                val exception = result.exceptionOrNull()
                val msg = exception?.localizedMessage ?: ""
                when {
                    msg.contains("no user record") || msg.contains("invalid_user") || msg.contains("INVALID_LOGIN_CREDENTIALS") -> {
                        createNewUser(email, password)
                    }
                    msg.contains("network") || msg.contains("timeout") -> {
                        showToast(appContext.getString(R.string.internet_invalid))
                    }
                    msg.contains("too many requests") || msg.contains("TOO_MANY_ATTEMPTS_TRY_LATER") -> {
                        showToast(appContext.getString(R.string.retry_later))
                    }
                    else -> {
                        showToast(appContext.getString(R.string.email_or_pass_not_valid))
                        binding.boglanishButton.isVisible = true
                    }
                }
                loading.postVal(false)
            }
        }
    }

    private fun createNewUser(email: String, password: String) {
        loading.postVal(true)

        viewModel.signUpEmail(email, password) { result ->
            // Fragment view may have been destroyed while the network call was in-flight
            if (bindingT == null) return@signUpEmail
            if (result.isSuccess) {
                loading.postVal(false)
                signedIn()
            } else {
                loading.postVal(false)
                val exception = result.exceptionOrNull()
                val msg = exception?.localizedMessage ?: ""
                when {
                    msg.contains("weak password") || msg.contains("WEAK_PASSWORD") -> {
                        showToast(appContext.getString(R.string.weak_password))
                    }
                    msg.contains("invalid credential") || msg.contains("INVALID_EMAIL") -> {
                        showToast(appContext.getString(R.string.email_or_pass_not_valid))
                    }
                    msg.contains("network") || msg.contains("timeout") -> {
                        showToast(appContext.getString(R.string.internet_invalid))
                    }
                    msg.contains("too many requests") || msg.contains("TOO_MANY_ATTEMPTS_TRY_LATER") -> {
                        showToast(appContext.getString(R.string.retry_later))
                    }
                    else -> {
                        showToast(appContext.getString(R.string.biz_bilan_bog_lanish))
                        binding.boglanishButton.isVisible = true
                    }
                }
            }
        }
    }

    private fun signedIn() {
        loading.postVal(true)
        viewLifecycleOwner.lifecycleScope.launch {
            if (activity == null) return@launch
            val user = viewModel.authFirebaseUser()
            loading.postVal(false)
            if (user.valid.not()) return@launch
            mainActivity()?.recreateUi(true)
        }
    }

    override fun viewCreated(bind: LoginGmailFragmentBinding) {
        bind.apply {
            toolbar.setUpBackButton(this@LoginGmailFragment)

            loading.observe(viewLifecycleOwner) {
                continueButton.isEnabled = it.not()
                gmailView.isEnabled = it.not()
                passwordView.isEnabled = it.not()
                progressBar.isVisible = it
            }
            boglanishButton.setOnClickListener {
                mainActivity()?.showSupportSheet()
            }
            binding.boglanishButton?.isVisible = false
            continueButton.setOnClickListener {
                val email = gmailView.editText?.text?.toString()
                val password = passwordView.editText?.text?.toString()
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
                signInUser(email, password)
            }
        }
    }

}