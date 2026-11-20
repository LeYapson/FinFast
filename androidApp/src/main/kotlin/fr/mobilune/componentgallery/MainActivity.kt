package fr.mobilune.componentgallery

import fr.mobilune.componentgallery.data.PREFERENCES_FILE_NAME
import fr.mobilune.componentgallery.data.db.DATABASE_FILE_NAME
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.lifecycleScope
import androidx.work.Constraints
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit
import com.mmk.kmpnotifier.KMPNotifier
import com.mmk.kmpnotifier.extensions.onCreateOrOnNewIntent
import com.mmk.kmpnotifier.permission.AndroidPermissionUtil
import com.mmk.kmpnotifier.push.firebase.firebasePushNotifier
import kotlinx.coroutines.launch
import android.app.NotificationManager

class MainActivity : ComponentActivity() {
    // Gère la demande de permission POST_NOTIFICATIONS (Android 13+).
    // Doit être créé ici, avant que l'activité soit démarrée : il enregistre un « activity result launcher ».
    private val permissionUtil = AndroidPermissionUtil(this)

    // Les deux boutons de l'écran Options passent par là (voir NotificationPermissionAccess).
    private val notificationPermission = object : NotificationPermissionAccess {
        override fun isGranted(onResult: (Boolean) -> Unit) {
            val manager = getSystemService(NotificationManager::class.java)
            onResult(manager.areNotificationsEnabled())
        }

        override fun request(onResult: (Boolean) -> Unit) {
            permissionUtil.askNotificationPermission { granted ->
                Log.d("PUSH", "permission notifications accordée = $granted")
                onResult(granted)
            }
        }
    }

    // « Simuler une dépense dans 30 s » : WorkManager exécute l'appel même si l'app est fermée entre-temps.
    private val transactionScheduler = object : SimulatedTransactionScheduler {
        override val isSupported = true

        override fun scheduleIn(delaySeconds: Int) {
            val request = OneTimeWorkRequestBuilder<SimulateTransactionWorker>()
                .setInitialDelay(delaySeconds.toLong(), TimeUnit.SECONDS)
                .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
                .build()
            WorkManager.getInstance(applicationContext).enqueue(request)
            Log.d("PUSH", "dépense simulée programmée dans $delaySeconds s")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        initAndroidAppContext(this)

        // App fermée : un clic sur une notification démarre l'activité avec les données dans l'intent.
        // Seulement au premier démarrage : sans ça, une recréation de l'activité (savedInstanceState non nul)
        // rejouerait ce même clic et rouvrirait « Ma carte » alors que l'utilisateur est ailleurs.
        if (savedInstanceState == null) {
            KMPNotifier.onCreateOrOnNewIntent(intent)
        }

        // Jeton FCM dans Logcat (filtre : tag:PUSH)
        lifecycleScope.launch {
            Log.d("PUSH", "FCM token = ${KMPNotifier.firebasePushNotifier.getToken()}")
        }

        setContent {
            App(
                onExit = { finish() },
                dataStoreFilePath = filesDir.resolve(PREFERENCES_FILE_NAME).absolutePath,
                databaseFilePath = getDatabasePath(DATABASE_FILE_NAME).absolutePath,
                notificationPermission = notificationPermission,
                transactionScheduler = transactionScheduler,
            )
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        KMPNotifier.onCreateOrOnNewIntent(intent)
    }
}

@Preview
@Composable
fun AppAndroidPreview() {
    App()
}