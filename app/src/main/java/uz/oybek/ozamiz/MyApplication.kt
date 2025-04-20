package uz.oybek.ozamiz

import android.app.Application
import android.content.Context
import com.google.firebase.Firebase
import com.google.firebase.appcheck.FirebaseAppCheck
import com.google.firebase.appcheck.debug.DebugAppCheckProviderFactory
import com.google.firebase.initialize
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

lateinit var appContext: Context

class MyApplication : Application() {

    @OptIn(DelicateCoroutinesApi::class)
    override fun onCreate() {
        super.onCreate()
        appContext = this

        GlobalScope.launch(Dispatchers.IO) {
            initFirebase()
//            try {
//                MobileAds.initialize(
//                    appContext
//                )
//            } catch (e: Exception) {
//                handleException(e)
//            }
        }
    }

    private fun initFirebase() {
        try {
            Firebase.initialize(this)
            FirebaseAppCheck.getInstance().installAppCheckProviderFactory(
                DebugAppCheckProviderFactory.getInstance()
            )
        } catch (e: Exception) {
            handleException(e)
        }
    }
}