package fr.mobilune.componentgallery.viewmodel

import app.cash.turbine.test
import fr.mobilune.componentgallery.core.network.AuthSession
import fr.mobilune.componentgallery.data.FakeAuthRepository
import fr.mobilune.componentgallery.data.FakeTransactionRepository
import fr.mobilune.componentgallery.data.UserProfile
import fr.mobilune.componentgallery.data.fakeTransaction
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull

/**
 * Mêmes scénarios que `CarteViewModelTest`, mais avec Turbine : au lieu de regarder `uiState.value`
 * à un instant donné, on vérifie la séquence des états que l'écran verrait passer.
 *
 * Pas besoin d'`advanceUntilIdle()` avant un `awaitItem()` : tant que le test attend, `runTest`
 * fait tourner le dispatcher de test, donc le ViewModel avance tout seul.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class CarteViewModelTurbineTest {
    private val authRepository = FakeAuthRepository()
    private val transactionRepository = FakeTransactionRepository()
    private val authSession = AuthSession()

    private val profile = UserProfile(userId = "user", displayedName = "Alice", balanceCents = 12_500)
    private val remote = listOf(fakeTransaction("1", -2_000), fakeTransaction("2", 4_500))

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(StandardTestDispatcher())
        authRepository.profile = profile
        transactionRepository.remoteTransactions = remote
        // Sans délai, le fake répond instantanément et le StateFlow fusionne les états intermédiaires.
        transactionRepository.refreshDelayMillis = 100
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel() = CarteViewModel(transactionRepository, authRepository, authSession)

    @Test
    fun emitsLoadingThenTheDashboard() = runTest {
        createViewModel().uiState.test {
            assertEquals(CarteUiState.Loading, awaitItem())

            val dashboard = assertIs<CarteUiState.Dashboard>(awaitItem())
            assertEquals(profile, dashboard.profile)
            assertEquals(remote, dashboard.transactions)
            assertEquals(false, dashboard.isRefreshing)

            // Turbine échoue aussi à la sortie du bloc s'il reste un événement non consommé.
            expectNoEvents()
        }
    }

    @Test
    fun emitsLoadingThenErrorWhenRefreshFailsWithoutCache() = runTest {
        transactionRepository.refreshError = IllegalStateException("Pas de réseau")

        createViewModel().uiState.test {
            assertEquals(CarteUiState.Loading, awaitItem())
            assertEquals(CarteUiState.Error("Pas de réseau"), awaitItem())
            expectNoEvents()
        }
    }

    @Test
    fun reloadEmitsRefreshingThenIdle() = runTest {
        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.uiState.test {
            // Un StateFlow rejoue toujours sa valeur courante : le premier item est l'état déjà affiché.
            val before = assertIs<CarteUiState.Dashboard>(awaitItem())
            assertEquals(false, before.isRefreshing)

            viewModel.loadDashboard()

            val during = assertIs<CarteUiState.Dashboard>(awaitItem())
            assertEquals(true, during.isRefreshing)
            // Pendant le rechargement, les données déjà affichées restent à l'écran.
            assertEquals(remote, during.transactions)

            val after = assertIs<CarteUiState.Dashboard>(awaitItem())
            assertEquals(false, after.isRefreshing)
            assertNull(after.refreshErrorMessage)
        }
    }

    @Test
    fun failedReloadKeepsTheDataAndReportsTheError() = runTest {
        val viewModel = createViewModel()
        advanceUntilIdle()
        transactionRepository.refreshError = IllegalStateException("Pas de réseau")

        viewModel.uiState.test {
            awaitItem() // état courant
            viewModel.loadDashboard()

            assertEquals(true, assertIs<CarteUiState.Dashboard>(awaitItem()).isRefreshing)

            val after = assertIs<CarteUiState.Dashboard>(awaitItem())
            assertEquals(false, after.isRefreshing)
            assertEquals("Pas de réseau", after.refreshErrorMessage)
            assertEquals(remote, after.transactions)
        }
    }

    @Test
    fun sessionExpirationRemovesTheProfileFromTheEmittedState() = runTest {
        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.uiState.test {
            assertEquals(profile, assertIs<CarteUiState.Dashboard>(awaitItem()).profile)

            authSession.expire()

            assertNull(assertIs<CarteUiState.Dashboard>(awaitItem()).profile)
        }
    }
}
