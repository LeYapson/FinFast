package fr.mobilune.componentgallery.data

import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.receiveAsFlow

/**
 * Pont entre les notifications push (reçues par la plateforme, parfois avant même que l'interface
 * existe) et l'interface : « nouvelle dépense » reçue, ou clic sur la notification.
 */
object PushEvents {
    // Un clic sur la notification peut arriver à froid (app fermée), avant que l'écran écoute :
    // le canal garde la demande jusqu'à ce que l'interface connectée la consomme, une seule fois.
    private val mutableOpenCardRequests = Channel<Unit>(Channel.CONFLATED)

    /** L'utilisateur a touché une notification de nouvelle dépense : ouvrir « Ma carte », à jour. */
    val openCardRequests: Flow<Unit> = mutableOpenCardRequests.receiveAsFlow()

    private val mutableNewTransactions = MutableSharedFlow<Unit>(extraBufferCapacity = 1)

    /** Une notification de nouvelle dépense vient d'arriver, app ouverte : recharger les transactions. */
    val newTransactions: SharedFlow<Unit> = mutableNewTransactions.asSharedFlow()

    /** Appelé quand une notification est touchée. [data] : données (payload) de la notification. */
    fun onNotificationClicked(data: Map<String, *>) {
        if (data.isNewTransactionPayload()) mutableOpenCardRequests.trySend(Unit)
    }

    /** Appelé quand une notification push est reçue app ouverte (premier plan). */
    fun onPushReceived(data: Map<String, *>) {
        if (data.isNewTransactionPayload()) mutableNewTransactions.tryEmit(Unit)
    }
}

// Clés ajoutées par Firebase / Android / KMPNotifier : elles ne viennent pas de notre serveur.
private val TECHNICAL_KEY_PREFIXES = listOf("google.", "gcm.", "android.", "com.mmk.")
private val TECHNICAL_KEYS = setOf("from", "collapse_key", "message_type")

/**
 * Une notification de nouvelle dépense porte des données applicatives (`data`) du serveur ;
 * une notification ordinaire (`POST /push/send` sans `data`) n'en porte pas et ne déclenche rien.
 */
fun Map<String, *>.isNewTransactionPayload(): Boolean =
    keys.any { key -> key !in TECHNICAL_KEYS && TECHNICAL_KEY_PREFIXES.none { key.startsWith(it) } }
