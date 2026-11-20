package fr.mobilune.componentgallery.data

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/** Quelles notifications comptent comme « nouvelle dépense » (et donc ouvrent / rafraîchissent Ma carte). */
class PushEventsTest {
    @Test
    fun `Une notification avec des donnees du serveur est une nouvelle depense`() {
        assertTrue(mapOf("type" to "transaction", "amountCents" to "1250").isNewTransactionPayload())
    }

    @Test
    fun `Une notification sans donnees ne declenche rien`() {
        assertFalse(emptyMap<String, Any?>().isNewTransactionPayload())
    }

    @Test
    fun `Les cles techniques ajoutees par Firebase et la bibliotheque sont ignorees`() {
        val technical = mapOf(
            "google.sent_time" to 1L,
            "google.message_id" to "0:abc",
            "gcm.n.e" to "1",
            "from" to "123456",
            "collapse_key" to "fr.mobilune.finfast",
            "com.mmk.kmpnotifier.notification.ACTION_NOTIFICATION_CLICK" to "x",
        )
        assertFalse(technical.isNewTransactionPayload())
    }

    @Test
    fun `Une donnee du serveur est reconnue meme parmi des cles techniques`() {
        assertTrue((mapOf("google.sent_time" to 1L, "from" to "1") + ("transactionId" to "42")).isNewTransactionPayload())
    }
}
