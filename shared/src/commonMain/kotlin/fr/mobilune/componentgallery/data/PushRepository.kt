package fr.mobilune.componentgallery.data

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

interface PushRepository {
    /** Demande au serveur d'envoyer une notification de test à l'appareil qui possède ce jeton FCM. */
    suspend fun sendTestPush(deviceToken: String): PushSendResult

    /** Notifie tous les appareils de l'utilisateur [userId] (soi-même ou un autre, par exemple son binôme). */
    suspend fun sendPush(
        userId: String,
        title: String,
        body: String,
        data: Map<String, String> = emptyMap(),
    ): PushSendResult

    /**
     * Enregistre le jeton FCM actuel de cet appareil pour l'utilisateur connecté.
     * À appeler après la connexion. Renvoie `false` si rien n'a pu être fait (pas de session, jeton indisponible).
     */
    suspend fun registerCurrentDevice(): Boolean

    /**
     * Le jeton FCM a changé (`onNewToken`) : on l'enregistre si une session est active.
     * Sans session, la connexion suivante enregistrera le jeton courant (voir [registerCurrentDevice]).
     */
    suspend fun onNewToken(token: String): Boolean

    /** Appareils enregistrés par l'utilisateur connecté. */
    suspend fun listDevices(): List<PushDevice>
}

class PushRepositoryImpl(
    private val pushApi: PushApi,
    private val authSession: AuthSession,
    private val getDeviceToken: suspend () -> String?,
    private val platform: String,
) : PushRepository {
    private val mutex = Mutex()

    // (jeton d'accès, jeton FCM) du dernier enregistrement réussi : évite de renvoyer la même chose à chaque retour à l'accueil.
    // Un autre compte (autre jeton d'accès) ou un nouveau jeton FCM déclenche bien un nouvel enregistrement.
    private var lastRegistered: Pair<String, String>? = null

    override suspend fun sendTestPush(deviceToken: String): PushSendResult = pushApi.sendTestPush(deviceToken)

    override suspend fun sendPush(
        userId: String,
        title: String,
        body: String,
        data: Map<String, String>,
    ): PushSendResult = pushApi.sendPush(userId, title, body, data)

    override suspend fun registerCurrentDevice(): Boolean {
        val deviceToken = getDeviceToken() ?: return false
        return register(deviceToken)
    }

    override suspend fun onNewToken(token: String): Boolean = register(token)

    override suspend fun listDevices(): List<PushDevice> = pushApi.listDevices()

    private suspend fun register(deviceToken: String): Boolean {
        if (deviceToken.isBlank()) return false
        return mutex.withLock {
            val accessToken = authSession.accessToken ?: return@withLock false
            if (lastRegistered != accessToken to deviceToken) {
                pushApi.registerDevice(deviceToken, platform)
                lastRegistered = accessToken to deviceToken
            }
            true
        }
    }
}
