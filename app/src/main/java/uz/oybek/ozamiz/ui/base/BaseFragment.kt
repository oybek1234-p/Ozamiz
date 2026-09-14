package uz.oybek.ozamiz.ui.base

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import androidx.annotation.IdRes
import androidx.fragment.app.Fragment
import androidx.navigation.NavController
import androidx.navigation.NavOptions
import androidx.navigation.fragment.findNavController
import androidx.viewbinding.ViewBinding
import uz.oybek.ozamiz.ui.MainActivity
import uz.oybek.ozamiz.R
import uz.oybek.ozamiz.data.food.limit.FoodLimitManager
import uz.oybek.ozamiz.data.analytics.AnalyticsManager
import uz.oybek.ozamiz.util.handleException
import uz.oybek.ozamiz.ui.utils.hideSoftInput

abstract class BaseFragment<T : ViewBinding> : Fragment() {

    abstract fun viewCreated(bind: T)

    var bindingT: T? = null
    val binding: T get() = bindingT!!

    fun mainActivity() = if (activity is MainActivity) activity as MainActivity else null

    var showBottomSheet = false

    fun navigate(
        @IdRes resId: Int,
        args: Bundle? = null,
        navOptions: NavOptions? = null
    ) {
        if (!isAdded || isDetached) return
        val navController = try {
            findNavController()
        } catch (e: Exception) {
            null
        } ?: return

        try {
            val currentDestination = navController.currentDestination
            if (currentDestination != null) {
                val action = currentDestination.getAction(resId)
                if (action != null) {
                    navController.navigate(resId, args, navOptions)
                } else if (currentDestination.id != resId) {
                    navController.navigate(resId, args, navOptions ?: animNavOptions)
                }
            } else {
                navController.navigate(resId, args, navOptions ?: animNavOptions)
            }
        } catch (e: Exception) {
            handleException(e)
        }
    }

    fun navigate(
        directions: androidx.navigation.NavDirections,
        navOptions: NavOptions? = null
    ) {
        if (!isAdded || isDetached) return
        val navController = try {
            findNavController()
        } catch (e: Exception) {
            null
        } ?: return

        try {
            val currentDestination = navController.currentDestination
            if (currentDestination != null) {
                val action = currentDestination.getAction(directions.actionId)
                if (action != null) {
                    navController.navigate(directions, navOptions)
                } else {
                    navController.navigate(directions, navOptions ?: animNavOptions)
                }
            } else {
                navController.navigate(directions, navOptions ?: animNavOptions)
            }
        } catch (e: Exception) {
            handleException(e)
        }
    }

    fun popBackStack(): Boolean {
        if (!isAdded || isDetached || view == null) return false
        val navController = try {
            findNavController()
        } catch (e: Exception) {
            null
        } ?: return false

        return try {
            navController.popBackStack()
        } catch (e: Exception) {
            handleException(e)
            false
        }
    }

    fun popBackStack(@IdRes destinationId: Int, inclusive: Boolean = false): Boolean {
        if (!isAdded || isDetached || view == null) return false
        val navController = try {
            findNavController()
        } catch (e: Exception) {
            null
        } ?: return false

        return try {
            val currentDest = navController.currentDestination
            if (currentDest != null && currentDest.id == destinationId && !inclusive) {
                true
            } else {
                navController.popBackStack(destinationId, inclusive)
            }
        } catch (e: Exception) {
            handleException(e)
            false
        }
    }

    fun navigateUp(): Boolean {
        if (!isAdded || isDetached || view == null) return false
        val navController = try {
            findNavController()
        } catch (e: Exception) {
            null
        } ?: return false

        return try {
            navController.navigateUp()
        } catch (e: Exception) {
            handleException(e)
            false
        }
    }

    fun checkLimitAndProceed(onProceed: () -> Unit) {
        if (!isAdded || isDetached) return
        val act = activity ?: return
        FoodLimitManager.checkLimitAndProceed(act) {
            if (isAdded && !isDetached && context != null) {
                onProceed()
            }
        }
    }

    var transitionAnimating = false
    open fun onTransitionEnd() {}

    override fun onCreateAnimation(transit: Int, enter: Boolean, nextAnim: Int): Animation? {
        try {
            val animation =
                super.onCreateAnimation(transit, enter, nextAnim) ?: AnimationUtils.loadAnimation(
                    context, nextAnim
                )

            return animation?.also {
                it.setAnimationListener(object : Animation.AnimationListener {
                    override fun onAnimationEnd(animation: Animation?) {
                        transitionAnimating = false
                        onTransitionEnd()
                    }

                    override fun onAnimationRepeat(animation: Animation?) {

                    }

                    override fun onAnimationStart(animation: Animation?) {
                        transitionAnimating = true
                    }
                })
            }
        } catch (e: Exception) {
            return null
        }
    }

    open fun onInternetAvailable() {

    }

    open val screenName: String get() = this::class.java.simpleName
    private var screenStartTime: Long = 0L

    override fun onResume() {
        super.onResume()
        screenStartTime = System.currentTimeMillis()
        AnalyticsManager.logScreenView(screenName, this::class.java.name)
        mainActivity()?.showBottomSheet(showBottomSheet, true)
    }

    abstract fun inflateBinding(inflater: LayoutInflater, container: ViewGroup?): T

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        bindingT = inflateBinding(inflater, container)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewCreated(binding)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        bindingT = null
    }

    override fun onPause() {
        activity?.let { hideSoftInput(it) }
        if (screenStartTime > 0L) {
            val durationSeconds = (System.currentTimeMillis() - screenStartTime) / 1000L
            AnalyticsManager.logScreenTimeSpent(screenName, durationSeconds)
            screenStartTime = 0L
        }
        super.onPause()
    }

    fun closeFragment() {
        popBackStack()
    }

    companion object {
        val animNavOptions = NavOptions.Builder().setEnterAnim(R.anim.fragment_open_anim)
            .setExitAnim(R.anim.fragment_close_anim).setPopEnterAnim(R.anim.fragment_pop_enter)
            .setPopExitAnim(R.anim.fragment_pop_exit).build()
    }
}
