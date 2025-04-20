package uz.oybek.ozamiz

import android.content.Intent
import android.content.IntentSender
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.os.Bundle
import android.view.LayoutInflater
import android.view.animation.DecelerateInterpolator
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
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
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import uz.oybek.ozamiz.data.LocalUser
import uz.oybek.ozamiz.data.SocialMedia
import uz.oybek.ozamiz.data.valid
import uz.oybek.ozamiz.databinding.ActivityMainBinding
import uz.oybek.ozamiz.databinding.NoInternetDialogBinding
import uz.oybek.ozamiz.databinding.SupportSheetBinding
import uz.oybek.ozamiz.ui.base.BaseFragment
import uz.oybek.ozamiz.ui.visibleOrGone

class MainActivity : AppCompatActivity() {

    lateinit var navcontroller: NavController
    private lateinit var navHost: NavHostFragment

    private lateinit var binding: ActivityMainBinding


    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<out String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        PermissionController.getInstance().onPermissionResult(requestCode, grantResults)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        PermissionController.getInstance()
            .onActivityResult(requestCode, data, resultCode == RESULT_OK)
    }

    private val updateLauncher = registerForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult()
    ) {}

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(null)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        initUser()

        binding.apply {
            navHost = (supportFragmentManager.findFragmentById(R.id.container) as NavHostFragment)
            navcontroller = navHost.navController
            NavigationUI.setupWithNavController(binding.bottomNavView, navcontroller)
        }

        val inflater = navHost.navController.navInflater
        val graph = inflater.inflate(R.navigation.main_navigation)
        val user = LocalUser.user
        var new = false
        if (LocalUser.user.valid) {
            if (user.name.isEmpty() || user.gender == -1 || user.age == -1 || user.height == -1 || user.weight == -1) {
                graph.setStartDestination(R.id.getUserNameFragment)
                new = true
            } else {
                graph.setStartDestination(R.id.home)
            }
        } else {
            graph.setStartDestination(R.id.authFragment)
        }
        navHost.navController.setGraph(graph, Bundle().apply {
            putBoolean("new", new)
        })

        initUpdateManager(updateLauncher)
        initInternetConnectivity()
    }

    private fun initUser() {
        LocalUser.getUser()
    }

    fun showSupportSheet() {
        val dialog = BottomSheetDialog(this, R.style.SheetStyle)
        val binding = SupportSheetBinding.inflate(LayoutInflater.from(this), null, false)
        dialog.setContentView(binding.root)
        binding.apply {
            callview.setOnClickListener {
                dialog.dismiss()
                openPhoneCall(this@MainActivity, "+998971871415")
            }
            requestButton.setOnClickListener {
                dialog.dismiss()
                SocialMedia.openLink(
                    this@MainActivity, SocialMedia.parseTelegramLink("@oybekmi")
                )
            }
        }
        dialog.show()
    }

    fun recreateUi(isNew: Boolean = false) {
        finish()
        startActivity(intent.also {
            it.putExtra("new", isNew)
        })
    }

    private fun initUpdateManager(launcher: ActivityResultLauncher<IntentSenderRequest>) {
        try {
            val appUpdateManager = AppUpdateManagerFactory.create(applicationContext)
            val appUpdateInfoTask = appUpdateManager.appUpdateInfo
            val listener = InstallStateUpdatedListener {
                if (it.installStatus() == InstallStatus.DOWNLOADED) {
                    // Complete the update if downloaded
                    appUpdateManager.completeUpdate()
                }
            }

            appUpdateManager.registerListener(listener)

            appUpdateInfoTask.addOnSuccessListener { appUpdateInfo ->
                if (isDestroyed || isFinishing) return@addOnSuccessListener
                if (appUpdateInfo.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE) {
                    try {
                        // Start the update flow using the registered launcher
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

    fun requestNotificationPermission() {
        val permissionState =
            ContextCompat.checkSelfPermission(this, android.Manifest.permission.POST_NOTIFICATIONS);

        if (permissionState == PackageManager.PERMISSION_DENIED) {
            ActivityCompat.requestPermissions(
                this, arrayOf(android.Manifest.permission.POST_NOTIFICATIONS), 1
            );
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
            MainScope().launch {
                internetBottomSheet.show()
            }
        }
        val networkCallback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                super.onAvailable(network)
                if (isFinishing || isDestroyed) return
                if (hasInternet) {
                    return
                }
                hasInternet = true
                lifecycleScope.launch {
                    internetBottomSheet.dismiss()
                    try {
                        val currentFragment = navHost.childFragmentManager.fragments[0]
                        if (currentFragment is BaseFragment<*>) currentFragment.onInternetAvailable()
                    } catch (e: Exception) {
                        handleException(e)
                    }
                }
            }

            override fun onLost(network: Network) {
                super.onLost(network)
                if (isFinishing || isDestroyed) return
                if (window.decorView.isAttachedToWindow) {
                    lifecycleScope.launch {
                        internetBottomSheet.show()
                    }
                }
                hasInternet = false
            }
        }
        connectivityManager.requestNetwork(networkRequest, networkCallback)
    }

    private val internetBottomSheet: BottomSheetDialog by lazy {
        BottomSheetDialog(this).apply {
            val binding =
                NoInternetDialogBinding.inflate(LayoutInflater.from(this@MainActivity), null, false)
            setContentView(binding.root)
            binding.button.setOnClickListener {
                dismiss()
            }
        }
    }

    private var bottomSetUp = false
    private var bottomNavShown = true

    private val decelerateInterpolator = DecelerateInterpolator(1.5f)
    fun showBottomSheet(show: Boolean, animate: Boolean) {
        if (bottomNavShown == show) return
        bottomNavShown = show
        binding.bottomNavView.apply {
            if (!bottomSetUp && show) {
                bottomSetUp = true
            }
            if (show && !isVisible) {
                visibleOrGone(true)
            }
            if (!show && !isVisible) return
            if (show) {
                translationY = 120.toFloat()
            }
            if (animate) {
                binding.root.post {
                    animate().setDuration(180).setInterpolator(decelerateInterpolator)
                        .translationY(if (show) 0f else measuredHeight.toFloat()).withEndAction {
                            if (!show) {
                                visibleOrGone(false)
                            }
                        }.start()
                }
            } else {
                visibleOrGone(show)
            }
        }
    }

}