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
import uz.oybek.ozamiz.MainActivity
import uz.oybek.ozamiz.R
import uz.oybek.ozamiz.handleException
import uz.oybek.ozamiz.ui.hideSoftInput

abstract class BaseFragment<T : ViewBinding> : Fragment() {

    abstract fun viewCreated(bind: T)

    var bindingT: T? = null
    val binding: T get() = bindingT!!

    fun mainActivity() = if (activity is MainActivity) activity as MainActivity else null

    var showBottomSheet = false


    fun navigate(@IdRes resId: Int, args: Bundle? = null) {
        var navController: NavController? = null
        try {
            navController = findNavController()
        } catch (e: Exception) {
            //
        }
        try {
            navController?.navigate(
                resId,
                args,
                navOptions = animNavOptions,
            )
        } catch (e: Exception) {
            handleException(e)
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

    override fun onResume() {
        super.onResume()
        mainActivity()?.showBottomSheet(showBottomSheet, true)
    }

    abstract fun inflateBinding(inflater: LayoutInflater, container: ViewGroup?): T

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        bindingT = inflateBinding(inflater, container)
        viewCreated(binding)
        return binding.root
    }

    override fun onPause() {
        hideSoftInput(requireActivity())
        super.onPause()
    }

    fun closeFragment() {
        var navController: NavController? = null
        try {
            navController = findNavController()
        } catch (e: Exception) {
            //Ignore
        }
        try {
            if (isAdded && isResumed) {
                navController?.popBackStack()
            }
        } catch (e: Exception) {
            handleException(e)
        }
    }

    companion object {
        val animNavOptions = NavOptions.Builder().setEnterAnim(R.anim.fragment_open_anim)
            .setExitAnim(R.anim.fragment_close_anim).setPopEnterAnim(R.anim.fragment_pop_enter)
            .setPopExitAnim(R.anim.fragment_pop_exit).build()
    }
}
