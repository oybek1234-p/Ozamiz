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
import uz.oybek.ozamiz.R
import uz.oybek.ozamiz.ui.MainActivity
import uz.oybek.ozamiz.ui.utils.hideSoftInput

abstract class BaseFragment<T : ViewBinding> : Fragment() {

    abstract fun viewCreated(bind: T)

    var bindingT: T? = null
    val binding: T
        get() = bindingT!!

    fun mainActivity(): MainActivity? = activity as? MainActivity

    var showBottomSheet: Boolean = false

    var transitionAnimating: Boolean = false
    open fun onTransitionEnd() {}
    open fun onInternetAvailable() {}

    abstract fun inflateBinding(inflater: LayoutInflater, container: ViewGroup?): T

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        bindingT = inflateBinding(inflater, container)
        return bindingT?.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        bindingT?.let { viewCreated(it) }
    }

    override fun onResume() {
        super.onResume()
        mainActivity()?.showBottomSheet(showBottomSheet, true)
    }

    override fun onPause() {
        activity?.let { hideSoftInput(it) }
        super.onPause()
    }

    override fun onDestroyView() {
        bindingT = null
        super.onDestroyView()
    }

    override fun onCreateAnimation(transit: Int, enter: Boolean, nextAnim: Int): Animation? {
        if (nextAnim == 0) return null
        return try {
            val anim = AnimationUtils.loadAnimation(context, nextAnim)
            anim?.also {
                it.setAnimationListener(object : Animation.AnimationListener {
                    override fun onAnimationStart(animation: Animation?) {
                        transitionAnimating = true
                    }

                    override fun onAnimationEnd(animation: Animation?) {
                        transitionAnimating = false
                        onTransitionEnd()
                    }

                    override fun onAnimationRepeat(animation: Animation?) {}
                })
            }
        } catch (_: Exception) {
            null
        }
    }

    fun navController(): NavController? {
        if (!isAdded || isDetached || view == null) return null
        return try {
            findNavController()
        } catch (_: Exception) {
            null
        }
    }

    fun navigate(
        @IdRes resId: Int,
        args: Bundle? = null,
        navOptions: NavOptions? = animNavOptions
    ) {
        val controller = navController() ?: return
        try {
            val currentDestination = controller.currentDestination
            if (currentDestination != null) {
                val action = currentDestination.getAction(resId)
                if (action != null) {
                    controller.navigate(resId, args, navOptions)
                } else if (currentDestination.id != resId) {
                    controller.navigate(resId, args, navOptions ?: animNavOptions)
                }
            } else {
                controller.navigate(resId, args, navOptions ?: animNavOptions)
            }
        } catch (_: Exception) {
        }
    }

    fun navigate(
        @IdRes resId: Int,
        args: Bundle? = null,
        builder: NavOptions.Builder.() -> Unit
    ) {
        navigate(resId, args, navOptions(builder))
    }

    fun popBackStack(@IdRes destinationId: Int? = null, inclusive: Boolean = false): Boolean {
        val controller = navController() ?: return false
        return try {
            if (destinationId != null) {
                val currentDest = controller.currentDestination
                if (currentDest != null && currentDest.id == destinationId && !inclusive) {
                    true
                } else {
                    controller.popBackStack(destinationId, inclusive)
                }
            } else {
                controller.popBackStack()
            }
        } catch (_: Exception) {
            false
        }
    }

    fun closeFragment(): Boolean = popBackStack()

    fun navigateUp(): Boolean = popBackStack()

    fun navOptions(builder: NavOptions.Builder.() -> Unit = {}): NavOptions {
        return NavOptions.Builder()
            .setEnterAnim(R.anim.fragment_open_anim)
            .setExitAnim(R.anim.fragment_close_anim)
            .setPopEnterAnim(R.anim.fragment_pop_enter)
            .setPopExitAnim(R.anim.fragment_pop_exit)
            .apply(builder)
            .build()
    }

    companion object {
        val animNavOptions: NavOptions = NavOptions.Builder()
            .setEnterAnim(R.anim.fragment_open_anim)
            .setExitAnim(R.anim.fragment_close_anim)
            .setPopEnterAnim(R.anim.fragment_pop_enter)
            .setPopExitAnim(R.anim.fragment_pop_exit)
            .build()
    }
}
