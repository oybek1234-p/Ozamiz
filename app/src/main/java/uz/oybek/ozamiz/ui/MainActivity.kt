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
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isInvisible
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavController
import androidx.navigation.NavGraph
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.NavigationUI
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.appupdate.AppUpdateOptions
import com.google.android.play.core.install.InstallStateUpdatedListener
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.InstallStatus
import com.google.android.play.core.install.model.UpdateAvailability
import com.google.android.play.core.review.ReviewManagerFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import uz.oybek.ozamiz.R
import uz.oybek.ozamiz.data.ai.aiChat.AiChatRepository
import uz.oybek.ozamiz.data.config.AppPreference
import uz.oybek.ozamiz.data.level.Level
import uz.oybek.ozamiz.data.level.LevelRepository
import uz.oybek.ozamiz.data.level.PointsRepository
import uz.oybek.ozamiz.data.motivation.NotificationSchedular
import uz.oybek.ozamiz.data.user.UserAuthRepository
import uz.oybek.ozamiz.data.user.UserProfileRepository
import uz.oybek.ozamiz.data.user.local.LocalUser
import uz.oybek.ozamiz.data.user.local.user
import uz.oybek.ozamiz.data.utils.CompanyLinks
import uz.oybek.ozamiz.databinding.ActivityMainBinding
import uz.oybek.ozamiz.databinding.DialogLevelUpBinding
import uz.oybek.ozamiz.databinding.NoInternetDialogBinding
import uz.oybek.ozamiz.databinding.SupportSheetBinding
import uz.oybek.ozamiz.ui.base.BaseFragment
import uz.oybek.ozamiz.ui.utils.showToast
import uz.oybek.ozamiz.ui.utils.visibleOrGone
import uz.oybek.ozamiz.util.PhoneUtils
import uz.oybek.ozamiz.util.handleException

class MainActivity : AppCompatActivity() {

    lateinit var navController: NavController
    private lateinit var navHost: NavHostFragment

    private lateinit var binding: ActivityMainBinding
    private var systemInsets: WindowInsetsCompat? = null
    private var topInsetPaddingEnabled = true

    fun setTopInsetPaddingEnabled(enabled: Boolean) {
        if (topInsetPaddingEnabled == enabled) return
        topInsetPaddingEnabled = enabled
        updateContainerPadding()
    }

    private fun updateRootPadding() {
        val insets = systemInsets ?: return
        val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
        binding.root.setPadding(
            systemBars.left,
            0,
            systemBars.right,
            0
        )
    }

    private fun handleSignOut() {
        UserAuthRepository.signOut()
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

    private fun initInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            systemInsets = insets
            updateRootPadding()
            binding.bottomNavView.setPadding(0, 0, 0, insets.getInsets(WindowInsetsCompat.Type.systemBars()).bottom)
            updateContainerPadding()
            insets
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        initInsets()

        setContentView(binding.root)
        initUser()

        binding.apply {
            navHost = (supportFragmentManager.findFragmentById(R.id.container) as NavHostFragment)
            navController = navHost.navController
            NavigationUI.setupWithNavController(
                navigationBarView = binding.bottomNavView, navController = navController
            )

        }
        val inflater = navHost.navController.navInflater
        val graph = inflater.inflate(R.navigation.main_navigation)

        checkCurrentUser(graph)
        navHost.navController.graph = graph

        initChatBadgeObserving()
        initUpdateManager(updateLauncher)
        initInternetConnectivity()
        observeLevelUp()
        NotificationSchedular.rescheduleToday(this)
    }

    private fun observeLevelUp() {
        lifecycleScope.launch {
            PointsRepository.newLevelUnlockedFlow.collect {
                delay(1000)
                showLevelUpDialog(it)
            }
        }
    }

    private fun checkCurrentUser(graph: NavGraph) {
        if (user.valid.not()) {
            UserAuthRepository.signOut()
            graph.setStartDestination(R.id.authFragment)
        } else if (user.valid) {
            if (user.name.isEmpty()) {
                graph.setStartDestination(R.id.onboarding)
            }
        } else {
            graph.setStartDestination(R.id.authFragment)
        }
    }

    private fun initChatBadgeObserving() {
        lifecycleScope.launch {
            AiChatRepository.newMessagesEvent.collect { advice ->
                if (advice.isNotBlank() && navController.currentDestination?.id != R.id.aiChatFragment) {
                    binding.bottomNavView.post {
                        val badge = binding.bottomNavView.getOrCreateBadge(R.id.aiChatFragment)
                        badge.isVisible = true
                        badge.number = if (badge.number == 0) 1 else badge.number + 1
                    }
                }
            }
        }
        binding.bottomNavView.post {
            navController.addOnDestinationChangedListener { _, destination, _ ->
                if (destination.id == R.id.aiChatFragment) {
                    val badge = binding.bottomNavView.getBadge(R.id.aiChatFragment)
                    if (badge != null) {
                        badge.isVisible = false
                        badge.clearNumber()
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        UserProfileRepository.updateLastSeen()
    }

    private fun initUser() {
        LocalUser.getUser()
        if (LocalUser.user.valid) {
            UserProfileRepository.observeCurrentUser {
                if (it == null || it.valid.not()) {
                    signOut?.run()
                }
            }

            NotificationSchedular.rescheduleToday(this)
        }
    }

    fun showSupportSheet() {
        val dialog = BottomSheetDialog(this, R.style.SheetStyle)
        val bindingDialog = SupportSheetBinding.inflate(
            LayoutInflater.from(this), null, false
        )
        dialog.setContentView(bindingDialog.root)
        bindingDialog.apply {
            callview.setOnClickListener {
                dialog.dismiss()
                PhoneUtils.openPhoneCall(CompanyLinks.PHONE_NUMBER)
            }
            requestButton.setOnClickListener {
                dialog.dismiss()
                CompanyLinks.openLink(
                    this@MainActivity, CompanyLinks.TELEGRAM
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
                if (appUpdateInfo.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE
                    && appUpdateInfo.isUpdateTypeAllowed(
                        AppUpdateType.IMMEDIATE
                    )
                ) {
                    try {
                        appUpdateManager.startUpdateFlowForResult(
                            appUpdateInfo,
                            launcher,
                            AppUpdateOptions.newBuilder(AppUpdateType.IMMEDIATE).build()
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
                }
            }
        }
        connectivityManager.requestNetwork(networkRequest, networkCallback)
    }

    private val internetBottomSheet: BottomSheetDialog by lazy {
        BottomSheetDialog(this).apply {
            val dialogBinding =
                NoInternetDialogBinding.inflate(
                    LayoutInflater.from(
                        this@MainActivity
                    ), null, false
                )
            setContentView(dialogBinding.root)
            dialogBinding.button.setOnClickListener {
                dismiss()
            }
            setCancelable(false)
            setCanceledOnTouchOutside(false)
        }
    }

    private var bottomNavShown = true

    private val decelerateInterpolator = DecelerateInterpolator(1.5f)
    fun showBottomSheet(show: Boolean, animate: Boolean, justInvisible: Boolean = false) {
        if (bottomNavShown == show && binding.bottomNavView.isVisible == show) return
        bottomNavShown = show
        updateContainerPadding()
        if (show.not() && justInvisible) {
            binding.bottomNavView.isInvisible = false
            return
        }
        binding.bottomNavView.post {
            binding.bottomNavView.apply {
                if (show && !isVisible) {
                    visibleOrGone(true)
                }

                val h = if (measuredHeight > 0) measuredHeight.toFloat() else 200f
                val targetY = if (show) 0f else h

                if (animate) {
                    if (show && translationY == 0f) translationY = h

                    animate().setDuration(220).setInterpolator(decelerateInterpolator)
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



    fun requestAppReview() {
        try {
            val manager = ReviewManagerFactory.create(this)
            val request = manager.requestReviewFlow()
            request.addOnCompleteListener { task ->
                if (isFinishing || isDestroyed) return@addOnCompleteListener
                if (task.isSuccessful) {
                    val reviewInfo = task.result
                    manager.launchReviewFlow(this, reviewInfo)
                }
            }
        } catch (e: Exception) {
            handleException(e)
        }
    }

    fun updateContainerPadding() {
        val insets = systemInsets ?: return
        val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
        val show = bottomNavShown
        binding.container.setPadding(
            0,
            if (topInsetPaddingEnabled) systemBars.top else 0,
            0,
            if (show) 0 else systemBars.bottom
        )
    }

    private fun showLevelUpDialog(level: Level) {
        val binding = DialogLevelUpBinding.inflate(layoutInflater).apply {
            textLevelEmoji.text = level.emoji
            textLevelName.text = level.name
            textLevelMotivation.text = level.motivationText
        }
        val dialog = AlertDialog.Builder(
            this,
            R.style.RoundedCornersDialog
        ).apply {
            setView(binding.root)
        }.create()
        binding.btnLevelUpOk.setOnClickListener {
            dialog.dismiss()
        }
        dialog.show()
    }
}