package uz.oybek.ozamiz

import android.app.Application
import android.content.Context
import com.google.firebase.BuildConfig
import com.google.firebase.FirebaseApp
import com.google.firebase.appcheck.FirebaseAppCheck
import com.google.firebase.appcheck.debug.DebugAppCheckProviderFactory
import com.google.firebase.appcheck.playintegrity.PlayIntegrityAppCheckProviderFactory
import com.google.firebase.database.FirebaseDatabase
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import uz.oybek.ozamiz.data.ads.AdsManager
import uz.oybek.ozamiz.data.motivation.MotivationNotificationManager
import uz.oybek.ozamiz.util.handleException

lateinit var appContext: Context

val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

@HiltAndroidApp
class Application : Application() {

    override fun onCreate() {
        super.onCreate()
        appContext = this

        initFirebase()

        applicationScope.launch(Dispatchers.Default) {
            AdsManager.initialize(this@Application)
        }

        val appCheckProviderFactory = if (BuildConfig.DEBUG) {
            DebugAppCheckProviderFactory.getInstance()
        } else {
            PlayIntegrityAppCheckProviderFactory.getInstance()
        }
        FirebaseAppCheck.getInstance().installAppCheckProviderFactory(
            appCheckProviderFactory
        )

    }

    private fun initFirebase() {
        FirebaseApp.initializeApp(this)
        MotivationNotificationManager(this).createNotificationChannel()
    }
}
