package fr.mobilune.componentgallery.projects

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** DataStore en mémoire : suffit pour tester les dépôts sans fichier. */
class InMemoryPreferencesDataStore : DataStore<Preferences> {
    private val state = MutableStateFlow(emptyPreferences())
    override val data: Flow<Preferences> = state

    override suspend fun updateData(transform: suspend (t: Preferences) -> Preferences): Preferences {
        val updated = transform(state.value)
        state.value = updated
        return updated
    }
}

class ProjectsTest {
    private val dayMillis = 86_400_000L

    // 14 novembre 2023, 22:13 UTC
    private val nov2023 = 1_700_000_000_000L

    private var now = nov2023
    private val repository = ProjectRepositoryImpl(InMemoryPreferencesDataStore(), { now }, newId = counter())

    private fun counter(): () -> String {
        var n = 0
        return { "p${++n}" }
    }

    // --- Montants ---

    @Test
    fun `Les montants saisis se convertissent en centimes`() {
        assertEquals(1200_00L, parseAmountCents("1200"))
        assertEquals(1250L, parseAmountCents("12,5"))
        assertEquals(1205L, parseAmountCents("12.05"))
        assertNull(parseAmountCents(""))
        assertNull(parseAmountCents("12,345"))
        assertNull(parseAmountCents("a"))
        assertNull(parseAmountCents("1.2.3"))
    }

    @Test
    fun `Les montants s'affichent comme sur l'accueil`() {
        assertEquals("12,50 €", formatEuros(1250))
        assertEquals("0,05 €", formatEuros(5))
        assertEquals("−3,00 €", formatEuros(-300))
        assertEquals("1200 €", formatEurosShort(120_000))
        assertEquals("12,50 €", formatEurosShort(1250))
    }

    @Test
    fun `Score et bonus sont liés par 10 points pour 1 centime`() {
        assertEquals(0L, bonusCentsForScore(9))
        assertEquals(1L, bonusCentsForScore(10))
        assertEquals(184L, bonusCentsForScore(1840))
        assertEquals(0L, bonusCentsForScore(-50))
    }

    @Test
    fun `La mensualité et la durée se déduisent l'une de l'autre`() {
        assertEquals(20_000L, monthlyCentsFor(120_000, 6))
        assertEquals(10_000L, monthlyCentsFor(100_000, 10))
        assertEquals(3_334L, monthlyCentsFor(10_000, 3), "arrondi au centime supérieur")
        assertEquals(6, monthsFor(120_000, 20_000))
        assertEquals(7, monthsFor(120_000, 19_000))
        assertEquals(MAX_DEADLINE_MONTHS, monthsFor(120_000, 1))
        assertEquals(MIN_DEADLINE_MONTHS, monthsFor(100, 1_000_000))
    }

    @Test
    fun `Le mois d'arrivée est calculé depuis la date du jour`() {
        assertEquals(2023 to 11, civilMonth(nov2023))
        assertEquals(1970 to 1, civilMonth(0))
        assertEquals(2024 to 2, civilMonth(1_709_164_800_000L)) // 29 février 2024
        assertEquals("novembre 2023", formatTargetMonth(nov2023, 0))
        assertEquals("mars 2024", formatTargetMonth(nov2023, 4))
        assertEquals("décembre 2024", formatTargetMonth(nov2023, 13))
    }

    // --- Dépôt ---

    @Test
    fun `Un projet créé est relu avec ses valeurs`() = runTest {
        val project = repository.create("  Voyage Japon ", 120_000, 20_000, 6)
        val stored = repository.projects.first().single()
        assertEquals(project, stored)
        assertEquals("Voyage Japon", stored.name)
        assertEquals(0L, stored.savedCents)
        assertEquals(nov2023, stored.createdAtMillis)
    }

    @Test
    fun `Supprimer retire uniquement le projet visé`() = runTest {
        val a = repository.create("A", 1000, 100, 10)
        val b = repository.create("B", 2000, 200, 10)
        repository.delete(a.id)
        assertEquals(listOf(b.id), repository.projects.first().map { it.id })
    }

    @Test
    fun `Le bonus d'une partie est crédité et historisé`() = runTest {
        val project = repository.create("Vélo", 50_000, 5_000, 10)
        val result = repository.creditBonus(project.id, score = 450)!!
        assertEquals(45L, result.bonusCents)
        assertFalse(result.cappedByDailyLimit)
        assertEquals(0L, result.savedBeforeCents)
        assertEquals(45L, result.savedAfterCents)
        val stored = repository.projects.first().single()
        assertEquals(45L, stored.savedCents)
        assertEquals(1, stored.gains.size)
        assertEquals(450, stored.gains.single().score)
        assertEquals(100L - 45L, repository.remainingDailyBonusCents())
    }

    @Test
    fun `Le plafond quotidien limite le gain puis se réinitialise le lendemain`() = runTest {
        val project = repository.create("Vélo", 50_000, 5_000, 10)

        val first = repository.creditBonus(project.id, score = 5_000)!! // 500 centimes demandés
        assertEquals(DAILY_BONUS_CAP_CENTS, first.bonusCents)
        assertTrue(first.cappedByDailyLimit)
        assertEquals(0L, repository.remainingDailyBonusCents())

        val second = repository.creditBonus(project.id, score = 2_000)!!
        assertEquals(0L, second.bonusCents, "plafond déjà atteint aujourd'hui")
        assertTrue(second.cappedByDailyLimit)
        assertEquals(1, repository.projects.first().single().gains.size, "pas de gain à 0 dans l'historique")

        now += dayMillis
        assertEquals(DAILY_BONUS_CAP_CENTS, repository.remainingDailyBonusCents())
        val third = repository.creditBonus(project.id, score = 300)!!
        assertEquals(30L, third.bonusCents)
        assertEquals(DAILY_BONUS_CAP_CENTS + 30, repository.projects.first().single().savedCents)
    }

    @Test
    fun `Le plafond est partagé entre tous les projets`() = runTest {
        val a = repository.create("A", 10_000, 1_000, 10)
        val b = repository.create("B", 10_000, 1_000, 10)
        repository.creditBonus(a.id, score = 700) // 70 centimes
        val result = repository.creditBonus(b.id, score = 700)!! // 70 demandés, 30 restants
        assertEquals(30L, result.bonusCents)
        assertTrue(result.cappedByDailyLimit)
    }

    @Test
    fun `Créditer un projet supprimé ne fait rien`() = runTest {
        assertNull(repository.creditBonus("inconnu", score = 1_000))
        assertEquals(DAILY_BONUS_CAP_CENTS, repository.remainingDailyBonusCents())
    }

    @Test
    fun `Des données corrompues donnent une liste vide au lieu de planter`() = runTest {
        val store = InMemoryPreferencesDataStore()
        val repo = ProjectRepositoryImpl(store, { now })
        repo.create("A", 1000, 100, 10)
        store.updateData { prefs ->
            val mutable = prefs.toMutablePreferences()
            mutable[androidx.datastore.preferences.core.stringPreferencesKey("projects_json")] = "{pas du json"
            mutable
        }
        assertTrue(repo.projects.first().isEmpty())
    }
}
