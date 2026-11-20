package fr.mobilune.componentgallery.data

import fr.mobilune.componentgallery.core.network.AccessTokenStore

/** [AccessTokenStore] en mémoire : remplace le chiffrement Keystore d'Android. */
class InMemoryAccessTokenStore(var token: String? = null) : AccessTokenStore {
    override suspend fun read(): String? = token
    override suspend fun write(token: String) { this.token = token }
    override suspend fun clear() { token = null }
}
