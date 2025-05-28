package uz.oybek.ozamiz

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import com.google.firebase.FirebaseApp
import com.google.firebase.appcheck.FirebaseAppCheck
import com.google.firebase.appcheck.debug.DebugAppCheckProviderFactory
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.firestoreSettings
import com.google.firebase.firestore.persistentCacheSettings
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

lateinit var appContext: Context

class MyApplication : Application() {

    companion object {
        const val REMINDER_CHANNEL_ID = "ozamiz_reminder_channel"
    }

    @OptIn(DelicateCoroutinesApi::class)
    override fun onCreate() {
        super.onCreate()
        appContext = this

        GlobalScope.launch(Dispatchers.IO) {
            initFirebase()
        }
        createNotificationChannels()
    }

    private fun initFirebase() {
        FirebaseDatabase.getInstance().setPersistenceEnabled(true)
        FirebaseApp.initializeApp(this)

        val settings = firestoreSettings {
            setLocalCacheSettings(persistentCacheSettings {})
        }
        FirebaseFirestore.getInstance().firestoreSettings = settings

        FirebaseAppCheck.getInstance().installAppCheckProviderFactory(
            DebugAppCheckProviderFactory.getInstance()
        )
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val reminderChannel = NotificationChannel(
                REMINDER_CHANNEL_ID,
                "Eslatmalar",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Ovqatlanish, suv ichish va mashqlar uchun eslatmalar"
                enableLights(true)
                enableVibration(true)
                // lockscreenVisibility = Notification.VISIBILITY_PUBLIC // Handled by MainActivity
            }

            val notificationManager: NotificationManager =
                getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(reminderChannel)
        }
    }
}