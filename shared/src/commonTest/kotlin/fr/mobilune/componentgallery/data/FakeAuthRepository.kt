package fr.mobilune.componentgallery.data

/**
 * Double de test d'[AuthRepository] : aucun réseau, les appels sont enregistrés et chaque opération
 * peut être forcée en échec avec l'erreur correspondante.
 */
class FakeAuthRepository : AuthRepository {
    data class RegisterCall(val userId: String, val code: String, val displayedName: String)
    data class LoginCall(val userId: String, val code: String)
    data class MoneyCall(val userId: String, val amountCents: Long)

    /** Profil renvoyé par [getCurrentUser]. */
    var profile: UserProfile = UserProfile(userId = "user", displayedName = "User", balanceCents = 0)

    var registerError: Throwable? = null
    var loginError: Throwable? = null
    var getCurrentUserError: Throwable? = null
    var logoutError: Throwable? = null
    var sendMoneyError: Throwable? = null
    var receiveMoneyError: Throwable? = null

    val registerCalls = mutableListOf<RegisterCall>()
    val loginCalls = mutableListOf<LoginCall>()
    var getCurrentUserCount = 0
        private set
    var logoutCount = 0
        private set
    val sentMoney = mutableListOf<MoneyCall>()
    val receivedMoney = mutableListOf<MoneyCall>()

    override suspend fun register(userId: String, code: String, displayedName: String) {
        registerCalls += RegisterCall(userId, code, displayedName)
        registerError?.let { throw it }
    }

    override suspend fun login(userId: String, code: String) {
        loginCalls += LoginCall(userId, code)
        loginError?.let { throw it }
    }

    override suspend fun getCurrentUser(): UserProfile {
        getCurrentUserCount++
        getCurrentUserError?.let { throw it }
        return profile
    }

    override suspend fun logout() {
        logoutCount++
        logoutError?.let { throw it }
    }

    override suspend fun sendMoney(userId: String, amountCents: Long) {
        sentMoney += MoneyCall(userId, amountCents)
        sendMoneyError?.let { throw it }
    }

    override suspend fun receiveMoney(userId: String, amountCents: Long) {
        receivedMoney += MoneyCall(userId, amountCents)
        receiveMoneyError?.let { throw it }
    }
}
