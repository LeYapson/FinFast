package fr.mobilune.componentgallery.data

import fr.mobilune.componentgallery.core.network.AuthSession
import fr.mobilune.componentgallery.core.network.PushApi
import fr.mobilune.componentgallery.core.network.PushDevice
import fr.mobilune.componentgallery.core.network.PushSendResult
import fr.mobilune.componentgallery.core.network.SimulateTransactionResult
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

private class FakePushApi : PushApi {
    val registered = mutableListOf<Pair<String, String>>()

    override suspend fun registerDevice(token: String, platform: String) {
        registered += token to platform
    }

    override suspend fun listDevices(): List<PushDevice> = emptyList()

    override suspend fun sendTestPush(deviceToken: String): PushSendResult = PushSendResult(sent = 1, failed = 0)

    override suspend fun sendPush(
        userId: String,
        title: String,
        body: String,
        data: Map<String, String>,
    ): PushSendResult = PushSendResult(sent = 1, failed = 0)

    override suspend fun simulateTransaction(): SimulateTransactionResult =
        SimulateTransactionResult(transactionId = "tx-1", sent = 1)
}

/** Enregistrement de l'appareil : après la connexion et à chaque nouveau jeton FCM. */
class PushRepositoryTest {
    private val api = FakePushApi()
    private val session = AuthSession(InMemoryAccessTokenStore())
    private var currentFcmToken: String? = "fcm-1"

    private val repository = PushRepositoryImpl(
        pushApi = api,
        authSession = session,
        getDeviceToken = { currentFcmToken },
        platform = "android",
    )

    @Test
    fun `Sans session rien n'est envoyé au serveur`() = runTest {
        assertFalse(repository.registerCurrentDevice())
        assertFalse(repository.onNewToken("fcm-2"))
        assertTrue(api.registered.isEmpty())
    }

    @Test
    fun `Après la connexion le jeton courant est enregistré avec la plateforme`() = runTest {
        session.setAccessToken("access-1")
        assertTrue(repository.registerCurrentDevice())
        assertEquals(listOf("fcm-1" to "android"), api.registered)
    }

    @Test
    fun `Le même jeton n'est pas renvoyé à chaque retour à l'accueil`() = runTest {
        session.setAccessToken("access-1")
        repository.registerCurrentDevice()
        repository.registerCurrentDevice()
        assertEquals(1, api.registered.size)
    }

    @Test
    fun `Un nouveau jeton FCM est enregistré`() = runTest {
        session.setAccessToken("access-1")
        repository.registerCurrentDevice()
        assertTrue(repository.onNewToken("fcm-2"))
        assertEquals(listOf("fcm-1", "fcm-2"), api.registered.map { it.first })
    }

    @Test
    fun `Une nouvelle connexion réenregistre le même jeton`() = runTest {
        session.setAccessToken("access-1")
        repository.registerCurrentDevice()
        session.setAccessToken("access-2")
        repository.registerCurrentDevice()
        assertEquals(2, api.registered.size)
    }

    @Test
    fun `Sans jeton FCM disponible rien n'est enregistré`() = runTest {
        session.setAccessToken("access-1")
        currentFcmToken = null
        assertFalse(repository.registerCurrentDevice())
        assertTrue(api.registered.isEmpty())
    }
}
