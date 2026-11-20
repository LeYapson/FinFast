package fr.mobilune.componentgallery.viewmodel

import fr.mobilune.componentgallery.core.network.AuthSession
import fr.mobilune.componentgallery.data.FakeAuthRepository
import fr.mobilune.componentgallery.data.FakeLastVisitedMenuRepository
import fr.mobilune.componentgallery.data.FakeRegisteredAccountRepository
import fr.mobilune.componentgallery.data.FakeTransactionRepository
import fr.mobilune.componentgallery.data.FakeVirtualCardRepository
import fr.mobilune.componentgallery.data.InMemoryAccessTokenStore
import fr.mobilune.componentgallery.data.MainMenu
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
import kotlin.test.assertNull

/** Restauration de la session au démarrage : le jeton stocké décide de l'écran affiché. */
@OptIn(ExperimentalCoroutinesApi::class)
class AuthViewModelTest {
    private val testDispatcher = StandardTestDispatcher()
    private val tokenStore = InMemoryAccessTokenStore()
    private val lastVisitedMenuRepository = FakeLastVisitedMenuRepository()

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    // Le même dispatcher sert de Main et de « IO » : runTest contrôle ainsi tout le travail du ViewModel.
    private fun createViewModel() = AuthViewModel(
        authRepository = FakeAuthRepository(),
        authSession = AuthSession(tokenStore),
        lastVisitedMenuRepository = lastVisitedMenuRepository,
        virtualCardRepository = FakeVirtualCardRepository(),
        registeredAccountRepository = FakeRegisteredAccountRepository(),
        transactionRepository = FakeTransactionRepository(),
        ioDispatcher = testDispatcher,
    )

    @Test
    fun `Une session valide renvoie sur l'accueil`() = runTest {
        tokenStore.token = "token"
        val viewModel = createViewModel()
        assertEquals(AuthRoute.Welcome, viewModel.uiState.value.route)
        advanceUntilIdle()
        assertEquals(AuthRoute.Card, viewModel.uiState.value.route)
    }

    @Test
    fun `Une session invalide reste sur Welcome`() = runTest {
        val viewModel = createViewModel()
        assertEquals(AuthRoute.Welcome, viewModel.uiState.value.route)
        advanceUntilIdle()
        assertEquals(AuthRoute.Welcome, viewModel.uiState.value.route)
        assertNull(viewModel.uiState.value.errorMessage)
    }

    @Test
    fun `Une session valide rouvre le dernier menu visite`() = runTest {
        tokenStore.token = "token"
        lastVisitedMenuRepository.menu = MainMenu.Card
        val viewModel = createViewModel()
        advanceUntilIdle()
        assertEquals(AuthRoute.TransactionHistory, viewModel.uiState.value.route)
    }
}
