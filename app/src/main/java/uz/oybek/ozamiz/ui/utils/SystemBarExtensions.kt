package uz.oybek.ozamiz.ui.utils

import androidx.annotation.Keep
import android.graphics.Color
import androidx.core.view.WindowCompat
import androidx.fragment.app.Fragment

@Keep
data class SystemBarStyle(
    val statusBarColor: Int,
    val navigationBarColor: Int,
    val lightStatusBars: Boolean,
    val lightNavigationBars: Boolean,
)

fun Fragment.applySystemBarStyle(
    statusBarColor: Int = Color.BLACK,
    navigationBarColor: Int = Color.BLACK,
    lightStatusBars: Boolean = false,
    lightNavigationBars: Boolean = false,
): SystemBarStyle {
    val window = requireActivity().window
    val decorView = window.decorView
    val controller = WindowCompat.getInsetsController(window, decorView)

    val previous = SystemBarStyle(
        statusBarColor = window.statusBarColor,
        navigationBarColor = window.navigationBarColor,
        lightStatusBars = controller.isAppearanceLightStatusBars,
        lightNavigationBars = controller.isAppearanceLightNavigationBars,
    )
    window.statusBarColor = statusBarColor
    window.navigationBarColor = navigationBarColor
    controller.isAppearanceLightStatusBars = lightStatusBars
    controller.isAppearanceLightNavigationBars = lightNavigationBars
    return previous
}

fun Fragment.restoreSystemBarStyle(style: SystemBarStyle) {
    val activity = activity ?: return
    val window = activity.window
    val decorView = window.decorView
    val controller = WindowCompat.getInsetsController(window, decorView)

    window.statusBarColor = style.statusBarColor
    window.navigationBarColor = style.navigationBarColor
    controller.isAppearanceLightStatusBars = style.lightStatusBars
    controller.isAppearanceLightNavigationBars = style.lightNavigationBars
}
