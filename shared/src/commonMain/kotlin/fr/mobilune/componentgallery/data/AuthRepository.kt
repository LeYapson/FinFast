package fr.mobilune.componentgallery.data

data class UserProfile(
    val userId: String,
    val displayedName: String,
    val balanceCents: Long,
)

interface AuthRepository {
    suspend fun register(userId: String, code: String, displayedName: String)

    suspend fun login(userId: String, code: String)

    suspend fun getCurrentUser(): UserProfile

    suspend fun logout()

    suspend fun sendMoney(userId: String, amountCents: Long)

    suspend fun receiveMoney(userId: String, amountCents: Long)
}
