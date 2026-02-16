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
