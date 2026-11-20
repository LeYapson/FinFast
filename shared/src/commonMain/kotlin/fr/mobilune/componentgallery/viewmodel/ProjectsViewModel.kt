package fr.mobilune.componentgallery.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import fr.mobilune.componentgallery.abdelrun.AbdelRunAudio
import fr.mobilune.componentgallery.abdelrun.AbdelRunRepository
import fr.mobilune.componentgallery.abdelrun.GAME_VOLUME
import fr.mobilune.componentgallery.abdelrun.GameEngine
import fr.mobilune.componentgallery.projects.DAILY_BONUS_CAP_CENTS
import fr.mobilune.componentgallery.projects.DEFAULT_DEADLINE_MONTHS
import fr.mobilune.componentgallery.projects.MAX_DEADLINE_MONTHS
import fr.mobilune.componentgallery.projects.MAX_PROJECT_NAME_LENGTH
import fr.mobilune.componentgallery.projects.MIN_DEADLINE_MONTHS
import fr.mobilune.componentgallery.projects.Project
import fr.mobilune.componentgallery.projects.ProjectRepository
import fr.mobilune.componentgallery.projects.RunResult
import fr.mobilune.componentgallery.projects.formatEuros
import fr.mobilune.componentgallery.projects.monthlyCentsFor
import fr.mobilune.componentgallery.projects.monthsFor
import fr.mobilune.componentgallery.projects.parseAmountCents
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Écrans de la section « Projets » (de la liste jusqu'au jeu AbdelRun). */
enum class ProjectsScreen {
    List,
    CreateName,
    CreateSchedule,
    Created,
    Detail,
    GameSplash,
    GameMenu,
    Playing,
    GameOver,
}

/** Écrans du jeu : paysage, plein écran. */
fun ProjectsScreen.isGameScreen(): Boolean = this.ordinal >= ProjectsScreen.GameSplash.ordinal

data class ProjectsUiState(
    val screen: ProjectsScreen = ProjectsScreen.List,
    val isLoaded: Boolean = false,
    val projects: List<Project> = emptyList(),
    val selectedId: String? = null,
    val draftName: String = "",
    val draftTarget: String = "",
    val draftMonths: Int = DEFAULT_DEADLINE_MONTHS,
    val draftMonthly: String = "",
    val errorMessage: String? = null,
    val isSaving: Boolean = false,
    val createdProjectName: String = "",
    /** Bonus encore créditable aujourd'hui, en centimes. */
    val dailyRemainingCents: Long = DAILY_BONUS_CAP_CENTS,
    val engine: GameEngine? = null,
    /** Incrémenté à chaque nouvelle partie : repart d'un moteur propre et d'un écran neuf. */
    val runId: Int = 0,
    val lastRun: RunResult? = null,
    val isFinishingRun: Boolean = false,
) {
    val selectedProject: Project? get() = projects.firstOrNull { it.id == selectedId }
}

class ProjectsViewModel(
    private val projectRepository: ProjectRepository,
    private val abdelRunRepository: AbdelRunRepository,
    val audio: AbdelRunAudio,
    private val nowMillis: () -> Long,
) : ViewModel() {
    private val mutableUiState = MutableStateFlow(ProjectsUiState())
    val uiState: StateFlow<ProjectsUiState> = mutableUiState.asStateFlow()

    private var splashShown = false

    init {
        viewModelScope.launch {
            projectRepository.projects.collect { list ->
                mutableUiState.update { it.copy(projects = list, isLoaded = true) }
            }
        }
        viewModelScope.launch {
            try {
                val save = abdelRunRepository.load()
                val engine = GameEngine(save = save, onSaveChanged = ::persistSave)
                audio.setVolume(GAME_VOLUME)
                mutableUiState.update { it.copy(engine = engine) }
            } catch (exception: CancellationException) {
                throw exception
            } catch (_: Exception) {
                // Sans sauvegarde lisible on repart de zéro plutôt que de bloquer le jeu.
                val engine = GameEngine(onSaveChanged = ::persistSave)
                mutableUiState.update { it.copy(engine = engine) }
            }
        }
        refreshDailyBonus()
    }

    // ---------------------------------------------------------------------------------------------
    // Navigation
    // ---------------------------------------------------------------------------------------------

    /** Retour système ou flèche retour. Renvoie faux quand il faut quitter la section « Projets ». */
    fun onBack(): Boolean {
        val state = mutableUiState.value
        if (state.isSaving || state.isFinishingRun) return true
        when (state.screen) {
            ProjectsScreen.List -> return false
            ProjectsScreen.CreateName, ProjectsScreen.Created, ProjectsScreen.Detail -> goTo(ProjectsScreen.List)
            ProjectsScreen.CreateSchedule -> goTo(ProjectsScreen.CreateName)
            ProjectsScreen.GameSplash, ProjectsScreen.GameMenu -> {
                audio.stop()
                goTo(ProjectsScreen.Detail)
            }
            ProjectsScreen.Playing -> leaveRun()
            ProjectsScreen.GameOver -> goTo(ProjectsScreen.Detail)
        }
        return true
    }

    private fun goTo(screen: ProjectsScreen) {
        mutableUiState.update { it.copy(screen = screen, errorMessage = null) }
        if (screen == ProjectsScreen.Detail || screen == ProjectsScreen.List) refreshDailyBonus()
    }

    /** À l'ouverture de la section depuis l'accueil. */
    fun reset() {
        mutableUiState.update {
            it.copy(screen = ProjectsScreen.List, selectedId = null, errorMessage = null, isSaving = false)
        }
        refreshDailyBonus()
    }

    /** Quand on quitte la section : plus de musique, la partie en cours est abandonnée proprement. */
    fun onLeave() {
        audio.stop()
        mutableUiState.value.engine?.abandon()
        mutableUiState.update { it.copy(isFinishingRun = false) }
    }

    private fun refreshDailyBonus() {
        viewModelScope.launch {
            try {
                val remaining = projectRepository.remainingDailyBonusCents()
                mutableUiState.update { it.copy(dailyRemainingCents = remaining) }
            } catch (exception: CancellationException) {
                throw exception
            } catch (_: Exception) {
                // L'info est indicative : le plafond est de toute façon appliqué au crédit.
            }
        }
    }

    // ---------------------------------------------------------------------------------------------
    // Création d'un projet
    // ---------------------------------------------------------------------------------------------

    fun startCreate() {
        mutableUiState.update {
            it.copy(
                screen = ProjectsScreen.CreateName,
                draftName = "",
                draftTarget = "",
                draftMonths = DEFAULT_DEADLINE_MONTHS,
                draftMonthly = "",
                errorMessage = null,
            )
        }
    }

    fun updateDraftName(value: String) {
        if (value.length > MAX_PROJECT_NAME_LENGTH) return
        mutableUiState.update { it.copy(draftName = value, errorMessage = null) }
    }

    fun updateDraftTarget(value: String) {
        if (value.length > 9 || value.any { !it.isDigit() && it != ',' && it != '.' }) return
        mutableUiState.update { it.copy(draftTarget = value, errorMessage = null) }
    }

    fun continueToSchedule() {
        val state = mutableUiState.value
        val target = parseAmountCents(state.draftTarget)
        val error = when {
            state.draftName.isBlank() -> "Donne un nom à ton projet."
            target == null || target <= 0 -> "Saisis un objectif valide supérieur à 0 € (2 décimales maximum)."
            else -> null
        }
        if (error != null) {
            mutableUiState.update { it.copy(errorMessage = error) }
            return
        }
        val months = state.draftMonths.coerceIn(MIN_DEADLINE_MONTHS, MAX_DEADLINE_MONTHS)
        mutableUiState.update {
            it.copy(
                screen = ProjectsScreen.CreateSchedule,
                draftMonths = months,
                draftMonthly = centsToInput(monthlyCentsFor(target!!, months)),
                errorMessage = null,
            )
        }
    }

    fun changeDraftMonths(delta: Int) {
        val state = mutableUiState.value
        val target = parseAmountCents(state.draftTarget) ?: return
        val months = (state.draftMonths + delta).coerceIn(MIN_DEADLINE_MONTHS, MAX_DEADLINE_MONTHS)
        mutableUiState.update {
            it.copy(
                draftMonths = months,
                draftMonthly = centsToInput(monthlyCentsFor(target, months)),
                errorMessage = null,
            )
        }
    }

    fun updateDraftMonthly(value: String) {
        if (value.length > 9 || value.any { !it.isDigit() && it != ',' && it != '.' }) return
        val state = mutableUiState.value
        val target = parseAmountCents(state.draftTarget)
        val monthly = parseAmountCents(value)
        // Une mensualité valide recalcule l'échéance ; sinon on garde la saisie telle quelle.
        val months = if (target != null && monthly != null && monthly > 0) {
            monthsFor(target, monthly)
        } else {
            state.draftMonths
        }
        mutableUiState.update { it.copy(draftMonthly = value, draftMonths = months, errorMessage = null) }
    }

    fun createProject() {
        val state = mutableUiState.value
        if (state.isSaving) return
        val target = parseAmountCents(state.draftTarget)
        val monthly = parseAmountCents(state.draftMonthly)
        val error = when {
            state.draftName.isBlank() -> "Donne un nom à ton projet."
            target == null || target <= 0 -> "Saisis un objectif valide supérieur à 0 €."
            monthly == null || monthly <= 0 -> "Saisis une somme mensuelle valide supérieure à 0 €."
            else -> null
        }
        if (error != null) {
            mutableUiState.update { it.copy(errorMessage = error) }
            return
        }
        mutableUiState.update { it.copy(isSaving = true, errorMessage = null) }
        viewModelScope.launch {
            try {
                val project = projectRepository.create(
                    name = state.draftName,
                    targetCents = target!!,
                    monthlyCents = monthly!!,
                    deadlineMonths = monthsFor(target, monthly),
                )
                mutableUiState.update {
                    it.copy(
                        screen = ProjectsScreen.Created,
                        selectedId = project.id,
                        createdProjectName = project.name,
                        isSaving = false,
                    )
                }
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: Exception) {
                mutableUiState.update {
                    it.copy(isSaving = false, errorMessage = exception.message ?: "Impossible de créer le projet.")
                }
            }
        }
    }

    fun openProject(projectId: String) {
        mutableUiState.update { it.copy(screen = ProjectsScreen.Detail, selectedId = projectId, errorMessage = null) }
        refreshDailyBonus()
    }

    fun deleteSelectedProject() {
        val id = mutableUiState.value.selectedId ?: return
        viewModelScope.launch {
            try {
                projectRepository.delete(id)
                mutableUiState.update { it.copy(screen = ProjectsScreen.List, selectedId = null, errorMessage = null) }
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: Exception) {
                mutableUiState.update { it.copy(errorMessage = exception.message ?: "Impossible de supprimer le projet.") }
            }
        }
    }

    // ---------------------------------------------------------------------------------------------
    // Jeu AbdelRun
    // ---------------------------------------------------------------------------------------------

    fun openGame() {
        val screen = if (splashShown) ProjectsScreen.GameMenu else ProjectsScreen.GameSplash
        mutableUiState.update { it.copy(screen = screen, errorMessage = null) }
        refreshDailyBonus()
    }

    fun onSplashFinished() {
        splashShown = true
        if (mutableUiState.value.screen == ProjectsScreen.GameSplash) goTo(ProjectsScreen.GameMenu)
    }

    fun showGameMenu() = goTo(ProjectsScreen.GameMenu)

    fun startRun() {
        val engine = mutableUiState.value.engine ?: return
        engine.reset()
        mutableUiState.update {
            it.copy(
                screen = ProjectsScreen.Playing,
                runId = it.runId + 1,
                lastRun = null,
                isFinishingRun = false,
                errorMessage = null,
            )
        }
    }

    /** Le joueur a quitté la partie avec « Retour » : ce qu'il a couru compte quand même. */
    private fun leaveRun() {
        val engine = mutableUiState.value.engine
        engine?.abandon()
        finishRun(abandoned = true)
    }

    /** Fin de partie (mort du cube ou abandon) : convertit le score en bonus pour le projet. */
    fun finishRun(abandoned: Boolean = false) {
        val state = mutableUiState.value
        val engine = state.engine ?: return
        if (state.screen != ProjectsScreen.Playing || state.isFinishingRun) return
        val projectId = state.selectedId
        val score = engine.scoreInt()
        if (projectId == null || (abandoned && score <= 0)) {
            engine.reset()
            goTo(ProjectsScreen.GameMenu)
            return
        }
        mutableUiState.update { it.copy(isFinishingRun = true) }
        viewModelScope.launch {
            try {
                val result = projectRepository.creditBonus(projectId, score)
                val remaining = projectRepository.remainingDailyBonusCents()
                mutableUiState.update {
                    it.copy(
                        screen = if (result != null) ProjectsScreen.GameOver else ProjectsScreen.GameMenu,
                        lastRun = result,
                        dailyRemainingCents = remaining,
                        isFinishingRun = false,
                        errorMessage = if (result == null) "Le projet n'existe plus." else null,
                    )
                }
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: Exception) {
                mutableUiState.update {
                    it.copy(
                        screen = ProjectsScreen.GameMenu,
                        isFinishingRun = false,
                        errorMessage = exception.message ?: "Impossible d'enregistrer ton gain.",
                    )
                }
            }
        }
    }

    private fun persistSave() {
        val engine = mutableUiState.value.engine ?: return
        viewModelScope.launch {
            try {
                abdelRunRepository.write(engine.save)
            } catch (exception: CancellationException) {
                throw exception
            } catch (_: Exception) {
                // La progression de jeu n'est pas critique : on réessaiera à la prochaine sauvegarde.
            }
        }
    }

    override fun onCleared() {
        audio.release()
        super.onCleared()
    }

    private fun centsToInput(cents: Long): String =
        if (cents % 100 == 0L) (cents / 100).toString() else formatEuros(cents).removeSuffix(" €")
}
