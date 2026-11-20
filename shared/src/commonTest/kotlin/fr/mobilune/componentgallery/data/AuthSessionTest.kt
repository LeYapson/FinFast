package fr.mobilune.componentgallery.data

import fr.mobilune.componentgallery.core.network.AuthSession
import app.cash.turbine.test
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

@OptIn(ExperimentalCoroutinesApi::class)
class AuthSessionTest {
    private val store = InMemoryAccessTokenStore()
    private val session = AuthSession(store)

    // --- Sans Turbine : ce qu'il faut écrire à la main pour observer un SharedFlow ---------------

    @Test
    fun expirationIsEmittedOnceWhenCollectedByHand() = runTest {
        val events = mutableListOf<Unit>()
        // `expirations` n'a pas de replay : si personne n'écoute au moment de expire(), l'événement est perdu.
        // Le dispatcher Unconfined garantit que la collecte est abonnée avant d'appeler expire().
        val collector = launch(UnconfinedTestDispatcher(testScheduler)) {
            session.expirations.toList(events)
        }

        session.expire()

        assertEquals(1, events.size)
        collector.cancel() // sinon runTest attend indéfiniment la fin du flow
    }

    // --- Avec Turbine : même test, sans lancer ni annuler de collecte nous-mêmes ----------------

    @Test
    fun expirationIsEmittedOnce() = runTest {
        session.expirations.test {
            session.expire()

            awaitItem()
            expectNoEvents()
        }
    }

    @Test
    fun expireForgetsTheTokenAndNotifiesCollectors() = runTest {
        session.setAccessToken("secret")
        assertEquals("secret", session.accessToken)

        session.expirations.test {
            session.expire()
            awaitItem()
        }

        assertNull(session.accessToken)
        assertNull(store.read())
    }

    @Test
    fun clearDoesNotEmitAnExpiration() = runTest {
        session.setAccessToken("secret")

        session.expirations.test {
            // Une déconnexion volontaire n'est pas une expiration : l'UI ne doit pas être prévenue.
            session.clear()

            expectNoEvents()
        }
        assertNull(session.accessToken)
    }
}
