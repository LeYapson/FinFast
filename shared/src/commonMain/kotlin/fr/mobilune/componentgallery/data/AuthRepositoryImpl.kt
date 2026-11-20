package fr.mobilune.componentgallery.data

import fr.mobilune.componentgallery.core.network.AuthApi
import fr.mobilune.componentgallery.core.network.AuthSession

/** Les appels HTTP sont dans [AuthApi] ; ici on garde la session (jeton, nom affiché) et on adapte les modèles. */
class AuthRepositoryImpl(
    private val authApi: AuthApi,
    private val authSession: AuthSession,
) : AuthRepository {
    override suspend fun register(userId: String, code: String, displayedName: String) {
        authApi.register(userId, code, displayedName)
    }

    override suspend fun login(userId: String, code: String) {
        authSession.setAccessToken(authApi.login(userId, code))
        getCurrentUser()
    }

    override suspend fun getCurrentUser(): UserProfile {
        val user = authApi.currentUser()
        authSession.setDisplayedName(user.displayedName)
        return UserProfile(
            userId = user.userId,
            displayedName = user.displayedName,
            balanceCents = user.balanceCents,
        )
    }

    override suspend fun logout() {
        authApi.logout()
        authSession.clear()
    }

    override suspend fun sendMoney(userId: String, amountCents: Long) {
        authApi.sendMoney(userId, amountCents)
    }

    override suspend fun receiveMoney(userId: String, amountCents: Long) {
        authApi.receiveMoney(userId, amountCents)
    }
}
