package fr.mobilune.componentgallery

import com.mmk.kmpnotifier.KMPNotifier
import com.mmk.kmpnotifier.push.PushListener
import com.mmk.kmpnotifier.push.firebase.addPushListener
import com.mmk.kmpnotifier.push.firebase.firebasePushNotifier
import com.mmk.kmpnotifier.push.firebase.removePushListener
import kotlinx.coroutines.CancellationException

/**
 * Accès à la permission « notifications » du système. La demande doit partir d'une Activity
 * sur Android : elle est donc fournie par la plateforme (voir `MainActivity`) et passée à [App].
 */
interface NotificationPermissionAccess {
    /** Indique si l'utilisateur a déjà autorisé les notifications. */
    fun isGranted(onResult: (Boolean) -> Unit)

    /** Affiche la fenêtre d'autorisation si nécessaire, puis donne le résultat. */
    fun request(onResult: (Boolean) -> Unit)

    companion object {
        /** Plateforme sans notifications push (ex. desktop) : tout est refusé. */
        val Unsupported: NotificationPermissionAccess = object : NotificationPermissionAccess {
            override fun isGranted(onResult: (Boolean) -> Unit) = onResult(false)
            override fun request(onResult: (Boolean) -> Unit) = onResult(false)
        }
    }
}

/**
 * Prévient [callback] à chaque nouveau jeton FCM (`onNewToken`), y compris le premier.
 * Renvoie la fonction qui arrête l'écoute ; sans effet sur les plateformes sans push (desktop).
 */
fun observePushTokens(callback: (String) -> Unit): () -> Unit {
    val listener = object : PushListener {
        override fun onNewToken(token: String) = callback(token)
    }
    try {
        KMPNotifier.addPushListener(listener)
    } catch (exception: Exception) {
        return {}
    }
    return {
        try {
            KMPNotifier.removePushListener(listener)
        } catch (_: Exception) {
            // L'écoute s'arrête avec l'application de toute façon.
        }
    }
}

/** Jeton FCM de cet appareil, ou `null` s'il est indisponible (desktop, Firebase non initialisé…). */
suspend fun fetchDevicePushToken(): String? = try {
    KMPNotifier.firebasePushNotifier.getToken()
} catch (exception: CancellationException) {
    throw exception
} catch (exception: Exception) {
    null
}
