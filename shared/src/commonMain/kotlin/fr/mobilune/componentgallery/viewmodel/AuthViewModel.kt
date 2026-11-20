package fr.mobilune.componentgallery.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import fr.mobilune.componentgallery.data.AuthRepository
import fr.mobilune.componentgallery.data.AuthSession
import fr.mobilune.componentgallery.data.LastVisitedMenuRepository
import fr.mobilune.componentgallery.data.MainMenu
import fr.mobilune.componentgallery.data.RegisteredAccountRepository
import fr.mobilune.componentgallery.data.TransactionRepository
import fr.mobilune.componentgallery.data.VirtualCardScanEvents
import fr.mobilune.componentgallery.data.VirtualCardRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout

enum class AuthRoute {
    Welcome,
    RegisterNfc,
    RegisterCode,
    RegisterDisplayedName,
    LoginNfc,
    LoginCode,
    Card,
    TransactionHistory,
    SendAmount,
    ReceiveAmount,
    SendNfc,
    ReceiveNfc,
    Options,
    Projects,
}

/** Écrans pour lesquels le bouton retour du système remonte d'un cran (sinon il quitte l'app). */
fun AuthRoute.hasBackTarget(): Boolean =
    this != AuthRoute.Welcome && this != AuthRoute.Card && this != AuthRoute.Projects // Projects gère son propre retour

enum class MoneyTransferType {
    Send,
    Receive,
}

data class AuthUiState(
    val route: AuthRoute = AuthRoute.Welcome,
    val userId: String = "",
    val code: String = "",
    val displayedName: String = "",
    val registeredUserId: String? = null,
    val isSubmitting: Boolean = false,
    val errorMessage: String? = null,
    val menuErrorMessage: String? = null,
    val hasVirtualCard: Boolean = false,
    val isVirtualCardEnabled: Boolean = false,
    val isUpdatingVirtualCard: Boolean = false,
    val virtualCardErrorMessage: String? = null,
    val isLoggingOut: Boolean = false,
    val logoutErrorMessage: String? = null,
    val transferType: MoneyTransferType? = null,
    val transferAmount: String = "",
    val transferAmountCents: Long = 0,
    val isTransferring: Boolean = false,
    val transferErrorMessage: String? = null,
    val dashboardRefreshVersion: Int = 0,
    val virtualCardScanVersion: Int = 0,
    // Écran à retrouver en quittant les options.
    val optionsReturnRoute: AuthRoute = AuthRoute.Card,
)

class AuthViewModel(
    private val authRepository: AuthRepository,
    private val authSession: AuthSession,
    private val lastVisitedMenuRepository: LastVisitedMenuRepository,
    private val virtualCardRepository: VirtualCardRepository,
    private val registeredAccountRepository: RegisteredAccountRepository,
    private val transactionRepository: TransactionRepository,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) : ViewModel() {
    private val mutableUiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = mutableUiState.asStateFlow()

    init {
        // Le serveur a rejeté le jeton (expiré ou révoqué) : retour à l'écran de connexion.
        viewModelScope.launch {
            authSession.expirations.collect {
                try {
                    // Les transactions en cache appartiennent à la session qui vient de se terminer.
                    transactionRepository.clear()
                } catch (exception: CancellationException) {
                    throw exception
                } catch (_: Exception) {
                    // Le cache sera remplacé à la prochaine synchro.
                }
                mutableUiState.update { state ->
                    AuthUiState(
                        route = AuthRoute.Welcome,
                        errorMessage = "Ta session a expiré. Reconnecte-toi.",
                        hasVirtualCard = state.hasVirtualCard,
                        isVirtualCardEnabled = state.isVirtualCardEnabled,
                        virtualCardScanVersion = state.virtualCardScanVersion,
                    )
                }
            }
        }
        viewModelScope.launch {
            VirtualCardScanEvents.events.collect {
                mutableUiState.update { state ->
                    state.copy(virtualCardScanVersion = state.virtualCardScanVersion + 1)
                }
            }
        }
        viewModelScope.launch {
            try {
                withTimeout(10_000) {
                    withContext(ioDispatcher) {
                        authSession.restoreAccessToken()
                    }
                }
            } catch (_: TimeoutCancellationException) {
                mutableUiState.value = mutableUiState.value.copy(
                    errorMessage = "La restauration de la session a pris trop de temps. Réessaie de te connecter.",
                )
                return@launch
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: Exception) {
                mutableUiState.value = mutableUiState.value.copy(
                    route = AuthRoute.Welcome,
                    errorMessage = exception.message ?: "Impossible de restaurer la session.",
                )
                return@launch
            }

            if (authSession.accessToken == null) return@launch

            val currentState = mutableUiState.value
            if (currentState.route == AuthRoute.Welcome) {
                mutableUiState.value = currentState.copy(route = AuthRoute.Card)
            }

            try {
                val route = withTimeout(10_000) {
                    withContext(ioDispatcher) {
                        when (lastVisitedMenuRepository.getLastVisitedMenu()) {
                            MainMenu.Home -> AuthRoute.Card
                            MainMenu.Card -> AuthRoute.TransactionHistory
                        }
                    }
                }
                val state = mutableUiState.value
                if (state.route == AuthRoute.Card || state.route == AuthRoute.TransactionHistory) {
                    mutableUiState.value = state.copy(route = route, menuErrorMessage = null)
                }
            } catch (_: TimeoutCancellationException) {
                mutableUiState.value = mutableUiState.value.copy(
                    menuErrorMessage = "Impossible de restaurer le dernier menu.",
                )
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: Exception) {
                mutableUiState.value = mutableUiState.value.copy(
                    menuErrorMessage = exception.message ?: "Impossible de restaurer le dernier menu.",
                )
            }
        }
        viewModelScope.launch {
            try {
                // Lire avant de copier : sinon `value` est capturé avant la suspension et écrase la route.
                val hasVirtualCard = virtualCardRepository.getCardId() != null
                val isVirtualCardEnabled = virtualCardRepository.isCardEnabled()
                mutableUiState.value = mutableUiState.value.copy(
                    hasVirtualCard = hasVirtualCard,
                    isVirtualCardEnabled = isVirtualCardEnabled,
                )
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: Exception) {
                mutableUiState.value = mutableUiState.value.copy(
                    virtualCardErrorMessage = exception.message ?: "Impossible de charger la carte virtuelle.",
                )
            }
        }
    }

    fun toggleVirtualCard() {
        val state = mutableUiState.value
        if (state.isUpdatingVirtualCard) return
        mutableUiState.value = state.copy(
            isUpdatingVirtualCard = true,
            virtualCardErrorMessage = null,
        )
        viewModelScope.launch {
            try {
                val enabled = !state.isVirtualCardEnabled
                if (enabled && virtualCardRepository.getCardId() == null) {
                    throw IllegalStateException("Scanne d'abord ta carte physique pour enregistrer son identifiant.")
                }
                virtualCardRepository.setCardEnabled(enabled)
                val hasVirtualCard = enabled || virtualCardRepository.getCardId() != null
                mutableUiState.value = mutableUiState.value.copy(
                    isVirtualCardEnabled = enabled,
                    hasVirtualCard = hasVirtualCard,
                    isUpdatingVirtualCard = false,
                    virtualCardErrorMessage = null,
                )
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: Exception) {
                mutableUiState.value = mutableUiState.value.copy(
                    isUpdatingVirtualCard = false,
                    virtualCardErrorMessage = exception.message ?: "Impossible d'activer la carte virtuelle.",
                )
            }
        }
    }

    fun startRegister() {
        mutableUiState.value = AuthUiState(route = AuthRoute.RegisterNfc)
    }

    fun startLogin() {
        mutableUiState.value = AuthUiState(route = AuthRoute.LoginNfc)
    }

    /**
     * Bouton ou geste « retour » du système. Ne fait rien pendant un envoi (comme les boutons
     * désactivés à l'écran) ; sur l'accueil et l'écran de bienvenue, le système garde la main.
     */
    fun handleSystemBack() {
        val state = mutableUiState.value
        if (state.isSubmitting || state.isTransferring || state.isLoggingOut) return
        when (state.route) {
            AuthRoute.Options -> closeOptions()
            AuthRoute.Projects -> closeProjects()
            AuthRoute.TransactionHistory -> showHome()
            AuthRoute.SendAmount, AuthRoute.ReceiveAmount,
            AuthRoute.SendNfc, AuthRoute.ReceiveNfc -> cancelTransfer()

            AuthRoute.RegisterNfc, AuthRoute.RegisterCode, AuthRoute.RegisterDisplayedName,
            AuthRoute.LoginNfc, AuthRoute.LoginCode -> goBack()

            AuthRoute.Welcome, AuthRoute.Card -> Unit
        }
    }

    fun openOptions() {
        mutableUiState.update { state ->
            if (state.route == AuthRoute.Options) {
                state
            } else {
                state.copy(route = AuthRoute.Options, optionsReturnRoute = state.route)
            }
        }
    }

    fun closeOptions() {
        mutableUiState.update { state ->
            if (state.route == AuthRoute.Options) state.copy(route = state.optionsReturnRoute) else state
        }
    }

    /** Ouvre la section « Projets » (épargne et jeu AbdelRun) depuis l'accueil. */
    fun openProjects() {
        mutableUiState.update { it.copy(route = AuthRoute.Projects, menuErrorMessage = null) }
    }

    fun closeProjects() {
        mutableUiState.update { state ->
            if (state.route == AuthRoute.Projects) state.copy(route = AuthRoute.Card) else state
        }
    }

    fun showTransactionHistory() {
        saveLastVisitedMenu(MainMenu.Card, AuthRoute.TransactionHistory)
    }

    fun showHome() {
        saveLastVisitedMenu(MainMenu.Home, AuthRoute.Card)
    }

    fun startTransfer(type: MoneyTransferType) {
        mutableUiState.value = mutableUiState.value.copy(
            route = if (type == MoneyTransferType.Send) AuthRoute.SendAmount else AuthRoute.ReceiveAmount,
            transferType = type,
            transferAmount = "",
            transferAmountCents = 0,
            transferErrorMessage = null,
            isTransferring = false,
        )
    }

    fun updateTransferAmount(amount: String) {
        if (amount.length > 12 || amount.any { !it.isDigit() && it != ',' && it != '.' }) return
        mutableUiState.value = mutableUiState.value.copy(
            transferAmount = amount,
            transferErrorMessage = null,
        )
    }

    fun continueTransferToNfc() {
        val state = mutableUiState.value
        val amountCents = parseAmountCents(state.transferAmount)
        if (amountCents == null || amountCents <= 0) {
            mutableUiState.value = state.copy(
                transferErrorMessage = "Saisis un montant valide supérieur à 0 € (maximum 2 décimales).",
            )
            return
        }

        mutableUiState.value = state.copy(
            route = if (state.transferType == MoneyTransferType.Send) AuthRoute.SendNfc else AuthRoute.ReceiveNfc,
            transferAmountCents = amountCents,
            transferErrorMessage = null,
        )
    }

    fun clearTransferError() {
        mutableUiState.value = mutableUiState.value.copy(transferErrorMessage = null)
    }

    fun cancelTransfer() {
        mutableUiState.value = mutableUiState.value.copy(
            route = AuthRoute.Card,
            transferType = null,
            transferAmount = "",
            transferAmountCents = 0,
            transferErrorMessage = null,
            isTransferring = false,
        )
    }

    fun cancelLogout() {
        mutableUiState.value = mutableUiState.value.copy(logoutErrorMessage = null)
    }

    fun logout(onLoggedOut: () -> Unit) {
        if (mutableUiState.value.isLoggingOut) return
        mutableUiState.value = mutableUiState.value.copy(
            isLoggingOut = true,
            logoutErrorMessage = null,
        )
        viewModelScope.launch {
            try {
                authRepository.logout()
                // Les transactions en cache appartiennent au compte qui se déconnecte.
                transactionRepository.clear()
                mutableUiState.value = AuthUiState()
                onLoggedOut()
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: Exception) {
                mutableUiState.value = mutableUiState.value.copy(
                    isLoggingOut = false,
                    logoutErrorMessage = exception.message ?: "Impossible de se déconnecter.",
                )
            }
        }
    }

    fun onNfcCardScanned(cardId: String) {
        val state = mutableUiState.value
        if (state.route == AuthRoute.SendNfc || state.route == AuthRoute.ReceiveNfc) {
            transferMoney(state, cardId)
            return
        }
        if (state.isSubmitting) return
        if (cardId.isBlank()) {
            mutableUiState.value = state.copy(errorMessage = "La carte n'a pas fourni d'identifiant valide.")
            return
        }

        mutableUiState.value = state.copy(isSubmitting = true, errorMessage = null)
        viewModelScope.launch {
            try {
                virtualCardRepository.saveCardId(cardId)
                mutableUiState.value = mutableUiState.value.copy(
                    userId = cardId,
                    hasVirtualCard = true,
                    route = when (state.route) {
                        AuthRoute.RegisterNfc -> AuthRoute.RegisterCode
                        AuthRoute.LoginNfc -> AuthRoute.LoginCode
                        else -> state.route
                    },
                    code = "",
                    isSubmitting = false,
                    errorMessage = null,
                )
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: Exception) {
                mutableUiState.value = mutableUiState.value.copy(
                    isSubmitting = false,
                    errorMessage = exception.message ?: "Impossible d'enregistrer l'identifiant NFC.",
                )
            }
        }
    }

    fun clearNfcError() {
        mutableUiState.value = mutableUiState.value.copy(errorMessage = null)
    }

    fun addCodeDigit(digit: Int) {
        val state = mutableUiState.value
        if (digit !in 0..9 || state.code.length >= 4 || state.isSubmitting) return
        mutableUiState.value = state.copy(code = state.code + digit, errorMessage = null)
    }

    fun eraseCodeDigit() {
        val state = mutableUiState.value
        if (state.code.isNotEmpty() && !state.isSubmitting) {
            mutableUiState.value = state.copy(
                code = state.code.dropLast(1),
                errorMessage = null,
            )
        }
    }

    fun continueFromCode() {
        val state = mutableUiState.value
        if (state.code.length != 4) {
            mutableUiState.value = state.copy(errorMessage = "Choisis un code à 4 chiffres.")
            return
        }

        when (state.route) {
            AuthRoute.RegisterCode -> mutableUiState.value = state.copy(
                route = AuthRoute.RegisterDisplayedName,
                errorMessage = null,
            )

            AuthRoute.LoginCode -> authenticate(state)
            else -> Unit
        }
    }

    fun updateDisplayedName(value: String) {
        mutableUiState.value = mutableUiState.value.copy(
            displayedName = value,
            errorMessage = null,
        )
    }

    fun createAccount() {
        val state = mutableUiState.value
        if (state.displayedName.isBlank()) {
            mutableUiState.value = state.copy(errorMessage = "Saisis le nom que tu souhaites afficher.")
            return
        }

        authSession.setDisplayedName(state.displayedName.trim())
        mutableUiState.value = state.copy(isSubmitting = true, errorMessage = null)
        viewModelScope.launch {
            try {
                val userId = state.userId.trim()
                if (state.registeredUserId != userId) {
                    if (registeredAccountRepository.isRegistered(userId)) {
                        throw IllegalStateException(
                            "Un compte existe déjà pour cette carte. Choisis « J'ai un compte » pour te connecter.",
                        )
                    }
                    authRepository.register(
                        userId = userId,
                        code = state.code,
                        displayedName = state.displayedName.trim(),
                    )
                    registeredAccountRepository.markRegistered(userId)
                    mutableUiState.value = mutableUiState.value.copy(registeredUserId = userId)
                }
                authRepository.login(userId, state.code)
                val menuErrorMessage = try {
                    lastVisitedMenuRepository.saveLastVisitedMenu(MainMenu.Home)
                    null
                } catch (exception: CancellationException) {
                    throw exception
                } catch (exception: Exception) {
                    exception.message ?: "Impossible d'enregistrer le dernier menu."
                }
                mutableUiState.value = mutableUiState.value.copy(
                    route = AuthRoute.Card,
                    isSubmitting = false,
                    errorMessage = null,
                    menuErrorMessage = menuErrorMessage,
                )
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: Exception) {
                val accountCreated = mutableUiState.value.registeredUserId == state.userId.trim()
                mutableUiState.value = mutableUiState.value.copy(
                    isSubmitting = false,
                    errorMessage = if (accountCreated) {
                        "Compte créé, mais connexion impossible. Réessaie."
                    } else {
                        exception.message ?: "Impossible de créer le compte."
                    },
                )
            }
        }
    }

    fun goBack() {
        val state = mutableUiState.value
        mutableUiState.value = state.copy(
            route = when (state.route) {
                AuthRoute.RegisterCode -> AuthRoute.RegisterNfc
                AuthRoute.RegisterDisplayedName -> AuthRoute.RegisterCode
                AuthRoute.LoginCode -> AuthRoute.LoginNfc
                else -> AuthRoute.Welcome
            },
            errorMessage = null,
            isSubmitting = false,
        )
    }

    private fun authenticate(state: AuthUiState) {
        mutableUiState.value = state.copy(isSubmitting = true, errorMessage = null)
        viewModelScope.launch {
            try {
                authRepository.login(state.userId.trim(), state.code)
                val route = try {
                    when (lastVisitedMenuRepository.getLastVisitedMenu()) {
                        MainMenu.Home -> AuthRoute.Card
                        MainMenu.Card -> AuthRoute.TransactionHistory
                    }
                } catch (exception: CancellationException) {
                    throw exception
                } catch (exception: Exception) {
                    mutableUiState.value = mutableUiState.value.copy(
                        route = AuthRoute.Card,
                        isSubmitting = false,
                        errorMessage = null,
                        menuErrorMessage = exception.message ?: "Impossible de restaurer le dernier menu.",
                    )
                    return@launch
                }
                mutableUiState.value = mutableUiState.value.copy(
                    route = route,
                    isSubmitting = false,
                    errorMessage = null,
                    menuErrorMessage = null,
                )
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: Exception) {
                mutableUiState.value = mutableUiState.value.copy(
                    isSubmitting = false,
                    errorMessage = exception.message ?: "Impossible de se connecter.",
                )
            }
        }
    }

    private fun saveLastVisitedMenu(menu: MainMenu, route: AuthRoute) {
        viewModelScope.launch {
            try {
                lastVisitedMenuRepository.saveLastVisitedMenu(menu)
                mutableUiState.value = mutableUiState.value.copy(
                    route = route,
                    menuErrorMessage = null,
                )
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: Exception) {
                mutableUiState.value = mutableUiState.value.copy(
                    menuErrorMessage = exception.message ?: "Impossible d'enregistrer le dernier menu.",
                )
            }
        }
    }

    private fun transferMoney(state: AuthUiState, counterpartyUserId: String) {
        if (state.isTransferring) return
        if (counterpartyUserId.isBlank()) {
            mutableUiState.value = state.copy(
                transferErrorMessage = "La carte NFC n'a pas fourni d'identifiant valide.",
            )
            return
        }
        val transferType = state.transferType ?: return
        mutableUiState.value = state.copy(isTransferring = true, transferErrorMessage = null)
        viewModelScope.launch {
            try {
                when (transferType) {
                    MoneyTransferType.Send -> authRepository.sendMoney(
                        userId = counterpartyUserId,
                        amountCents = state.transferAmountCents,
                    )

                    MoneyTransferType.Receive -> authRepository.receiveMoney(
                        userId = counterpartyUserId,
                        amountCents = state.transferAmountCents,
                    )
                }
                mutableUiState.value = mutableUiState.value.copy(
                    route = AuthRoute.Card,
                    transferType = null,
                    transferAmount = "",
                    transferAmountCents = 0,
                    isTransferring = false,
                    transferErrorMessage = null,
                    dashboardRefreshVersion = mutableUiState.value.dashboardRefreshVersion + 1,
                )
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: Exception) {
                mutableUiState.value = mutableUiState.value.copy(
                    isTransferring = false,
                    transferErrorMessage = exception.message ?: "Le transfert a échoué.",
                )
            }
        }
    }

    private fun parseAmountCents(amount: String): Long? {
        val normalized = amount.trim().replace(',', '.')
        val parts = normalized.split('.')
        if (parts.size > 2 || parts[0].isEmpty()) return null
        val euros = parts[0].toLongOrNull() ?: return null
        val centText = parts.getOrElse(1) { "" }
        if (centText.length > 2) return null
        val cents = centText.padEnd(2, '0').ifEmpty { "00" }.toLongOrNull() ?: return null
        if (euros > (Long.MAX_VALUE - cents) / 100) return null
        return euros * 100 + cents
    }

}
