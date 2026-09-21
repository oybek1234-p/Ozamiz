package uz.oybek.ozamiz

import android.app.Application
import android.content.Context
import com.facebook.FacebookSdk
import com.google.firebase.BuildConfig
import com.google.firebase.FirebaseApp
import com.google.firebase.appcheck.FirebaseAppCheck
import com.google.firebase.appcheck.debug.DebugAppCheckProviderFactory
import com.google.firebase.appcheck.playintegrity.PlayIntegrityAppCheckProviderFactory
import com.google.firebase.firestore.firestoreSettings
import com.google.firebase.firestore.persistentCacheSettings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.serialization.descriptors.StructureKind
import uz.oybek.ozamiz.data.firebase.database
import uz.oybek.ozamiz.data.firebase.firestore
import uz.oybek.ozamiz.data.motivation.NotificationSchedular
import uz.oybek.ozamiz.data.motivation.MotivationNotificationManager
import uz.oybek.ozamiz.data.user.local.LocalUser
import uz.oybek.ozamiz.data.ads.AdsManager
import uz.oybek.ozamiz.util.handleException

import dagger.hilt.android.HiltAndroidApp

lateinit var appContext: Context

val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

@HiltAndroidApp
class Application : Application() {


    override fun onCreate() {
        super.onCreate()
        appContext = this

        initFirebase()
        AdsManager.initialize(this)

        try {
            FacebookSdk.fullyInitialize()
            FacebookSdk.setAdvertiserIDCollectionEnabled(true)
        } catch (e: Exception) {
            handleException(e)
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

        val settings = firestoreSettings {
            setLocalCacheSettings(persistentCacheSettings {})
        }
        firestore.firestoreSettings = settings
        runCatching {
            database.setPersistenceEnabled(true)
        }
    }
}
