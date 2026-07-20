package uz.oybek.ozamiz.ui.utils

import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/**
 * Kotlin extensions for Fragment operations.
 * Simplifies common patterns like flow collection.
 */

/**
 * Collects a Flow in a lifecycle-aware manner.
 * Collection starts in STARTED state and stops when lifecycle goes below STARTED.
 */
inline fun <T> Fragment.collectFlow(
    flow: Flow<T>,
    crossinline action: suspend (T) -> Unit
) {
    viewLifecycleOwner.lifecycleScope.launch {
        repeatOnLifecycle(Lifecycle.State.STARTED) {
            flow.collect { action(it) }
        }
    }
}

/**
 * Collects a StateFlow and gets the current value immediately.
 */
inline fun <T> Fragment.collectLatestFlow(
    flow: StateFlow<T>,
    crossinline action: suspend (T) -> Unit
) {
    viewLifecycleOwner.lifecycleScope.launch {
        repeatOnLifecycle(Lifecycle.State.STARTED) {
            flow.collect { action(it) }
        }
    }
}

/**
 * Launches a coroutine in the viewLifecycleOwner scope.
 */
inline fun Fragment.launchWhenStarted(crossinline block: suspend () -> Unit) {
    viewLifecycleOwner.lifecycleScope.launch {
        repeatOnLifecycle(Lifecycle.State.STARTED) {
            block()
        }
    }
}

fun Fragment.toast(message: String) {
    activity?.let {
        uz.oybek.ozamiz.ui.utils.showToast(message, it)
    }
}

fun android.content.Context.triggerVibration(durationMs: Long = 50) {
    try {
        val vibrator = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            (getSystemService(android.content.Context.VIBRATOR_MANAGER_SERVICE) as? android.os.VibratorManager)?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            getSystemService(android.content.Context.VIBRATOR_SERVICE) as? android.os.Vibrator
        }
        if (vibrator?.hasVibrator() == true) {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                vibrator.vibrate(
                    android.os.VibrationEffect.createOneShot(
                        durationMs,
                        android.os.VibrationEffect.DEFAULT_AMPLITUDE
                    )
                )
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(durationMs)
            }
        }
    } catch (_: Exception) {
    }
}

fun Fragment.triggerVibration(durationMs: Long = 50) {
    context?.triggerVibration(durationMs)
}

fun Fragment.playSuccessSound() {
    context?.let { ctx ->
        try {
            val mediaPlayer = android.media.MediaPlayer.create(ctx.applicationContext, uz.oybek.ozamiz.R.raw.success)
            mediaPlayer?.setOnCompletionListener { mp ->
                mp.release()
            }
            mediaPlayer?.start()
        } catch (_: Exception) {
        }
    }
}

