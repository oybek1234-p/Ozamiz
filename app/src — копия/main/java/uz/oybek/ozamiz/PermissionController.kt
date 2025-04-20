package uz.oybek.ozamiz
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import androidx.appcompat.app.AppCompatActivity
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch

class PermissionController {

    var permissionResultListener: PermissionResult? = null

    var requestId: Int? = null

    companion object {

        private var INSTANCE: PermissionController? = null

        fun getInstance() =
            if (INSTANCE != null) INSTANCE!! else PermissionController().also { INSTANCE = it }

        const val ANY_REQUEST_CODE = -1

        fun checkPermissions(context: Context, permissions: Array<String>): Array<String>? {
            var shouldRequest: ArrayList<String>? = null

            permissions.forEach {
                if (context.checkSelfPermission(it) == PackageManager.PERMISSION_DENIED) {

                    if (shouldRequest == null) {
                        shouldRequest = ArrayList()
                    }

                    shouldRequest?.add(it)
                }
            }

            return if (shouldRequest == null) {
                null
            } else {
                return shouldRequest!!.toTypedArray()
            }
        }

    }

    fun requestPermissions(
        context: Context,
        requestId: Int,
        permissions: Array<String>,
        resultListener: PermissionResult,
    ) {
        if (context !is Activity || context !is AppCompatActivity) {
            return
        }

        permissionResultListener = resultListener
        this.requestId = requestId

        val shouldRequest = checkPermissions(context, permissions)

        if (shouldRequest == null || shouldRequest.isEmpty()) {

            resultListener.onGranted()

        } else {

            context.requestPermissions(shouldRequest, requestId)
        }
    }

    private var callbacks = hashMapOf<Int,(granted: Boolean) -> Unit>()

    fun doOnResult(requestId: Int,result: (granted: Boolean) -> Unit) {
        callbacks[requestId] = result
    }

    private var activityResultCallbacks = hashMapOf<Int,(any: Any?,resultOk: Boolean) -> Unit>()

    fun doOnActivityResult(requestId: Int,result: (any: Any?,resultOk: Boolean) -> Unit) {
        activityResultCallbacks.put(requestId,result)
    }

    fun onActivityResult(requestCode: Int,any: Any?,ok: Boolean) {
        activityResultCallbacks.forEach {
            try {
                if (it.key == requestCode || it.key == ANY_REQUEST_CODE) {
                    it.value.invoke(any,ok)
                    activityResultCallbacks.remove(requestCode)
                }
            }catch (e: Exception) {
                handleException(e)
            }
        }
    }

    fun removeCallback(requestId: Int) {
        callbacks.remove(requestId)
        activityResultCallbacks.remove(requestId)
    }

    fun onPermissionResult(requestId: Int, grantResults: IntArray) {
        callbacks.forEach {
            if (it.key == requestId) {
                it.value.invoke(!grantResults.contains(PackageManager.PERMISSION_DENIED))
                callbacks.remove(it.key)
            }
        }
        if (this.requestId != requestId) {
            return
        }
        MainScope().launch {
            permissionResultListener?.apply {
                grantResults.forEach {
                    if (it == PackageManager.PERMISSION_DENIED) {
                        onDenied()
                        return@launch
                    }
                }
                onGranted()
                permissionResultListener = null
            }
        }

        this.requestId = -1
    }

    interface PermissionResult {

        fun onGranted()

        fun onDenied()

    }
}