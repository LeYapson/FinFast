package fr.mobilune.componentgallery.viewmodel

import fr.mobilune.componentgallery.abdelrun.AbdelRunAudio
import fr.mobilune.componentgallery.abdelrun.AbdelRunRepositoryImpl
import fr.mobilune.componentgallery.abdelrun.AudioTrack
import fr.mobilune.componentgallery.abdelrun.GAME_VOLUME
import fr.mobilune.componentgallery.abdelrun.GameControls
import fr.mobilune.componentgallery.abdelrun.PlatformAudioPlayer
import fr.mobilune.componentgallery.abdelrun.RunState
import fr.mobilune.componentgallery.projects.DAILY_BONUS_CAP_CENTS
import fr.mobilune.componentgallery.projects.InMemoryPreferencesDataStore
import fr.mobilune.componentgallery.projects.ProjectRepositoryImpl
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

private class SilentAudioPlayer : PlatformAudioPlayer {
    var lastVolume = -1f
    override fun play(track: AudioTrack, bytes: ByteArray, loop: Boolean) = Unit
    override fun stop(track: AudioTrack) = Unit
    override fun pauseAll() = Unit
    override fun resumeAll() = Unit
    override fun setVolume(volume: Float) {
        lastVolume = volume
    }

    override fun release() = Unit
}

/** Parcours de la section « Projets » : création, détail, jeu et conversion du score en bonus. */
@OptIn(ExperimentalCoroutinesApi::class)
class ProjectsViewModelTest {
    private val testDispatcher = StandardTestDispatcher()
    private val dataStore = InMemoryPreferencesDataStore()
    private val projectRepository = ProjectRepositoryImpl(dataStore, { 1_700_000_000_000L })
    private val player = SilentAudioPlayer()

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel() = ProjectsViewModel(
        projectRepository = projectRepository,
        abdelRunRepository = AbdelRunRepositoryImpl(dataStore),
        audio = AbdelRunAudio(player),
        nowMillis = { 1_700_000_000_000L },
    )

    private suspend fun TestScope.createProjectVia(viewModel: ProjectsViewModel) {
        viewModel.startCreate()
        viewModel.updateDraftName("Voyage Japon")
        viewModel.updateDraftTarget("1200")
        viewModel.continueToSchedule()
        viewModel.createProject()
        advanceUntilIdle()
    }

    @Test
    fun `Au départ la liste est vide et le jeu se charge`() = runTest {
        val viewModel = createViewModel()
        advanceUntilIdle()
        assertEquals(GAME_VOLUME, player.lastVolume)
        val state = viewModel.uiState.value
        assertEquals(ProjectsScreen.List, state.screen)
        assertTrue(state.isLoaded)
        assertTrue(state.projects.isEmpty())
        assertNotNull(state.engine)
    }

    @Test
    fun `Créer un projet suit les deux étapes puis arrive sur la confirmation`() = runTest {
        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.startCreate()
        assertEquals(ProjectsScreen.CreateName, viewModel.uiState.value.screen)
        viewModel.updateDraftName("Voyage Japon")
        viewModel.updateDraftTarget("1200")
        viewModel.continueToSchedule()

        var state = viewModel.uiState.value
        assertEquals(ProjectsScreen.CreateSchedule, state.screen)
        assertEquals(6, state.draftMonths)
        assertEquals("200", state.draftMonthly)

        viewModel.changeDraftMonths(+4) // 10 mois
        state = viewModel.uiState.value
        assertEquals(10, state.draftMonths)
        assertEquals("120", state.draftMonthly)

        viewModel.updateDraftMonthly("100") // 12 mois
        assertEquals(12, viewModel.uiState.value.draftMonths)

        viewModel.createProject()
        advanceUntilIdle()
        state = viewModel.uiState.value
        assertEquals(ProjectsScreen.Created, state.screen)
        val project = state.projects.single()
        assertEquals("Voyage Japon", project.name)
        assertEquals(120_000L, project.targetCents)
        assertEquals(10_000L, project.monthlyCents)
        assertEquals(12, project.deadlineMonths)
        assertEquals(project.id, state.selectedId)

        assertTrue(viewModel.onBack())
        assertEquals(ProjectsScreen.List, viewModel.uiState.value.screen)
    }

    @Test
    fun `Un nom vide ou un objectif invalide bloque l'étape 1 avec un message`() = runTest {
        val viewModel = createViewModel()
        advanceUntilIdle()
        viewModel.startCreate()

        viewModel.continueToSchedule()
        assertEquals(ProjectsScreen.CreateName, viewModel.uiState.value.screen)
        assertNotNull(viewModel.uiState.value.errorMessage)

        viewModel.updateDraftName("Vélo")
        viewModel.updateDraftTarget("0")
        viewModel.continueToSchedule()
        assertEquals(ProjectsScreen.CreateName, viewModel.uiState.value.screen)
        assertNotNull(viewModel.uiState.value.errorMessage)

        viewModel.updateDraftTarget("abc") // caractères refusés : la saisie ne change pas
        assertEquals("0", viewModel.uiState.value.draftTarget)
    }

    @Test
    fun `Le retour remonte étape par étape puis quitte la section depuis la liste`() = runTest {
        val viewModel = createViewModel()
        advanceUntilIdle()
        assertFalse(viewModel.onBack(), "depuis la liste on quitte la section")

        viewModel.startCreate()
        viewModel.updateDraftName("Vélo")
        viewModel.updateDraftTarget("500")
        viewModel.continueToSchedule()
        assertTrue(viewModel.onBack())
        assertEquals(ProjectsScreen.CreateName, viewModel.uiState.value.screen)
        assertTrue(viewModel.onBack())
        assertEquals(ProjectsScreen.List, viewModel.uiState.value.screen)
    }

    @Test
    fun `Le jeu s'ouvre avec le logo la première fois puis directement sur le menu`() = runTest {
        val viewModel = createViewModel()
        advanceUntilIdle()
        createProjectVia(viewModel)
        viewModel.openProject(viewModel.uiState.value.projects.single().id)
        assertEquals(ProjectsScreen.Detail, viewModel.uiState.value.screen)

        viewModel.openGame()
        assertEquals(ProjectsScreen.GameSplash, viewModel.uiState.value.screen)
        viewModel.onSplashFinished()
        assertEquals(ProjectsScreen.GameMenu, viewModel.uiState.value.screen)

        viewModel.onBack()
        assertEquals(ProjectsScreen.Detail, viewModel.uiState.value.screen)
        viewModel.openGame()
        assertEquals(ProjectsScreen.GameMenu, viewModel.uiState.value.screen)
    }

    @Test
    fun `Une partie perdue convertit le score en bonus pour le projet`() = runTest {
        val viewModel = createViewModel()
        advanceUntilIdle()
        createProjectVia(viewModel)
        val projectId = viewModel.uiState.value.projects.single().id
        viewModel.openProject(projectId)
        viewModel.openGame()
        viewModel.onSplashFinished()

        viewModel.startRun()
        assertEquals(ProjectsScreen.Playing, viewModel.uiState.value.screen)
        val engine = viewModel.uiState.value.engine!!
        var guard = 0
        while (engine.state != RunState.Dead && guard++ < 3_000) engine.update(1f / 60f, GameControls())
        assertEquals(RunState.Dead, engine.state)

        viewModel.finishRun()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(ProjectsScreen.GameOver, state.screen)
        val result = assertNotNull(state.lastRun)
        assertEquals(engine.scoreInt(), result.score)
        assertEquals(minOf(engine.scoreInt() / 10L, DAILY_BONUS_CAP_CENTS), result.bonusCents)
        assertEquals(result.bonusCents, projectRepository.projects.first().single().savedCents)
        assertEquals(DAILY_BONUS_CAP_CENTS - result.bonusCents, state.dailyRemainingCents)
    }

    @Test
    fun `Quitter une partie sans avoir avancé ne crédite rien et revient au menu`() = runTest {
        val viewModel = createViewModel()
        advanceUntilIdle()
        createProjectVia(viewModel)
        viewModel.openProject(viewModel.uiState.value.projects.single().id)
        viewModel.openGame()
        viewModel.onSplashFinished()

        viewModel.startRun()
        viewModel.onBack()
        advanceUntilIdle()

        assertEquals(ProjectsScreen.GameMenu, viewModel.uiState.value.screen)
        assertEquals(0L, projectRepository.projects.first().single().savedCents)
    }

    @Test
    fun `Supprimer le projet ramène à la liste`() = runTest {
        val viewModel = createViewModel()
        advanceUntilIdle()
        createProjectVia(viewModel)
        viewModel.openProject(viewModel.uiState.value.projects.single().id)
        viewModel.deleteSelectedProject()
        advanceUntilIdle()
        assertEquals(ProjectsScreen.List, viewModel.uiState.value.screen)
        assertTrue(viewModel.uiState.value.projects.isEmpty())
        assertNull(viewModel.uiState.value.selectedProject)
    }
}
