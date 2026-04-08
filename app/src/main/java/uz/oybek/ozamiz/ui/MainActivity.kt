package uz.oybek.ozamiz.ui

import android.annotation.SuppressLint
import android.content.Intent
import android.content.IntentSender
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.os.Bundle
import android.view.LayoutInflater
import android.view.animation.DecelerateInterpolator
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.NavigationUI
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.appupdate.AppUpdateOptions
import com.google.android.play.core.install.InstallStateUpdatedListener
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.InstallStatus
import com.google.android.play.core.install.model.UpdateAvailability
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import uz.oybek.ozamiz.R
import uz.oybek.ozamiz.data.user.UserRepository
import uz.oybek.ozamiz.data.user.local.LocalUser
import uz.oybek.ozamiz.data.utils.CompanyLinks
import uz.oybek.ozamiz.databinding.ActivityMainBinding
import uz.oybek.ozamiz.databinding.NoInternetDialogBinding
import uz.oybek.ozamiz.databinding.SupportSheetBinding
import uz.oybek.ozamiz.ui.base.BaseFragment
import uz.oybek.ozamiz.ui.utils.visibleOrGone
import uz.oybek.ozamiz.util.PhoneUtils
import uz.oybek.ozamiz.util.handleException
import java.util.Calendar

class MainActivity : AppCompatActivity() {

    lateinit var navController: NavController
    private lateinit var navHost: NavHostFragment

    private lateinit var binding: ActivityMainBinding

    private fun handleSignOut() {
        if (UserRepository.user.valid.not()) return
        UserRepository.signOut()
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }

    init {
        signOut = Runnable {
            handleSignOut()
        }
    }

    private val updateLauncher = registerForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult()
    ) {

    }

    companion object {
        var signOut: Runnable? = null
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        initUser()

        binding.apply {
                navHost =
                (supportFragmentManager.findFragmentById(R.id.container) as NavHostFragment)
            navController = navHost.navController
            NavigationUI.setupWithNavController(
                navigationBarView = binding.bottomNavView,
                navController = navController
            )
        }
        val inflater = navHost.navController.navInflater
        val graph = inflater.inflate(R.navigation.main_navigation)
        val user = LocalUser.user

        if (user.valid) {
            checkRegisterDate()
            if (user.name.isEmpty()) {
                graph.setStartDestination(R.id.onboarding)
            }
        } else {
            graph.setStartDestination(R.id.authFragment)
        }
        navHost.navController.graph = graph

        initUpdateManager(updateLauncher)
        initInternetConnectivity()
        
        lifecycleScope.launch {
            uz.oybek.ozamiz.data.lessons.LessonSeeder.seedData()
        }


    }

    private fun checkRegisterDate() {
        if (LocalUser.user.registerDate == null) {
            UserRepository.updateUser {
                it.registerDate = Calendar.getInstance().time
            }
        }
    }

    private fun initUser() {
        LocalUser.getUser()
        if (LocalUser.user.valid) {
            UserRepository.updateLastSeen()
            UserRepository.observeCurrentUser { }
            lifecycleScope.launch(Dispatchers.IO) {
                UserRepository.refreshUser()
            }
        }
    }

    fun showSupportSheet() {
        val dialog = BottomSheetDialog(this, R.style.SheetStyle)
        val bindingDialog = SupportSheetBinding.inflate(
            LayoutInflater.from(this),
            null,
            false
        )
        dialog.setContentView(bindingDialog.root)
        bindingDialog.apply {
            callview.setOnClickListener {
                dialog.dismiss()
                PhoneUtils.openPhoneCall(CompanyLinks.Companion.PHONE_NUMBER)
            }
            requestButton.setOnClickListener {
                dialog.dismiss()
                CompanyLinks.Companion.openLink(
                    this@MainActivity, CompanyLinks.Companion.TELEGRAM
                )
            }
        }
        dialog.show()
    }

    @SuppressLint("UnsafeIntentLaunch")
    fun recreateUi(isNew: Boolean = false) {
        finish()
        this.startActivity(intent.also {
            it.putExtra("new", isNew)
        })
    }

    private fun initUpdateManager(launcher: ActivityResultLauncher<IntentSenderRequest>) {
        try {
            val appUpdateManager = AppUpdateManagerFactory.create(applicationContext)
            val appUpdateInfoTask = appUpdateManager.appUpdateInfo
            val listener = InstallStateUpdatedListener {
                if (it.installStatus() == InstallStatus.DOWNLOADED) {
                    appUpdateManager.completeUpdate()
                }
            }

            appUpdateManager.registerListener(listener)

            appUpdateInfoTask.addOnSuccessListener { appUpdateInfo ->
                if (isDestroyed || isFinishing) return@addOnSuccessListener
                if (appUpdateInfo.updateAvailability() ==
                    UpdateAvailability.UPDATE_AVAILABLE && appUpdateInfo.isUpdateTypeAllowed(
                        AppUpdateType.FLEXIBLE
                    )
                ) {
                    try {
                        appUpdateManager.startUpdateFlowForResult(
                            appUpdateInfo,
                            launcher,
                            AppUpdateOptions.newBuilder(AppUpdateType.FLEXIBLE).build()
                        )
                    } catch (e: IntentSender.SendIntentException) {
                        handleException(e)
                    }
                }
            }
        } catch (e: Exception) {
            handleException(e)
        }
    }

    private fun initInternetConnectivity() {
        val networkRequest =
            NetworkRequest.Builder().addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
                .addTransportType(NetworkCapabilities.TRANSPORT_CELLULAR).build()
        val connectivityManager =
            getSystemService(ConnectivityManager::class.java) as ConnectivityManager
        var hasInternet = connectivityManager.activeNetwork != null
        if (connectivityManager.activeNetwork == null) {
            lifecycleScope.launch(Dispatchers.Main) {
                if (!isFinishing && !isDestroyed) internetBottomSheet.show()
            }
        }
        val networkCallback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                super.onAvailable(network)
                lifecycleScope.launch(Dispatchers.Main) {
                    if (isFinishing || isDestroyed) return@launch
                    if (hasInternet) {
                        return@launch
                    }
                    hasInternet = true
                    if (internetBottomSheet.isShowing) {
                        try {
                            if (!isFinishing && !isDestroyed) {
                                internetBottomSheet.dismiss()
                            }
                        } catch (_: Exception) {
                        }
                    }
                    try {
                        val currentFragment = navHost.childFragmentManager.fragments.getOrNull(0)
                        if (currentFragment is BaseFragment<*>) currentFragment.onInternetAvailable()
                    } catch (e: Exception) {
                        handleException(e)
                    }
                }
            }

            override fun onLost(network: Network) {
                super.onLost(network)
                lifecycleScope.launch(Dispatchers.Main) {
                    if (isFinishing || isDestroyed) return@launch
                    if (window.decorView.isAttachedToWindow && !internetBottomSheet.isShowing) {
                        internetBottomSheet.show()
                    }
                    hasInternet = false
                }
            }
        }
        connectivityManager.requestNetwork(networkRequest, networkCallback)
    }

    private val internetBottomSheet: BottomSheetDialog by lazy {
        BottomSheetDialog(this).apply {
            val dialogBinding =
                NoInternetDialogBinding.inflate(LayoutInflater.from(this@MainActivity), null, false)
            setContentView(dialogBinding.root)
            dialogBinding.button.setOnClickListener {
                dismiss()
            }
            setCancelable(false)
            setCanceledOnTouchOutside(false)
        }
    }

    private var bottomSetUp = false
    private var bottomNavShown = true

    private val decelerateInterpolator = DecelerateInterpolator(1.5f)
    fun showBottomSheet(show: Boolean, animate: Boolean) {
        if (bottomNavShown == show && binding.bottomNavView.isVisible == show) return
        bottomNavShown = show
        binding.bottomNavView.apply {
            if (!bottomSetUp && show) {
                bottomSetUp = true
            }
            if (show && !isVisible) {
                visibleOrGone(true)
            }
            if (!show && !isVisible) return

            val targetY = if (show) 0f else measuredHeight.toFloat()
            if (animate) {
                if (show) translationY =
                    measuredHeight.toFloat()

                animate().setDuration(180).setInterpolator(decelerateInterpolator)
                    .translationY(targetY).withEndAction {
                        if (!show) {
                            visibleOrGone(false)
                        }
                    }.start()
            } else {
                translationY = targetY
                visibleOrGone(show)
            }
        }
    }
}