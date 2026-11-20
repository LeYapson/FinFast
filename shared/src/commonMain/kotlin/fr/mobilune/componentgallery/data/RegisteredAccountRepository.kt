package fr.mobilune.componentgallery.data

interface RegisteredAccountRepository {
    suspend fun isRegistered(userId: String): Boolean

    suspend fun markRegistered(userId: String)
}
