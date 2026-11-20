package fr.mobilune.componentgallery.projects

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.long
import kotlinx.serialization.json.put
import kotlin.random.Random

interface ProjectRepository {
    val projects: Flow<List<Project>>

    suspend fun create(name: String, targetCents: Long, monthlyCents: Long, deadlineMonths: Int): Project

    suspend fun delete(projectId: String)

    /** Bonus encore créditable aujourd'hui, en centimes. */
    suspend fun remainingDailyBonusCents(): Long

    /**
     * Convertit le score d'une partie en bonus pour le projet, dans la limite du plafond quotidien.
     * Renvoie null si le projet n'existe plus.
     */
    suspend fun creditBonus(projectId: String, score: Int): RunResult?
}

class ProjectRepositoryImpl(
    private val dataStore: DataStore<Preferences>,
    private val nowMillis: () -> Long,
    private val newId: () -> String = { Random.nextLong().toULong().toString(16) },
) : ProjectRepository {
    private val projectsKey = stringPreferencesKey("projects_json")
    private val bonusDayKey = longPreferencesKey("project_bonus_day")
    private val bonusCentsKey = longPreferencesKey("project_bonus_cents")

    override val projects: Flow<List<Project>> = dataStore.data.map { decode(it[projectsKey]) }

    override suspend fun create(
        name: String,
        targetCents: Long,
        monthlyCents: Long,
        deadlineMonths: Int,
    ): Project {
        val project = Project(
            id = newId(),
            name = name.trim(),
            targetCents = targetCents,
            savedCents = 0,
            monthlyCents = monthlyCents,
            deadlineMonths = deadlineMonths,
            createdAtMillis = nowMillis(),
        )
        dataStore.edit { prefs -> prefs[projectsKey] = encode(decode(prefs[projectsKey]) + project) }
        return project
    }

    override suspend fun delete(projectId: String) {
        dataStore.edit { prefs ->
            prefs[projectsKey] = encode(decode(prefs[projectsKey]).filterNot { it.id == projectId })
        }
    }

    override suspend fun remainingDailyBonusCents(): Long {
        val prefs = dataStore.data.first()
        return (DAILY_BONUS_CAP_CENTS - spentToday(prefs)).coerceAtLeast(0)
    }

    override suspend fun creditBonus(projectId: String, score: Int): RunResult? {
        var result: RunResult? = null
        dataStore.edit { prefs ->
            val list = decode(prefs[projectsKey])
            val project = list.firstOrNull { it.id == projectId } ?: return@edit
            val now = nowMillis()
            val raw = bonusCentsForScore(score)
            val spent = spentToday(prefs)
            val allowed = (DAILY_BONUS_CAP_CENTS - spent).coerceAtLeast(0)
            val credited = minOf(raw, allowed)
            val updated = project.copy(
                savedCents = project.savedCents + credited,
                gains = if (credited > 0) {
                    (listOf(ProjectGain(now, credited, score)) + project.gains).take(MAX_GAINS)
                } else {
                    project.gains
                },
            )
            prefs[projectsKey] = encode(list.map { if (it.id == projectId) updated else it })
            prefs[bonusDayKey] = dayIndex(now)
            prefs[bonusCentsKey] = spent + credited
            result = RunResult(
                score = score,
                bonusCents = credited,
                cappedByDailyLimit = credited < raw,
                projectName = project.name,
                savedBeforeCents = project.savedCents,
                savedAfterCents = updated.savedCents,
                targetCents = project.targetCents,
            )
        }
        return result
    }

    private fun dayIndex(millis: Long): Long = millis.floorDiv(86_400_000L)

    /** Bonus déjà crédité aujourd'hui (le compteur repart à zéro chaque jour). */
    private fun spentToday(prefs: Preferences): Long =
        if (prefs[bonusDayKey] == dayIndex(nowMillis())) prefs[bonusCentsKey] ?: 0 else 0

    private fun encode(projects: List<Project>): String = buildJsonArray {
        for (p in projects) {
            add(
                buildJsonObject {
                    put("id", p.id)
                    put("name", p.name)
                    put("target", p.targetCents)
                    put("saved", p.savedCents)
                    put("monthly", p.monthlyCents)
                    put("months", p.deadlineMonths)
                    put("created", p.createdAtMillis)
                    put(
                        "gains",
                        buildJsonArray {
                            for (g in p.gains) {
                                add(
                                    buildJsonObject {
                                        put("t", g.timestampMillis)
                                        put("c", g.cents)
                                        put("s", g.score)
                                    },
                                )
                            }
                        },
                    )
                },
            )
        }
    }.toString()

    private fun decode(text: String?): List<Project> {
        if (text.isNullOrBlank()) return emptyList()
        return try {
            Json.parseToJsonElement(text).jsonArray.map { it.jsonObject.toProject() }
        } catch (_: Exception) {
            emptyList() // Données corrompues : on repart d'une liste vide plutôt que de planter.
        }
    }

    private fun JsonObject.toProject() = Project(
        id = getValue("id").jsonPrimitive.content,
        name = getValue("name").jsonPrimitive.content,
        targetCents = getValue("target").jsonPrimitive.long,
        savedCents = getValue("saved").jsonPrimitive.long,
        monthlyCents = getValue("monthly").jsonPrimitive.long,
        deadlineMonths = getValue("months").jsonPrimitive.long.toInt(),
        createdAtMillis = getValue("created").jsonPrimitive.long,
        gains = (this["gains"] as? JsonArray).orEmpty().map {
            val g = it.jsonObject
            ProjectGain(
                timestampMillis = g.getValue("t").jsonPrimitive.long,
                cents = g.getValue("c").jsonPrimitive.long,
                score = g.getValue("s").jsonPrimitive.long.toInt(),
            )
        },
    )

    private companion object {
        const val MAX_GAINS = 50
    }
}
