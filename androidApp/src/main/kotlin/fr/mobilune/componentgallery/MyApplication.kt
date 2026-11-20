package fr.mobilune.componentgallery

import android.app.Application
import android.util.Log
import com.mmk.kmpnotifier.KMPNotifier
import com.mmk.kmpnotifier.notification.PayloadData
import com.mmk.kmpnotifier.notification.configuration.NotificationPlatformConfiguration
import com.mmk.kmpnotifier.push.PushListener
import com.mmk.kmpnotifier.push.firebase.FirebasePush
import com.mmk.kmpnotifier.push.firebase.addPushListener
import fr.mobilune.componentgallery.data.PushEvents

class MyApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        // Doit être appelé avant tout accès au notifier (sinon IllegalStateException).
        KMPNotifier.initialize(
            configuration = NotificationPlatformConfiguration.Android(
                notificationIconResId = R.mipmap.ic_launcher,
                showPushNotification = true,
            ),
            FirebasePush,
        )

        // Les écouteurs sont posés ici, et non dans l'Activity : l'Application existe toujours avant
        // elle, y compris quand un clic sur une notification démarre l'app fermée.
        KMPNotifier.addListener(object : KMPNotifier.Listener {
            override fun onNotificationClicked(data: PayloadData) {
                Log.d(TAG, "clic sur une notification, data = $data")
                PushEvents.onNotificationClicked(data)
            }
        })
        KMPNotifier.addPushListener(object : PushListener {
            override fun onPushNotificationWithPayloadData(title: String?, body: String?, data: PayloadData) {
                Log.d(TAG, "notification reçue (app ouverte) : title=$title body=$body data=$data")
                PushEvents.onPushReceived(data)
            }

            override fun onNewToken(token: String) {
                Log.d(TAG, "onNewToken = $token")
            }
        })
    }

    private companion object {
        const val TAG = "PUSH"
    }
}
