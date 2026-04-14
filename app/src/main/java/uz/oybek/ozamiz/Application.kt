package uz.oybek.ozamiz

import android.app.Application
import android.content.Context
import com.facebook.FacebookSdk
import com.google.firebase.FirebaseApp
import com.google.firebase.appcheck.BuildConfig
import com.google.firebase.appcheck.FirebaseAppCheck
import com.google.firebase.appcheck.debug.DebugAppCheckProviderFactory
import com.google.firebase.appcheck.playintegrity.PlayIntegrityAppCheckProviderFactory
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.firestoreSettings
import com.google.firebase.firestore.persistentCacheSettings
import uz.oybek.ozamiz.data.motivation.MotivationAlarmScheduler
import uz.oybek.ozamiz.data.motivation.MotivationNotificationManager
import uz.oybek.ozamiz.data.user.local.LocalUser
import uz.oybek.ozamiz.util.handleException

lateinit var appContext: Context

class Application : Application() {

    override fun onCreate() {
        super.onCreate()
        appContext = this

        LocalUser.getUser()
        initFirebase()

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
        FirebaseAppCheck.getInstance().installAppCheckProviderFactory(appCheckProviderFactory)
    }

    private fun initFirebase() {
        FirebaseApp.initializeApp(this)
        FirebaseDatabase.getInstance().setPersistenceEnabled(true)

        val settings = firestoreSettings {
            setLocalCacheSettings(persistentCacheSettings {})
        }
        FirebaseFirestore.getInstance().firestoreSettings = settings

        MotivationNotificationManager(this).createNotificationChannel()
        MotivationAlarmScheduler.scheduleAll(this)
    }
}
