package fr.mobilune.componentgallery

import fr.mobilune.componentgallery.core.designsystem.heading
import fr.mobilune.componentgallery.core.designsystem.screenTitle
import fr.mobilune.componentgallery.core.designsystem.AccessibilityState
import fr.mobilune.componentgallery.core.designsystem.BackIcon
import fr.mobilune.componentgallery.core.designsystem.ErrorText
import fr.mobilune.componentgallery.core.designsystem.fadeTransition
import fr.mobilune.componentgallery.core.designsystem.highContrastColorScheme
import fr.mobilune.componentgallery.core.designsystem.IconAction
import fr.mobilune.componentgallery.core.designsystem.LoadingIndicator
import fr.mobilune.componentgallery.core.designsystem.MoreIcon
import fr.mobilune.componentgallery.core.designsystem.NfcPayPanel
import fr.mobilune.componentgallery.core.designsystem.rememberAppHaptics
import fr.mobilune.componentgallery.core.designsystem.slideFadeIn
import fr.mobilune.componentgallery.core.designsystem.ThemeConstants
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.AlertDialog
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Density
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.TextButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import fr.mobilune.componentgallery.core.designsystem.components.AmountProjectCard
import fr.mobilune.componentgallery.core.designsystem.components.BudgetCard
import fr.mobilune.componentgallery.core.designsystem.components.CreditCard
import fr.mobilune.componentgallery.core.designsystem.components.StableAmountCard
import fr.mobilune.componentgallery.core.designsystem.components.TransactionList
import fr.mobilune.componentgallery.data.PushEvents
import fr.mobilune.componentgallery.di.appModule
import fr.mobilune.componentgallery.projects.ProjectsRoot
import fr.mobilune.componentgallery.viewmodel.ProjectsViewModel
import fr.mobilune.componentgallery.viewmodel.PushViewModel
import fr.mobilune.componentgallery.viewmodel.AuthRoute
import fr.mobilune.componentgallery.viewmodel.AuthViewModel
import fr.mobilune.componentgallery.viewmodel.MoneyTransferType
import fr.mobilune.componentgallery.viewmodel.CarteUiState
import fr.mobilune.componentgallery.viewmodel.CarteViewModel
import fr.mobilune.componentgallery.viewmodel.SettingsViewModel
import fr.mobilune.componentgallery.viewmodel.hasBackTarget
import androidx.compose.foundation.selection.toggleable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.semantics.Role
import fr.mobilune.componentgallery.data.PREFERENCES_FILE_NAME
import fr.mobilune.componentgallery.data.db.DATABASE_FILE_NAME
import org.koin.compose.KoinApplication
import org.koin.compose.viewmodel.koinViewModel
import org.koin.dsl.koinConfiguration

@Composable
@Preview
fun App(
    onExit: () -> Unit = {},
    dataStoreFilePath: String = PREFERENCES_FILE_NAME,
    databaseFilePath: String = DATABASE_FILE_NAME,
    notificationPermission: NotificationPermissionAccess = NotificationPermissionAccess.Unsupported,
    transactionScheduler: SimulatedTransactionScheduler = SimulatedTransactionScheduler.Unsupported,
) {
    KoinApplication(
        configuration = koinConfiguration {
            modules(appModule(dataStoreFilePath, databaseFilePath))
        },
    ) {
        AuthenticatedApp(onExit, notificationPermission, transactionScheduler)
    }
}

/**
 * Écoute les notifications de nouvelle dépense, uniquement tant que l'utilisateur est connecté :
 * un clic reçu avant la connexion (ou pendant la restauration de la session) attend dans [PushEvents].
 */
@Composable
private fun NotificationEventsHandler(onOpenCard: () -> Unit) {
    // Même instance que celle de l'accueil et de « Ma carte » : les écrans se mettent à jour d'eux-mêmes.
    val cardViewModel: CarteViewModel = koinViewModel()
    val currentOnOpenCard by rememberUpdatedState(onOpenCard)

    LaunchedEffect(Unit) {
        PushEvents.newTransactions.collect { cardViewModel.loadDashboard() }
    }
    LaunchedEffect(Unit) {
        PushEvents.openCardRequests.collect {
            currentOnOpenCard()
            cardViewModel.loadDashboard()
        }
    }
}

/** Vrai dès que l'utilisateur a une session ouverte (tout écran hors accueil, inscription et connexion). */
private fun AuthRoute.isLoggedIn(): Boolean = when (this) {
    AuthRoute.Welcome,
    AuthRoute.RegisterNfc,
    AuthRoute.RegisterCode,
    AuthRoute.RegisterDisplayedName,
    AuthRoute.LoginNfc,
    AuthRoute.LoginCode -> false

    else -> true
}

@Composable
private fun AuthenticatedApp(
    onExit: () -> Unit,
    notificationPermission: NotificationPermissionAccess,
    transactionScheduler: SimulatedTransactionScheduler,
) {
    val authViewModel: AuthViewModel = koinViewModel()
    val authState by authViewModel.uiState.collectAsState()
    val settingsViewModel: SettingsViewModel = koinViewModel()
    val settings by settingsViewModel.settings.collectAsState()
    val projectsViewModel: ProjectsViewModel = koinViewModel()
    val pushViewModel: PushViewModel = koinViewModel()
    val pushState by pushViewModel.uiState.collectAsState()

    // Après la connexion (POST /auth/login) ou la restauration d'une session : enregistre l'appareil sur le serveur.
    val isLoggedIn = authState.route.isLoggedIn()
    LaunchedEffect(isLoggedIn) {
        if (isLoggedIn) pushViewModel.registerDevice()
    }
    // Notification « nouvelle dépense » : recharge la liste (app ouverte) ou ouvre « Ma carte » (clic).
    if (isLoggedIn) {
        NotificationEventsHandler(onOpenCard = authViewModel::showTransactionHistory)
    }

    // Le jeton FCM a changé (onNewToken) : on le réenregistre.
    DisposableEffect(pushViewModel) {
        val stopObserving = observePushTokens(pushViewModel::onNewToken)
        onDispose(stopObserving)
    }

    // À chaque ouverture des options, on relit l'état de la permission (l'utilisateur a pu la changer dans les réglages).
    LaunchedEffect(authState.route) {
        if (authState.route == AuthRoute.Options) {
            notificationPermission.isGranted(pushViewModel::onPermissionChecked)
        }
    }
    SideEffect {
        ThemeConstants.highContrast = settings.highContrast
        AccessibilityState.reduceMotion = settings.reduceMotion
        AccessibilityState.hapticsEnabled = settings.hapticsEnabled
    }

    // Bouton / geste « retour » du système : remonte d'un écran au lieu de quitter l'app.
    SystemBackHandler(enabled = authState.route.hasBackTarget()) { authViewModel.handleSystemBack() }

    // La taille choisie dans l'app s'ajoute à celle des réglages du téléphone.
    val systemDensity = LocalDensity.current
    val scaledDensity = remember(systemDensity, settings.textScalePercent) {
        Density(systemDensity.density, systemDensity.fontScale * settings.textScalePercent / 100f)
    }

    CompositionLocalProvider(LocalDensity provides scaledDensity) {
    MaterialTheme(
        colorScheme = if (settings.highContrast) highContrastColorScheme() else MaterialTheme.colorScheme,
    ) {
        AnimatedContent(
            targetState = authState.route,
            transitionSpec = {
                if (AccessibilityState.reduceMotion) {
                    EnterTransition.None togetherWith ExitTransition.None
                } else {
                    (fadeIn(tween(220)) + slideInHorizontally(tween(220)) { it / 12 }) togetherWith
                            (fadeOut(tween(160)) + slideOutHorizontally(tween(160)) { -it / 12 })
                }
            },
            label = "app_screen_transition",
        ) { route ->
            when (route) {
                AuthRoute.Welcome -> WelcomeScreen(
                    onLogin = authViewModel::startLogin,
                    onRegister = authViewModel::startRegister,
                    errorMessage = authState.errorMessage,
                )

                AuthRoute.RegisterNfc -> NfcScanScreen(
                    title = "Scanne ta carte",
                    onBack = authViewModel::goBack,
                    onTagRead = authViewModel::onNfcCardScanned,
                    isSubmitting = authState.isSubmitting,
                    errorMessage = authState.errorMessage,
                    onRetry = authViewModel::clearNfcError,
                )

                AuthRoute.RegisterCode -> SecretCodeScreen(
                    title = "Choisis un code secret",
                    progressStep = 1,
                    code = authState.code,
                    actionLabel = "Suivant",
                    errorMessage = authState.errorMessage,
                    isSubmitting = authState.isSubmitting,
                    onDigit = authViewModel::addCodeDigit,
                    onErase = authViewModel::eraseCodeDigit,
                    onBack = authViewModel::goBack,
                    onAction = authViewModel::continueFromCode,
                )

                AuthRoute.RegisterDisplayedName -> DisplayedNameScreen(
                    displayedName = authState.displayedName,
                    errorMessage = authState.errorMessage,
                    isSubmitting = authState.isSubmitting,
                    onDisplayedNameChange = authViewModel::updateDisplayedName,
                    onBack = authViewModel::goBack,
                    onNext = authViewModel::createAccount,
                )

                AuthRoute.LoginNfc -> NfcScanScreen(
                    title = "Scanne ta carte",
                    onBack = authViewModel::goBack,
                    onTagRead = authViewModel::onNfcCardScanned,
                    isSubmitting = authState.isSubmitting,
                    errorMessage = authState.errorMessage,
                    onRetry = authViewModel::clearNfcError,
                )

                AuthRoute.LoginCode -> SecretCodeScreen(
                    title = "Ton code",
                    progressStep = null,
                    code = authState.code,
                    actionLabel = "Connexion",
                    errorMessage = authState.errorMessage,
                    isSubmitting = authState.isSubmitting,
                    onDigit = authViewModel::addCodeDigit,
                    onErase = authViewModel::eraseCodeDigit,
                    onBack = authViewModel::goBack,
                    onAction = authViewModel::continueFromCode,
                )

                AuthRoute.Card -> HomeScreen(
                    onShowHistory = authViewModel::showTransactionHistory,
                    onStartTransfer = authViewModel::startTransfer,
                    dashboardRefreshVersion = authState.dashboardRefreshVersion,
                    menuErrorMessage = authState.menuErrorMessage,
                    logoutErrorMessage = authState.logoutErrorMessage,
                    isLoggingOut = authState.isLoggingOut,
                    onLogout = { authViewModel.logout(onExit) },
                    onDismissLogout = authViewModel::cancelLogout,
                    onOpenOptions = authViewModel::openOptions,
                    onOpenProjects = {
                        projectsViewModel.reset()
                        authViewModel.openProjects()
                    },
                )

                AuthRoute.Projects -> ProjectsRoot(
                    onLeave = authViewModel::closeProjects,
                    onOpenOptions = authViewModel::openOptions,
                )

                AuthRoute.Options -> OptionsScreen(
                    settings = settings,
                    onHighContrastChange = settingsViewModel::setHighContrast,
                    onTextScaleChange = settingsViewModel::setTextScalePercent,
                    onReduceMotionChange = settingsViewModel::setReduceMotion,
                    onHapticsChange = settingsViewModel::setHapticsEnabled,
                    push = pushState,
                    onEnableNotifications = {
                        notificationPermission.request(pushViewModel::onPermissionResult)
                    },
                    onSendTestPush = pushViewModel::sendTestPush,
                    onSimulateTransaction = pushViewModel::simulateTransaction,
                    canScheduleTransaction = transactionScheduler.isSupported,
                    onScheduleTransaction = {
                        transactionScheduler.scheduleIn(SIMULATED_TRANSACTION_DELAY_SECONDS)
                        pushViewModel.onTransactionScheduled(SIMULATED_TRANSACTION_DELAY_SECONDS)
                    },
                    onSendPushToUser = pushViewModel::sendPushToUser,
                    onRegisterDevice = { pushViewModel.registerDevice(manual = true) },
                    onCheckDevices = pushViewModel::checkRegisteredDevices,
                    onBack = authViewModel::closeOptions,
                )

                AuthRoute.TransactionHistory -> TransactionHistoryScreen(
                    onBack = authViewModel::showHome,
                    onOpenOptions = authViewModel::openOptions,
                    menuErrorMessage = authState.menuErrorMessage,
                    hasVirtualCard = authState.hasVirtualCard,
                    isVirtualCardEnabled = authState.isVirtualCardEnabled,
                    isUpdatingVirtualCard = authState.isUpdatingVirtualCard,
                    virtualCardErrorMessage = authState.virtualCardErrorMessage,
                    virtualCardScanVersion = authState.virtualCardScanVersion,
                    onToggleVirtualCard = authViewModel::toggleVirtualCard,
                )

                AuthRoute.SendAmount, AuthRoute.ReceiveAmount -> MoneyAmountScreen(
                    isSend = authState.transferType == MoneyTransferType.Send,
                    amount = authState.transferAmount,
                    errorMessage = authState.transferErrorMessage,
                    onAmountChange = authViewModel::updateTransferAmount,
                    onBack = authViewModel::cancelTransfer,
                    onNext = authViewModel::continueTransferToNfc,
                )

                AuthRoute.SendNfc, AuthRoute.ReceiveNfc -> MoneyNfcScanScreen(
                    isSend = authState.transferType == MoneyTransferType.Send,
                    amountCents = authState.transferAmountCents,
                    isTransferring = authState.isTransferring,
                    errorMessage = authState.transferErrorMessage,
                    onBack = authViewModel::cancelTransfer,
                    onTagRead = authViewModel::onNfcCardScanned,
                    onRetry = authViewModel::clearTransferError,
                )
            }
        }
    }
    }
}

@Composable
private fun HomeScreen(
    onShowHistory: () -> Unit,
    onStartTransfer: (MoneyTransferType) -> Unit,
    dashboardRefreshVersion: Int,
    menuErrorMessage: String?,
    logoutErrorMessage: String?,
    isLoggingOut: Boolean,
    onLogout: () -> Unit,
    onDismissLogout: () -> Unit,
    onOpenOptions: () -> Unit,
    onOpenProjects: () -> Unit,
) {
    val cardViewModel: CarteViewModel = koinViewModel()
    val uiState by cardViewModel.uiState.collectAsState()
    val dashboard = uiState as? CarteUiState.Dashboard
    var showLogoutDialog by remember { mutableStateOf(false) }

    // Aussi à l'arrivée sur l'accueil : après une reconnexion (session expirée), le ViewModel existe déjà.
    LaunchedEffect(dashboardRefreshVersion) {
        cardViewModel.loadDashboard()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ThemeConstants.classicBackground)
            .screenTitle("Accueil")
            .safeContentPadding()
            .padding(horizontal = 12.dp, vertical = 12.dp),
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                // Cette flèche ouvre la boîte « Quitter l'application » : on l'annonce comme telle.
                IconAction(
                    description = "Se déconnecter et quitter l'application",
                    onClick = { showLogoutDialog = true },
                ) { BackIcon(it, contentDescription = null) }
                Text(
                    text = "Accueil",
                    modifier = Modifier.heading(),
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = ThemeConstants.onClassicBackground,
                )
                IconAction(
                    description = "Plus d'options",
                    onClick = onOpenOptions,
                ) { MoreIcon(it, contentDescription = null) }
            }
            if (menuErrorMessage != null) {
                Spacer(modifier = Modifier.height(8.dp))
                ErrorText(menuErrorMessage)
            }
            Spacer(modifier = Modifier.height(20.dp))
            AnimatedVisibility(
                visible = dashboard?.profile?.displayedName?.isNotBlank() == true,
                enter = slideFadeIn(delayMillis = 0, offsetDivisor = 5),
            ) {
                val displayedName = dashboard?.profile?.displayedName.orEmpty()
                Text(
                    text = "Bonjour $displayedName",
                    modifier = Modifier.heading(),
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = ThemeConstants.onClassicBackground,
                )
                Spacer(modifier = Modifier.height(16.dp))
            }
            AnimatedVisibility(
                visible = true,
                enter = slideFadeIn(delayMillis = 80),
            ) {
                AmountProjectCard(
                    modifier = Modifier.fillMaxWidth(),
                    epargne = "Solde du compte",
                    amount = dashboard?.profile?.balanceCents?.formatEuro() ?: "—",
                    projectButton = "Voir les projets",
                    onProjectsClick = onOpenProjects,
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
            AnimatedVisibility(
                visible = true,
                enter = slideFadeIn(delayMillis = 140),
            ) {
                BudgetCard(modifier = Modifier.fillMaxWidth())
            }
            Spacer(modifier = Modifier.height(28.dp))
            Text(
                text = "Balance",
                modifier = Modifier.heading(),
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = ThemeConstants.onClassicBackground,
            )
            Spacer(modifier = Modifier.height(12.dp))
            AnimatedVisibility(
                visible = true,
                enter = slideFadeIn(delayMillis = 200),
            ) {
                val expensesCard: @Composable (Modifier) -> Unit = { cardModifier ->
                    StableAmountCard(
                        modifier = cardModifier,
                        amount = dashboard?.expensesCents?.formatEuro() ?: "—",
                        status = "Dépenses",
                        backgroundColor = ThemeConstants.outcomeBackground,
                        statusColor = ThemeConstants.outcomeBackgroundStrong,
                    )
                }
                val incomeCard: @Composable (Modifier) -> Unit = { cardModifier ->
                    StableAmountCard(
                        modifier = cardModifier,
                        amount = dashboard?.incomeCents?.formatEuro() ?: "—",
                        status = "Entrées",
                    )
                }
                // Très grand texte : les montants ne tiendraient plus sur une demi-largeur, on empile.
                if (LocalDensity.current.fontScale >= 1.3f) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        expensesCard(Modifier.fillMaxWidth())
                        incomeCard(Modifier.fillMaxWidth())
                    }
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        expensesCard(Modifier.weight(1f))
                        incomeCard(Modifier.weight(1f))
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            TextButton(
                onClick = onShowHistory,
                modifier = Modifier.align(Alignment.CenterHorizontally),
            ) {
                Text(
                    text = "Voir toutes les transactions",
                    color = ThemeConstants.accentBackground,
                )
            }
            AnimatedVisibility(
                visible = uiState is CarteUiState.Error,
                enter = fadeIn(tween(180)),
                exit = fadeOut(tween(120)),
            ) {
                (uiState as? CarteUiState.Error)?.let { state ->
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        ErrorText(state.message)
                        TextButton(onClick = cardViewModel::loadDashboard) {
                            Text("Réessayer")
                        }
                    }
                }
            }
            when (uiState) {
                CarteUiState.Loading -> Box(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    LoadingIndicator()
                }

                is CarteUiState.Error -> Unit
                is CarteUiState.Dashboard -> Unit
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Button(
                onClick = { onStartTransfer(MoneyTransferType.Send) },
                modifier = Modifier.weight(1f),
                colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                    containerColor = ThemeConstants.accentBackground,
                    contentColor = ThemeConstants.onAccentBackground,
                ),
            ) {
                Text("Envoyer")
            }
            Button(
                onClick = { onStartTransfer(MoneyTransferType.Receive) },
                modifier = Modifier.weight(1f),
                colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                    containerColor = ThemeConstants.onClassicBackground,
                    contentColor = ThemeConstants.classicBackground,
                ),
            ) {
                Text("Recevoir")
            }
        }
    }

    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = {
                if (!isLoggingOut) {
                    showLogoutDialog = false
                    onDismissLogout()
                }
            },
            title = { Text("Quitter l'application ?") },
            text = {
                Column {
                    Text("Veux-tu te déconnecter et quitter l'application ?")
                    if (logoutErrorMessage != null) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = logoutErrorMessage,
                            color = ThemeConstants.error,
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = onLogout,
                    enabled = !isLoggingOut,
                ) {
                    if (isLoggingOut) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = ThemeConstants.onAccentBackground,
                            strokeWidth = 2.dp,
                        )
                    } else {
                        Text("Se déconnecter et quitter")
                    }
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showLogoutDialog = false
                        onDismissLogout()
                    },
                    enabled = !isLoggingOut,
                ) {
                    Text("Annuler")
                }
            },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TransactionHistoryScreen(
    onBack: () -> Unit,
    onOpenOptions: () -> Unit,
    menuErrorMessage: String?,
    hasVirtualCard: Boolean,
    isVirtualCardEnabled: Boolean,
    isUpdatingVirtualCard: Boolean,
    virtualCardErrorMessage: String?,
    virtualCardScanVersion: Int,
    onToggleVirtualCard: () -> Unit,
) {
    val cardViewModel: CarteViewModel = koinViewModel()
    val uiState by cardViewModel.uiState.collectAsState()
    val haptic = rememberAppHaptics()

    // Reprend la synchro à chaque ouverture : utile au retour du réseau après le mode avion.
    LaunchedEffect(Unit) { cardViewModel.loadDashboard() }

    // Retour haptique à l'activation / désactivation de la carte, et si l'opération échoue.
    var previousEnabled by remember { mutableStateOf(isVirtualCardEnabled) }
    LaunchedEffect(isVirtualCardEnabled) {
        if (isVirtualCardEnabled != previousEnabled) {
            previousEnabled = isVirtualCardEnabled
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        }
    }
    LaunchedEffect(virtualCardErrorMessage) {
        if (virtualCardErrorMessage != null) haptic.performHapticFeedback(HapticFeedbackType.Reject)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ThemeConstants.classicBackground)
            .screenTitle("Ma carte")
            .safeContentPadding()
            .padding(horizontal = 12.dp, vertical = 12.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            IconAction(description = "Retour à l'accueil", onClick = onBack) {
                BackIcon(it, contentDescription = null)
            }
            Text(
                text = "Ma carte",
                modifier = Modifier.heading(),
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = ThemeConstants.onClassicBackground,
            )
            IconAction(description = "Plus d'options", onClick = onOpenOptions) {
                MoreIcon(it, contentDescription = null)
            }
        }
        if (menuErrorMessage != null) {
            Spacer(modifier = Modifier.height(8.dp))
            ErrorText(menuErrorMessage)
        }
        Spacer(modifier = Modifier.height(24.dp))
        val cardHolderName = (uiState as? CarteUiState.Dashboard)
            ?.profile
            ?.displayedName
            ?.uppercase()
            ?: "MA CARTE"
        // Un interrupteur pour TalkBack : « Carte virtuelle YATO, interrupteur, activé / désactivé ».
        CreditCard(
            modifier = Modifier
                .fillMaxWidth()
                .toggleable(
                    value = isVirtualCardEnabled,
                    enabled = hasVirtualCard && !isUpdatingVirtualCard,
                    role = Role.Switch,
                    onValueChange = { onToggleVirtualCard() },
                ),
            name = cardHolderName,
            semanticsDescription = "Carte virtuelle $cardHolderName",
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = when {
                isUpdatingVirtualCard -> "Mise à jour de la carte virtuelle…"
                !hasVirtualCard -> "Scanne ta carte physique lors de la connexion pour créer ta carte virtuelle."
                isVirtualCardEnabled -> "Carte virtuelle activée. Appuie sur la carte pour la désactiver."
                else -> "Appuie sur la carte pour activer la carte virtuelle."
            },
            color = if (virtualCardErrorMessage == null) {
                ThemeConstants.accentBackground
            } else {
                ThemeConstants.error
            },
        )
        if (virtualCardErrorMessage != null) {
            Spacer(modifier = Modifier.height(8.dp))
            ErrorText(virtualCardErrorMessage)
        }
        Spacer(modifier = Modifier.height(28.dp))
        // Carte virtuelle active : les transactions laissent la place au panneau NFC, comme un wallet.
        AnimatedContent(
            targetState = isVirtualCardEnabled,
            modifier = Modifier.weight(1f).fillMaxWidth(),
            transitionSpec = { fadeTransition(enterMillis = 320, exitMillis = 220, delayMillis = 120) },
            label = "card_content",
        ) { walletMode ->
            if (walletMode) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    NfcPayPanel(scanVersion = virtualCardScanVersion)
                }
            } else {
                when (val state = uiState) {
                    CarteUiState.Loading -> Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center,
                    ) {
                        LoadingIndicator()
                    }

                    is CarteUiState.Error -> Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                    ) {
                        ErrorText(state.message)
                        TextButton(onClick = cardViewModel::loadDashboard) {
                            Text("Réessayer")
                        }
                    }

                    is CarteUiState.Dashboard -> PullToRefreshBox(
                        isRefreshing = state.isRefreshing,
                        onRefresh = cardViewModel::loadDashboard,
                        // Le tirage est un geste : TalkBack propose « Actualiser » dans ses actions.
                        modifier = Modifier.fillMaxSize().semantics {
                            customActions = listOf(
                                CustomAccessibilityAction("Actualiser les transactions") {
                                    cardViewModel.loadDashboard()
                                    true
                                },
                            )
                        },
                    ) {
                        TransactionList(
                            transactions = state.transactions,
                            modifier = Modifier.fillMaxSize(),
                            lastUpdatedMillis = state.lastUpdatedMillis,
                            isRefreshing = state.isRefreshing,
                            refreshErrorMessage = state.refreshErrorMessage,
                        )
                    }
                }
            }
        }
    }

}

private fun Long.formatEuro(): String {
    val sign = if (this < 0) "−" else ""
    val euros = kotlin.math.abs(this / 100)
    val cents = kotlin.math.abs(this % 100).toString().padStart(2, '0')
    return "$sign$euros,$cents €"
}
