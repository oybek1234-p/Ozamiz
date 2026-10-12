package uz.oybek.ozamiz.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.animation.DecelerateInterpolator
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.annotation.IdRes
import androidx.appcompat.app.AppCompatActivity
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isInvisible
import androidx.core.view.isVisible
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.navOptions
import androidx.navigation.ui.NavigationUI
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.play.core.review.ReviewManagerFactory
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import uz.oybek.ozamiz.R
import uz.oybek.ozamiz.data.motivation.NotificationSchedular
import uz.oybek.ozamiz.data.notification.FcmTokenManager
import uz.oybek.ozamiz.data.utils.CompanyLinks
import uz.oybek.ozamiz.databinding.ActivityMainBinding
import uz.oybek.ozamiz.databinding.NoInternetDialogBinding
import uz.oybek.ozamiz.databinding.SupportSheetBinding
import uz.oybek.ozamiz.notification.NotificationDisplayManager
import uz.oybek.ozamiz.ui.base.BaseFragment
import uz.oybek.ozamiz.ui.utils.showAppSnackBar
import uz.oybek.ozamiz.ui.utils.visibleOrGone
import uz.oybek.ozamiz.util.PhoneUtils
import uz.oybek.ozamiz.util.connectivity.ConnectivityObserver
import uz.oybek.ozamiz.util.handleException
import uz.oybek.ozamiz.util.update.AppUpdateHelper
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    private val viewModel: MainViewModel by viewModels()

    @Inject
    lateinit var connectivityObserver: ConnectivityObserver

    @Inject
    lateinit var appUpdateHelper: AppUpdateHelper

    @Inject
    lateinit var fcmTokenManager: FcmTokenManager

    lateinit var navController: NavController
    private lateinit var navHost: NavHostFragment

    private lateinit var binding: ActivityMainBinding
    private var systemInsets: WindowInsetsCompat? = null
    private var topInsetPaddingEnabled = true

    private fun updateRootPadding() {
        val insets = systemInsets ?: return
        val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
        binding.root.setPadding(
            systemBars.left, 0, systemBars.right, 0
        )
    }

    private val updateLauncher = registerForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult()
    ) {

    }

    private fun initInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            systemInsets = insets
            updateRootPadding()
            binding.bottomNavView.setPadding(
                0, 0, 0, insets.getInsets(WindowInsetsCompat.Type.systemBars()).bottom
            )
            updateContainerPadding()
            insets
        }
    }

    private fun setupNavGraph() {
        binding.apply {
            navHost = (supportFragmentManager.findFragmentById(R.id.container) as NavHostFragment)
            navController = navHost.navController
            NavigationUI.setupWithNavController(
                navigationBarView = binding.bottomNavView, navController = navController
            )
        }
        val inflater = navHost.navController.navInflater
        val graph = inflater.inflate(R.navigation.main_navigation)
        graph.setStartDestination(viewModel.getStartDestination())
        navHost.navController.graph = graph
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        initInsets()

        setContentView(binding.root)
        setupNavGraph()

        observeInternetConnectivity()
        observeAuthState()
        appUpdateHelper.checkForAppUpdate(this, updateLauncher)
        fcmTokenManager.syncToken()
    }

    private fun observeAuthState() {
        lifecycleScope.launch {
            viewModel.navigationCommand.collect { command ->
                when (command) {
                    is MainNavCommand.NavigateToAuth -> {
                        navigateToRootDestination(R.id.authFragment)
                    }

                    is MainNavCommand.NavigateToHome -> {
                        val currentId = navController.currentDestination?.id
                        if (currentId == R.id.authFragment || currentId == R.id.onboarding) {
                            navigateToRootDestination(R.id.home)
                        }
                    }

                    is MainNavCommand.NavigateToOnboarding -> {
                        val currentId = navController.currentDestination?.id
                        if (currentId == R.id.authFragment) {
                            navigateToRootDestination(R.id.onboarding)
                        }
                    }
                }
            }
        }
    }

    private fun navigateToRootDestination(@IdRes targetDestId: Int) {
        if (navController.currentDestination?.id == targetDestId) return

        navController.navigate(
            targetDestId, null, navOptions {
                popUpTo(R.id.main_navigation) {
                    inclusive = true
                }
                launchSingleTop = true
            })
    }

    override fun onResume() {
        super.onResume()

        viewModel.updateLastSeenTime()
        viewModel.checkPremium()
        NotificationSchedular.rescheduleToday(this)
        appUpdateHelper.completeUpdateIfDownloaded()
    }

    private fun observeInternetConnectivity() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                connectivityObserver.observe().collect { isConnected ->
                    if (isConnected) {
                        internetBottomSheet.dismiss()
                        (navHost.childFragmentManager.fragments.firstOrNull() as? BaseFragment<*>)?.onInternetAvailable()
                    } else {
                        internetBottomSheet.show()
                    }
                }
            }
        }
    }

    fun showSnackbar(message: String) {
        binding.root.showAppSnackBar(message)
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

    private val internetBottomSheet: BottomSheetDialog by lazy {
        BottomSheetDialog(this).apply {
            val dialogBinding = NoInternetDialogBinding.inflate(
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
        binding.bottomNavDivider.isVisible = show
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
        val imeInsets = insets.getInsets(WindowInsetsCompat.Type.ime())
        val imeBottom = imeInsets.bottom
        val systemBottom = systemBars.bottom
        val bottomPadding =
            if (imeBottom > 0) imeBottom else if (bottomNavShown) 0 else systemBottom

        binding.container.setPadding(
            0,
            if (topInsetPaddingEnabled) systemBars.top else 0,
            0,
            bottomPadding
        )
    }
}