package fr.mobilune.componentgallery.projects

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.clickable
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.runtime.collectAsState
import fr.mobilune.componentgallery.BackIcon
import fr.mobilune.componentgallery.ErrorText
import fr.mobilune.componentgallery.IconAction
import fr.mobilune.componentgallery.LoadingIndicator
import fr.mobilune.componentgallery.MoreIcon
import fr.mobilune.componentgallery.ScanSuccessBadge
import fr.mobilune.componentgallery.SystemBackHandler
import fr.mobilune.componentgallery.ThemeConstants
import fr.mobilune.componentgallery.abdelrun.AudioTrack
import fr.mobilune.componentgallery.abdelrun.ForceLandscape
import fr.mobilune.componentgallery.abdelrun.GameMenuScreen
import fr.mobilune.componentgallery.abdelrun.GameOverScreen
import fr.mobilune.componentgallery.abdelrun.GamePlayScreen
import fr.mobilune.componentgallery.abdelrun.GameSplashScreen
import fr.mobilune.componentgallery.formatLastUpdate
import fr.mobilune.componentgallery.heading
import fr.mobilune.componentgallery.screenTitle
import fr.mobilune.componentgallery.viewmodel.ProjectsScreen
import fr.mobilune.componentgallery.viewmodel.ProjectsUiState
import fr.mobilune.componentgallery.viewmodel.ProjectsViewModel
import fr.mobilune.componentgallery.viewmodel.isGameScreen
import org.koin.compose.viewmodel.koinViewModel

/** Racine de la section « Projets » : pages en portrait, jeu AbdelRun en paysage. */
@Composable
fun ProjectsRoot(onLeave: () -> Unit, onOpenOptions: () -> Unit) {
    val viewModel: ProjectsViewModel = koinViewModel()
    val state by viewModel.uiState.collectAsState()

    SystemBackHandler(enabled = true) { if (!viewModel.onBack()) onLeave() }
    ForceLandscape(enabled = state.screen.isGameScreen())
    DisposableEffect(viewModel) { onDispose { viewModel.onLeave() } }

    // Musique des menus du jeu ; la musique de partie est gérée par l'écran de jeu.
    LaunchedEffect(state.screen) {
        when (state.screen) {
            ProjectsScreen.GameMenu -> viewModel.audio.play(AudioTrack.Menu)

            ProjectsScreen.GameOver -> viewModel.audio.play(AudioTrack.GameOver)
            ProjectsScreen.Playing -> Unit
            else -> viewModel.audio.stop()
        }
    }

    val engine = state.engine
    val project = state.selectedProject
    when (state.screen) {
        ProjectsScreen.List -> ProjectListScreen(
            state = state,
            onBack = onLeave,
            onOpenOptions = onOpenOptions,
            onCreate = viewModel::startCreate,
            onOpenProject = viewModel::openProject,
        )

        ProjectsScreen.CreateName -> CreateNameScreen(
            state = state,
            onBack = { viewModel.onBack() },
            onOpenOptions = onOpenOptions,
            onNameChange = viewModel::updateDraftName,
            onTargetChange = viewModel::updateDraftTarget,
            onNext = viewModel::continueToSchedule,
        )

        ProjectsScreen.CreateSchedule -> CreateScheduleScreen(
            state = state,
            onBack = { viewModel.onBack() },
            onOpenOptions = onOpenOptions,
            onMonthsDelta = viewModel::changeDraftMonths,
            onMonthlyChange = viewModel::updateDraftMonthly,
            onCreate = viewModel::createProject,
        )

        ProjectsScreen.Created -> ProjectCreatedScreen(
            name = state.createdProjectName,
            onSeeProjects = { viewModel.onBack() },
        )

        ProjectsScreen.Detail -> if (project == null) {
            LaunchedEffect(Unit) { viewModel.onBack() }
        } else {
            ProjectDetailScreen(
                project = project,
                dailyRemainingCents = state.dailyRemainingCents,
                errorMessage = state.errorMessage,
                onBack = { viewModel.onBack() },
                onOpenOptions = onOpenOptions,
                onPlay = viewModel::openGame,
                onDelete = viewModel::deleteSelectedProject,
            )
        }

        // --- Jeu AbdelRun (paysage) ---
        ProjectsScreen.GameSplash -> GameSplashScreen(onFinished = viewModel::onSplashFinished)

        ProjectsScreen.GameMenu -> GameMenuScreen(
            projectName = project?.name.orEmpty(),
            dailyRemainingCents = state.dailyRemainingCents,
            errorMessage = state.errorMessage,
            onPlay = viewModel::startRun,
            onBack = { viewModel.onBack() },
        )

        ProjectsScreen.Playing -> if (engine != null) {
            // Une clé par partie : l'écran repart d'un état neuf à chaque « Rejouer ».
            androidx.compose.runtime.key(state.runId) {
                GamePlayScreen(
                    engine = engine,
                    audio = viewModel.audio,
                    onDead = { viewModel.finishRun() },
                    onExit = { viewModel.onBack() },
                )
            }
        }

        ProjectsScreen.GameOver -> {
            val result = state.lastRun
            if (result == null) {
                LaunchedEffect(Unit) { viewModel.onBack() }
            } else {
                GameOverScreen(
                    result = result,
                    onRestart = viewModel::startRun,
                    onMenu = viewModel::showGameMenu,
                    onBackToProject = { viewModel.onBack() },
                )
            }
        }
    }
}

// -------------------------------------------------------------------------------------------------
// Briques communes (portrait)
// -------------------------------------------------------------------------------------------------

@Composable
private fun ProjectsPage(
    title: String,
    onBack: () -> Unit,
    onOpenOptions: () -> Unit,
    modifier: Modifier = Modifier,
    trailing: String? = null,
    bottomBar: @Composable (() -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(ThemeConstants.classicBackground)
            .screenTitle(title)
            .safeContentPadding()
            .padding(horizontal = 12.dp, vertical = 12.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            IconAction(description = "Retour", onClick = onBack) { BackIcon(it, contentDescription = null) }
            Text(
                text = title,
                modifier = Modifier.weight(1f).heading(),
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = ThemeConstants.onClassicBackground,
                textAlign = TextAlign.Center,
                maxLines = 1,
            )
            if (trailing != null) {
                Box(Modifier.size(48.dp), contentAlignment = Alignment.Center) {
                    Text(
                        text = trailing,
                        fontSize = 14.sp,
                        color = ThemeConstants.secondaryText,
                        textAlign = TextAlign.Center,
                    )
                }
            } else {
                IconAction(description = "Plus d'options", onClick = onOpenOptions) {
                    MoreIcon(it, contentDescription = null)
                }
            }
        }
        Spacer(Modifier.height(16.dp))
        Column(
            modifier = Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()),
        ) { content() }
        if (bottomBar != null) {
            Spacer(Modifier.height(12.dp))
            bottomBar()
        }
    }
}

@Composable
private fun PrimaryButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.fillMaxWidth().heightIn(min = 48.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = ThemeConstants.accentBackground,
            contentColor = ThemeConstants.onAccentBackground,
        ),
    ) { Text(text) }
}

/** Barre d'avancement + « 120 € / 1 200 € ». */
@Composable
private fun ProjectProgress(project: Project, onAccent: Boolean) {
    val textColor = if (onAccent) ThemeConstants.onAccentBackground else ThemeConstants.onClassicBackground
    val track = textColor.copy(alpha = 0.25f)
    Column(
        modifier = Modifier.fillMaxWidth().semantics(mergeDescendants = true) {
            contentDescription = "${formatEurosShort(project.savedCents)} épargnés sur ${formatEurosShort(project.targetCents)}, " +
                "${(project.progress * 100).toInt()} pour cent"
        },
    ) {
        Box(
            Modifier.fillMaxWidth().height(10.dp).clip(RoundedCornerShape(50)).background(track),
        ) {
            Box(
                Modifier
                    .fillMaxWidth(project.progress)
                    .height(10.dp)
                    .clip(RoundedCornerShape(50))
                    .background(if (onAccent) Color(0xFF7FE574) else ThemeConstants.accentBackground),
            )
        }
        Spacer(Modifier.height(6.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(formatEurosShort(project.savedCents), color = textColor, fontSize = 14.sp, fontWeight = FontWeight.Medium)
            Text(formatEurosShort(project.targetCents), color = textColor, fontSize = 14.sp)
        }
    }
}

private fun targetLine(project: Project): String =
    if (project.isReached) {
        "Objectif atteint !"
    } else {
        "Prévu pour ${formatTargetMonth(project.createdAtMillis, project.deadlineMonths)}"
    }

/** Carte d'un projet : nom, avancement et échéance. */
@Composable
private fun ProjectCard(project: Project, onClick: (() -> Unit)?, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(ThemeConstants.accentBackground)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(
            text = project.name,
            color = ThemeConstants.onAccentBackground,
            fontSize = 18.sp,
            fontWeight = FontWeight.Medium,
        )
        Text(
            text = formatEurosShort(project.savedCents),
            color = ThemeConstants.onAccentBackground,
            fontSize = 32.sp,
            fontWeight = FontWeight.Bold,
        )
        ProjectProgress(project, onAccent = true)
        Text(
            text = targetLine(project),
            color = ThemeConstants.onAccentBackground,
            fontSize = 14.sp,
            fontWeight = FontWeight.Light,
        )
    }
}

// -------------------------------------------------------------------------------------------------
// Pages
// -------------------------------------------------------------------------------------------------

@Composable
private fun ProjectListScreen(
    state: ProjectsUiState,
    onBack: () -> Unit,
    onOpenOptions: () -> Unit,
    onCreate: () -> Unit,
    onOpenProject: (String) -> Unit,
) {
    ProjectsPage(
        title = "Projets",
        onBack = onBack,
        onOpenOptions = onOpenOptions,
        bottomBar = if (state.isLoaded && state.projects.isNotEmpty()) {
            { PrimaryButton("+ Nouveau projet", onCreate) }
        } else {
            null
        },
    ) {
        when {
            !state.isLoaded -> Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                LoadingIndicator()
            }

            state.projects.isEmpty() -> EmptyProjects(onCreate)
            else -> Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                state.projects.forEach { project ->
                    ProjectCard(project, onClick = { onOpenProject(project.id) })
                }
            }
        }
    }
}

/** État vide : première visite, aucun projet. */
@Composable
private fun EmptyProjects(onCreate: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(top = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("🐷", fontSize = 80.sp, modifier = Modifier.clearAndSetSemantics {})
        Text(
            text = "Aucun projet pour l'instant",
            modifier = Modifier.heading(),
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = ThemeConstants.onClassicBackground,
            textAlign = TextAlign.Center,
        )
        Text(
            text = "Crée ton premier projet d'épargne : un voyage, un vélo… " +
                "Puis fais-le avancer en jouant à AbdelRun.",
            fontSize = 15.sp,
            color = ThemeConstants.secondaryText,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(8.dp))
        PrimaryButton("Créer mon premier projet", onCreate)
    }
}

@Composable
private fun CreateNameScreen(
    state: ProjectsUiState,
    onBack: () -> Unit,
    onOpenOptions: () -> Unit,
    onNameChange: (String) -> Unit,
    onTargetChange: (String) -> Unit,
    onNext: () -> Unit,
) {
    ProjectsPage(
        title = "Nouveau projet",
        trailing = "1/2",
        onBack = onBack,
        onOpenOptions = onOpenOptions,
        bottomBar = { PrimaryButton("Suivant", onNext) },
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            OutlinedTextField(
                value = state.draftName,
                onValueChange = onNameChange,
                label = { Text("Nom du projet") },
                placeholder = { Text("Voyage au Japon") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = state.draftTarget,
                onValueChange = onTargetChange,
                label = { Text("Objectif en €") },
                placeholder = { Text("1200") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth(),
            )
            if (state.errorMessage != null) ErrorText(state.errorMessage)
        }
    }
}

@Composable
private fun CreateScheduleScreen(
    state: ProjectsUiState,
    onBack: () -> Unit,
    onOpenOptions: () -> Unit,
    onMonthsDelta: (Int) -> Unit,
    onMonthlyChange: (String) -> Unit,
    onCreate: () -> Unit,
) {
    // Le mois d'arrivée se calcule depuis maintenant ; une seule lecture suffit à l'affichage.
    val nowMillis = remember { currentTimeMillis() }
    ProjectsPage(
        title = "Nouveau projet",
        trailing = "2/2",
        onBack = onBack,
        onOpenOptions = onOpenOptions,
        bottomBar = { PrimaryButton(if (state.isSaving) "Création…" else "Créer le projet", onCreate, enabled = !state.isSaving) },
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text(
                text = "Pour quand ?",
                modifier = Modifier.heading(),
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = ThemeConstants.onClassicBackground,
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                OutlinedButton(
                    onClick = { onMonthsDelta(-1) },
                    modifier = Modifier.size(48.dp).semantics { contentDescription = "Un mois de moins" },
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp),
                ) { Text("−", fontSize = 22.sp) }
                Text(
                    text = if (state.draftMonths == 1) "1 mois" else "${state.draftMonths} mois",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = ThemeConstants.onClassicBackground,
                    modifier = Modifier.semantics { contentDescription = "Durée : ${state.draftMonths} mois" },
                )
                OutlinedButton(
                    onClick = { onMonthsDelta(1) },
                    modifier = Modifier.size(48.dp).semantics { contentDescription = "Un mois de plus" },
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp),
                ) { Text("+", fontSize = 22.sp) }
            }
            OutlinedTextField(
                value = state.draftMonthly,
                onValueChange = onMonthlyChange,
                label = { Text("Je mets combien par mois ? (€)") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth(),
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(ThemeConstants.incomeBackground)
                    .padding(16.dp)
                    .semantics(mergeDescendants = true) {},
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Objectif atteint en ${formatTargetMonth(nowMillis, state.draftMonths)}",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFF060606),
                )
            }
            if (state.errorMessage != null) ErrorText(state.errorMessage)
        }
    }
}

@Composable
private fun ProjectCreatedScreen(name: String, onSeeProjects: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ThemeConstants.classicBackground)
            .screenTitle("Projet créé")
            .safeContentPadding()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        ScanSuccessBadge(Modifier.size(160.dp))
        Spacer(Modifier.height(16.dp))
        Text(
            text = "Projet créé",
            modifier = Modifier.heading(),
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = ThemeConstants.onClassicBackground,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = "« $name » est prêt. Joue à AbdelRun pour le faire avancer.",
            fontSize = 15.sp,
            color = ThemeConstants.secondaryText,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(24.dp))
        PrimaryButton("Voir mes projets", onSeeProjects)
    }
}

@Composable
private fun ProjectDetailScreen(
    project: Project,
    dailyRemainingCents: Long,
    errorMessage: String?,
    onBack: () -> Unit,
    onOpenOptions: () -> Unit,
    onPlay: () -> Unit,
    onDelete: () -> Unit,
) {
    var confirmDelete by remember { mutableStateOf(false) }
    ProjectsPage(title = project.name, onBack = onBack, onOpenOptions = onOpenOptions) {
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            ProjectCard(project, onClick = null)
            if (errorMessage != null) ErrorText(errorMessage)

            // Carte « Jouer » : point d'entrée du jeu.
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .background(ThemeConstants.outcomeBackground)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = "Gagne de l'argent en jouant",
                    modifier = Modifier.heading(),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF060606),
                )
                Text(
                    text = "Chaque partie d'AbdelRun convertit ton score en bonus pour « ${project.name} » " +
                        "(10 points = 1 centime).",
                    fontSize = 14.sp,
                    color = Color(0xFF3F3F3F),
                )
                Text(
                    text = if (dailyRemainingCents > 0) {
                        "Gain possible aujourd'hui : ${formatEuros(dailyRemainingCents)} " +
                            "(plafond de ${formatEurosShort(DAILY_BONUS_CAP_CENTS)} par jour)"
                    } else {
                        "Plafond du jour atteint : reviens demain."
                    },
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFF3F3F3F),
                )
                PrimaryButton("Jouer à AbdelRun", onPlay)
            }

            Text(
                text = "Gains",
                modifier = Modifier.heading(),
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = ThemeConstants.onClassicBackground,
            )
            if (project.gains.isEmpty()) {
                Text(
                    text = "Aucun gain pour l'instant.",
                    fontSize = 15.sp,
                    color = ThemeConstants.secondaryText,
                )
            } else {
                project.gains.forEach { gain -> GainRow(gain) }
            }

            TextButton(onClick = { confirmDelete = true }, modifier = Modifier.align(Alignment.CenterHorizontally)) {
                Text("Supprimer le projet", color = ThemeConstants.error)
            }
        }
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("Supprimer ce projet ?") },
            text = { Text("« ${project.name} » et son historique de gains seront supprimés.") },
            confirmButton = {
                TextButton(onClick = {
                    confirmDelete = false
                    onDelete()
                }) { Text("Supprimer", color = ThemeConstants.error) }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("Annuler") } },
        )
    }
}

@Composable
private fun GainRow(gain: ProjectGain) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .semantics(mergeDescendants = true) {},
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                text = "Partie d'AbdelRun",
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                color = ThemeConstants.onClassicBackground,
            )
            Text(
                text = "Score ${gain.score} · ${formatLastUpdate(gain.timestampMillis)}",
                fontSize = 13.sp,
                color = ThemeConstants.secondaryText,
            )
        }
        Text(
            text = "+${formatEuros(gain.cents)}",
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = ThemeConstants.accentBackground,
        )
    }
}
