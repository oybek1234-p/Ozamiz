package uz.oybek.ozamiz.ui.feature.auth

import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.bumptech.glide.Glide
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import uz.oybek.ozamiz.R
import uz.oybek.ozamiz.databinding.FragmentAuthBinding
import uz.oybek.ozamiz.ui.base.BaseFragment
import uz.oybek.ozamiz.ui.utils.getMessage
import uz.oybek.ozamiz.ui.utils.showToast

@AndroidEntryPoint
class AuthFragment : BaseFragment<FragmentAuthBinding>() {

    private val viewModel: AuthViewModel by viewModels()
    private lateinit var credentialManager: CredentialManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        credentialManager = CredentialManager.create(requireContext())
    }

    override fun viewCreated(bind: FragmentAuthBinding) {
        bind.apply {
            toolbar.setUpBackButton(this@AuthFragment)
            toolbar.isVisible = false

            Glide.with(requireContext()).load(R.drawable.splash_icon).into(photo)

            gmailButton.setOnClickListener {
                loginGoogle()
            }

            emailButton.setOnClickListener {
                navigate(R.id.loginGmailFragment)
            }
        }

        observeState()
    }

    private fun observeState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.uiState.collect { state ->
                        binding.progressBar.isVisible = state.isLoading
                        binding.gmailButton.text = if (state.isLoading) "" else getString(R.string.google_orqali_kirish)
                        binding.gmailButton.icon = if (state.isLoading) null else ContextCompat.getDrawable(requireContext(), R.drawable.google_ic)
                        binding.gmailButton.isEnabled = !state.isLoading
                        binding.gmailButton.alpha = 1f
                        binding.emailButton.isEnabled = !state.isLoading
                        binding.emailButton.alpha = if (state.isLoading) 0.5f else 1f
                    }
                }

                launch {
                    viewModel.uiEffect.collect { effect ->
                        when (effect) {
                            is AuthUiEffect.ShowError -> {
                                showToast(effect.error.getMessage(requireContext()))
                            }
                        }
                    }
                }
            }
        }
    }

    private fun loginGoogle() {
        if (viewModel.uiState.value.isLoading) return
        val activity = activity ?: return

        val clientId = getString(R.string.your_web_client_id)
        val googleIdOption = GetSignInWithGoogleOption.Builder(clientId)
            .build()

        val request = GetCredentialRequest.Builder().addCredentialOption(googleIdOption).build()

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val result = credentialManager.getCredential(
                    request = request, context = activity
                )

                val credential = result.credential
                if (credential is CustomCredential && credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                    val googleIdTokenCredential =
                        GoogleIdTokenCredential.createFrom(credential.data)
                    val idToken = googleIdTokenCredential.idToken
                    viewModel.signInWithGoogle(idToken)
                } else {
                    viewModel.onGoogleSignInError()
                }
            } catch (_: GetCredentialCancellationException) {
            } catch (e: Exception) {
                viewModel.onGoogleSignInError(e)
            }
        }
    }

    override fun inflateBinding(
        inflater: LayoutInflater, container: ViewGroup?
    ): FragmentAuthBinding {
        return FragmentAuthBinding.inflate(inflater, container, false)
    }
}