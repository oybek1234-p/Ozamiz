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
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import uz.oybek.ozamiz.data.LocalUser
import uz.oybek.ozamiz.data.SocialMedia
import uz.oybek.ozamiz.data.UserRepository
import uz.oybek.ozamiz.databinding.ActivityMainBinding
import uz.oybek.ozamiz.databinding.NoInternetDialogBinding
import uz.oybek.ozamiz.databinding.SupportSheetBinding
import uz.oybek.ozamiz.reminders.ReminderScheduler
import uz.oybek.ozamiz.ui.base.BaseFragment
import uz.oybek.ozamiz.ui.visibleOrGone
import uz.oybek.ozamiz.util.LivesManager // LivesManager importi
import java.util.Calendar

class MainActivity : AppCompatActivity() {

    lateinit var navcontroller: NavController
    private lateinit var navHost: NavHostFragment

    private lateinit var binding: ActivityMainBinding

    init {
        signOut = Runnable {
            if (UserRepository.user.valid.not()) return@Runnable
            UserRepository.signOut()
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<out String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        PermissionController.getInstance().onPermissionResult(requestCode, grantResults)
        if (requestCode == NOTIFICATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                ReminderScheduler.scheduleAllActiveReminders(this)
            } else {
                // Ruxsat berilmadi
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        PermissionController.getInstance()
            .onActivityResult(requestCode, data, resultCode == RESULT_OK)
    }

    private val updateLauncher = registerForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult()
    ) {}

    companion object {
        private const val NOTIFICATION_PERMISSION_REQUEST_CODE = 101
        var signOut: Runnable? = null
        private const val WEIGHT_LOG_INTERVAL_DAYS = 7
        fun getWeightLogIntervalDays(): Int = WEIGHT_LOG_INTERVAL_DAYS
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)

        WindowCompat.setDecorFitsSystemWindows(window, false)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        initUser() // Bu yerda LocalUser yuklanadi va LivesManager chaqiriladi

        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { view, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        binding.apply {
            navHost = (supportFragmentManager.findFragmentById(R.id.container) as NavHostFragment)
            navcontroller = navHost.navController
            NavigationUI.setupWithNavController(binding.bottomNavView, navcontroller)
        }
        val inflater = navHost.navController.navInflater
        val graph = inflater.inflate(R.navigation.main_navigation)
        val user = LocalUser.user

        if (user.valid) {
            if (user.registerDate == null) {
                UserRepository.updateUser {
                    it.registerDate = Calendar.getInstance()
                        .apply { add(Calendar.YEAR, -1) }.time
                }
            }
            if (user.name.isEmpty() || user.gender == -1 || user.age == -1 || user.height == -1 || user.weight == -1.0) {
                graph.setStartDestination(R.id.getMainInfo)
            } else {
                graph.setStartDestination(R.id.home) // Yoki darslar ekrani
            }
        } else {
            graph.setStartDestination(R.id.authFragment)
        }
        navHost.navController.graph = graph

        initUpdateManager(updateLauncher)
        initInternetConnectivity()
    }

    private fun initUser() {
        LocalUser.getUser()
        if (LocalUser.user.valid) {
            lifecycleScope.launch(Dispatchers.IO) {
                LivesManager.checkAndRefillLives()
            }
        }
        UserRepository.observeCurrentUser {
            it?.let { currentUser ->
                if (currentUser.valid) {
                    ReminderScheduler.scheduleAllActiveReminders(this)
                }
            }
        }
    }
    fun showSupportSheet() {
        val dialog = BottomSheetDialog(this, R.style.SheetStyle)
        val bindingDialog = SupportSheetBinding.inflate(LayoutInflater.from(this), null, false)
        dialog.setContentView(bindingDialog.root)
        bindingDialog.apply {
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
                    appUpdateManager.completeUpdate()
                }
            }

            appUpdateManager.registerListener(listener)

            appUpdateInfoTask.addOnSuccessListener { appUpdateInfo ->
                if (isDestroyed || isFinishing) return@addOnSuccessListener
                if (appUpdateInfo.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE && appUpdateInfo.isUpdateTypeAllowed(
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
                        } catch (e: IllegalArgumentException) {
                            // Activity is likely finishing
                        } catch (e: Exception) {
                            handleException(e)
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