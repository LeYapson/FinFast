package fr.mobilune.componentgallery.viewmodel

import fr.mobilune.componentgallery.data.AuthSession
import fr.mobilune.componentgallery.data.FakeAuthRepository
import fr.mobilune.componentgallery.data.FakeTransactionRepository
import fr.mobilune.componentgallery.data.UserProfile
import fr.mobilune.componentgallery.data.fakeTransaction
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * Tests de [CarteViewModel] avec `kotlinx-coroutines-test` seul : on avance le temps virtuel à la main
 * puis on lit `uiState.value`. Voir `CarteViewModelTurbineTest` pour la même chose avec Turbine.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class CarteViewModelTest {
    private val authRepository = FakeAuthRepository()
    private val transactionRepository = FakeTransactionRepository()
    private val authSession = AuthSession()

    private val profile = UserProfile(userId = "user", displayedName = "Alice", balanceCents = 12_500)
    private val remote = listOf(fakeTransaction("1", -2_000), fakeTransaction("2", 4_500))

    @BeforeTest
    fun setUp() {
        // viewModelScope lance ses coroutines sur Dispatchers.Main, absent d'un test unitaire.
        Dispatchers.setMain(StandardTestDispatcher())
        authRepository.profile = profile
        transactionRepository.remoteTransactions = remote
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel() = CarteViewModel(transactionRepository, authRepository, authSession)

    @Test
    fun startsInLoadingThenShowsDashboardOnceRefreshCompletes() = runTest {
        val viewModel = createViewModel()

        // Rien n'a encore tourné : les coroutines du ViewModel attendent dans la file du dispatcher de test.
        assertEquals(CarteUiState.Loading, viewModel.uiState.value)

        advanceUntilIdle()

        assertEquals(
            CarteUiState.Dashboard(
                profile = profile,
                transactions = remote,
                lastUpdatedMillis = 1_000,
                isRefreshing = false,
                refreshErrorMessage = null,
            ),
            viewModel.uiState.value,
        )
    }

    @Test
    fun staysLoadingWhileTheNetworkCallIsInFlight() = runTest {
        transactionRepository.refreshDelayMillis = 500
        val viewModel = createViewModel()

        runCurrent()
        assertEquals(CarteUiState.Loading, viewModel.uiState.value)

        // Temps virtuel : 499 ms « passent » instantanément, le fake n'a pas encore répondu.
        advanceTimeBy(499)
        runCurrent()
        assertEquals(CarteUiState.Loading, viewModel.uiState.value)

        advanceTimeBy(1)
        runCurrent()
        assertEquals(remote, (viewModel.uiState.value as CarteUiState.Dashboard).transactions)
    }

    @Test
    fun showsErrorWhenRefreshFailsAndThereIsNoCache() = runTest {
        transactionRepository.refreshError = IllegalStateException("Pas de réseau")
        val viewModel = createViewModel()

        advanceUntilIdle()

        assertEquals(CarteUiState.Error("Pas de réseau"), viewModel.uiState.value)
    }

    @Test
    fun keepsCachedTransactionsWhenRefreshFails() = runTest {
        val cached = listOf(fakeTransaction("old", -500))
        val repository = FakeTransactionRepository(cached, cachedLastUpdatedMillis = 42)
        repository.refreshError = IllegalStateException("Pas de réseau")
        val viewModel = CarteViewModel(repository, authRepository, authSession)

        advanceUntilIdle()

        assertEquals(
            CarteUiState.Dashboard(
                profile = profile,
                transactions = cached,
                lastUpdatedMillis = 42,
                isRefreshing = false,
                refreshErrorMessage = "Pas de réseau",
            ),
            viewModel.uiState.value,
        )
    }

    @Test
    fun showsDashboardWithoutProfileWhenProfileCannotBeLoaded() = runTest {
        authRepository.getCurrentUserError = IllegalStateException("Hors ligne")
        val viewModel = createViewModel()

        advanceUntilIdle()

        val dashboard = viewModel.uiState.value as CarteUiState.Dashboard
        assertNull(dashboard.profile)
        assertEquals(remote, dashboard.transactions)
        assertNull(dashboard.refreshErrorMessage)
    }

    @Test
    fun ignoresLoadDashboardWhileARefreshIsAlreadyRunning() = runTest {
        val viewModel = createViewModel() // le init appelle déjà loadDashboard()
        viewModel.loadDashboard()
        viewModel.loadDashboard()

        advanceUntilIdle()

        assertEquals(1, transactionRepository.refreshCount)
        assertEquals(1, authRepository.getCurrentUserCount)

        // Une fois terminé, un nouveau chargement est de nouveau accepté.
        viewModel.loadDashboard()
        advanceUntilIdle()
        assertEquals(2, transactionRepository.refreshCount)
    }

    @Test
    fun forgetsTheProfileWhenTheSessionExpires() = runTest {
        val viewModel = createViewModel()
        advanceUntilIdle()
        assertEquals(profile, (viewModel.uiState.value as CarteUiState.Dashboard).profile)

        authSession.expire()
        advanceUntilIdle()

        assertNull((viewModel.uiState.value as CarteUiState.Dashboard).profile)
    }
}
