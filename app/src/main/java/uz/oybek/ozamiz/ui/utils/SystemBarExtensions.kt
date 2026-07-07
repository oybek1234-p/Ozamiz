package uz.oybek.ozamiz.ui.utils

import android.graphics.Color
import androidx.core.view.WindowCompat
import androidx.fragment.app.Fragment

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
    val controller = WindowCompat.getInsetsController(window, requireView())
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
    if (!isAdded) return
    val window = requireActivity().window
    val controller = WindowCompat.getInsetsController(window, requireView())
    window.statusBarColor = style.statusBarColor
    window.navigationBarColor = style.navigationBarColor
    controller.isAppearanceLightStatusBars = style.lightStatusBars
    controller.isAppearanceLightNavigationBars = style.lightNavigationBars
}
