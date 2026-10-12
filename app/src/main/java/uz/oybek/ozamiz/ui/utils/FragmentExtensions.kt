package uz.oybek.ozamiz.ui.utils

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import uz.oybek.ozamiz.ui.common.dialog.FullScreenImageDialog
import androidx.core.net.toUri

inline fun <T> Fragment.collectFlowOnStart(
    flow: Flow<T>,
    crossinline action: suspend (T) -> Unit
) {
    viewLifecycleOwner.lifecycleScope.launch {
        repeatOnLifecycle(Lifecycle.State.STARTED) {
            flow.collect { action(it) }
        }
    }
}

fun Fragment.showFullScreenImage(imageUrl: String?) {
    if (imageUrl.isNullOrBlank()) return
    FullScreenImageDialog.show(parentFragmentManager, imageUrl)
}

fun Context.openPlayStore(packageName: String = this.packageName) {
    try {
        val intent = Intent(
            Intent.ACTION_VIEW,
            "market://details?id=$packageName".toUri()
        ).apply {
            setPackage("com.android.vending")
            addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY or Intent.FLAG_ACTIVITY_MULTIPLE_TASK)
        }
        startActivity(intent)
    } catch (_: Exception) {
        try {
            val webIntent = Intent(
                Intent.ACTION_VIEW,
                "https://play.google.com/store/apps/details?id=$packageName".toUri()
            ).apply {
                setPackage("com.android.vending")
            }
            startActivity(webIntent)
        } catch (_: Exception) {
            try {
                startActivity(
                    Intent(
                        Intent.ACTION_VIEW,
                        "https://play.google.com/store/apps/details?id=$packageName".toUri()
                    )
                )
            } catch (_: Exception) {
            }
        }
    }
}

fun Fragment.openAppInPlayStore(packageName: String = requireContext().packageName) {
    context?.openPlayStore(packageName)
}
