package uz.oybek.ozamiz.ui.feature.auth

import android.app.Activity
import android.content.Intent
import android.content.IntentSender
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.google.android.gms.auth.api.identity.BeginSignInRequest
import com.google.android.gms.auth.api.identity.Identity
import com.google.android.gms.auth.api.identity.SignInClient
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.launch
import uz.oybek.ozamiz.R
import uz.oybek.ozamiz.databinding.FragmentAuthBinding
import uz.oybek.ozamiz.ui.base.BaseFragment
import uz.oybek.ozamiz.ui.utils.postVal
import uz.oybek.ozamiz.ui.utils.showToast
import uz.oybek.ozamiz.util.PermissionController
import uz.oybek.ozamiz.util.handleException

class AuthFragment : BaseFragment<FragmentAuthBinding>() {

    override fun inflateBinding(
        inflater: LayoutInflater, container: ViewGroup?
    ): FragmentAuthBinding {
        return FragmentAuthBinding.inflate(inflater)
    }

    private val viewModel: AuthViewModel by viewModels()

    private var splash = true

    private var signInRequest: BeginSignInRequest? = null
    private var oneTapClient: SignInClient? = null

    private var isSigning = false

    private val intentSenderLauncher =
        registerForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val data = result.data
                if (data != null) {
                    try {
                        val credential = oneTapClient?.getSignInCredentialFromIntent(data)
                        val idToken = credential?.googleIdToken
                        if (idToken != null) {
                            val firebaseCredential = GoogleAuthProvider.getCredential(idToken, null)

                            if (context != null && activity != null) {
                                viewModel.sendState.postVal(SendState.Sending)
                                viewModel.signInGoogle(firebaseCredential) {
                                    if (it) {
                                        verified()
                                    } else {
                                        viewModel.sendState.postVal(SendState.Input())
                                        isSigning = false
                                    }
                                }
                            } else {
                                isSigning = false
                            }
                        }
                    } catch (e: Exception) {
                        handleException(e)
                        isSigning = false
                    }
                } else {
                    isSigning = false
                }
            } else {
                isSigning = false
            }
        }

    private fun loginGoogle() {
        if (activity == null) return
        if (isSigning) return
        isSigning = true

        PermissionController.getInstance()
            .doOnActivityResult(PermissionController.ANY_REQUEST_CODE) { res, resultOk ->
                if (resultOk) {
                    if (res is Intent) {
                        try {
                            val credential = oneTapClient?.getSignInCredentialFromIntent(res)
                            val idToken = credential?.googleIdToken
                            if (idToken != null) {
                                val firebaseCredential =
                                    GoogleAuthProvider.getCredential(idToken, null)
                                if (context != null && activity != null) {
                                    viewModel.sendState.postVal(SendState.Sending)
                                    viewModel.signInGoogle(firebaseCredential) {
                                        if (it) {
                                            verified()
                                        } else {
                                            viewModel.sendState.postVal(SendState.Input())
                                        }
                                    }
                                }
                            }
                        } catch (e: Exception) {
                            handleException(e)
                        }
                    }
                }
            }
        signInRequest?.let { request ->
            oneTapClient?.beginSignIn(request)?.addOnSuccessListener { result ->
                try {
                    if (isDetached || isRemoving || isVisible.not()) return@addOnSuccessListener
                    intentSenderLauncher.launch(
                        IntentSenderRequest.Builder(result.pendingIntent).build()
                    )
                } catch (e: IntentSender.SendIntentException) {
                    showToast(e.message ?: "")
                }
            }?.addOnFailureListener { e ->
                showToast("Google One Tap error: ${e.localizedMessage}")
                android.util.Log.e("AuthFragment", "One Tap sign in failed", e)
            }
        }
    }

    private fun verified() {
        viewModel.sendState.postVal(SendState.Sending)
        isSigning = true

        viewLifecycleOwner.lifecycleScope.launch {
            if (activity == null) {
                isSigning = false
                return@launch
            }
            val user = viewModel.authFirebaseUser()
            if (isRemoving || user.valid.not()) {
                isSigning = false
                return@launch
            }
            isSigning = false
            closeFragment()
            isSigning = false
            if (user.name.isEmpty()) {
                navigate(R.id.onboarding)
            } else {
                mainActivity()?.recreateUi(true)
            }
        }
    }

    private fun initGoogleAuth() {
        if (activity == null) return
        oneTapClient = Identity.getSignInClient(requireActivity())

        val clientId = getString(R.string.your_web_client_id)
        signInRequest = BeginSignInRequest.builder().setGoogleIdTokenRequestOptions(
            BeginSignInRequest.GoogleIdTokenRequestOptions.builder()
                .setSupported(true)
                .setServerClientId(clientId)
                .setFilterByAuthorizedAccounts(false)
                .build()
        ).build()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        splash = arguments?.getBoolean("splash", false) ?: false
        initGoogleAuth()
        loginGoogle()
    }

    override fun viewCreated(bind: FragmentAuthBinding) {
        bind.apply {
            toolbar.setUpBackButton(this@AuthFragment)
            toolbar.isVisible = false

            Glide.with(requireContext()).load(R.drawable.splash_photo).into(photo)
            gmailButton.setOnClickListener {
                loginGoogle()
            }
            viewModel.sendState.observe(viewLifecycleOwner) {
                when (it) {
                    is SendState.Input -> {
                        progressBar.isVisible = false
                        gmailButton.isVisible = true
                        emailButton.isVisible = true
                    }

                    is SendState.Sending -> {
                        progressBar.isVisible = true
                        gmailButton.visibility = View.INVISIBLE
                        emailButton.visibility = View.INVISIBLE
                    }

                    else -> {}
                }
            }
            emailButton.setOnClickListener {
                navigate(R.id.loginGmailFragment)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        intentSenderLauncher.unregister()
        PermissionController.getInstance().removeCallback(PermissionController.ANY_REQUEST_CODE)
    }
}