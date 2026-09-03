package uz.oybek.ozamiz.ui.base

import android.content.Context
import android.util.AttributeSet
import androidx.core.graphics.drawable.toDrawable
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.color.MaterialColors
import uz.oybek.ozamiz.R

class MyToolBar @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet?,
    defS: Int = com.google.android.material.R.attr.toolbarSurfaceStyle
) : MaterialToolbar(
    context,
    attrs,
    defS
) {

    var showArrowBack = false
        set(value) {
            field = value
            if (value) {
                setNavigationIcon(R.drawable.arrow_back)
            } else {
                navigationIcon = null
            }
        }

    fun setUpBackButton(fragment: Fragment) {
        setNavigationOnClickListener {
            if (fragment is BaseFragment<*>) {
                fragment.popBackStack()
            } else {
                try {
                    fragment.findNavController().popBackStack()
                } catch (e: Exception) {
                    // ignore
                }
            }
        }
    }

    init {
        showArrowBack = true
        setTitleTextAppearance(context, R.style.ToolbarTitleApearence)
    }

}