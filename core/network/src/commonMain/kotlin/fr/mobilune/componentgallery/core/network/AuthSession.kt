package fr.mobilune.componentgallery.core.network

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

interface AccessTokenStore {
    suspend fun read(): String?
    suspend fun write(token: String)
    suspend fun clear()
}

/** Le serveur a refusé le jeton (401/403) : la session n'est plus valable. */
class SessionExpiredException : IllegalStateException("Ta session a expiré. Reconnecte-toi.")

class AuthSession(
    private val accessTokenStore: AccessTokenStore? = null,
) {
    private val mutableExpirations = MutableSharedFlow<Unit>(extraBufferCapacity = 1)

    /** Émet chaque fois que le serveur rejette le jeton ; l'UI doit alors renvoyer vers la connexion. */
    val expirations: SharedFlow<Unit> = mutableExpirations.asSharedFlow()

    var accessToken: String? = null
        private set

    var displayedName: String? = null
        private set

    suspend fun restoreAccessToken() {
        accessToken = accessTokenStore?.read()
    }

    suspend fun setAccessToken(token: String) {
        accessTokenStore?.write(token)
        accessToken = token
    }

    fun setDisplayedName(name: String) {
        displayedName = name
    }

    suspend fun clear() {
        accessTokenStore?.clear()
        accessToken = null
        displayedName = null
    }

    /** Le jeton est rejeté par le serveur : on l'oublie et on prévient l'UI. */
    suspend fun expire() {
        clear()
        mutableExpirations.tryEmit(Unit)
    }
}
