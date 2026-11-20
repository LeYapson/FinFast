package fr.mobilune.componentgallery.projects

import kotlin.time.Clock
import kotlin.time.ExperimentalTime

/** Un gain de bonus obtenu en jouant à AbdelRun pour un projet. */
data class ProjectGain(
    val timestampMillis: Long,
    val cents: Long,
    val score: Int,
)

/** Un projet d'épargne. Les montants sont en centimes. */
data class Project(
    val id: String,
    val name: String,
    val targetCents: Long,
    val savedCents: Long,
    val monthlyCents: Long,
    val deadlineMonths: Int,
    val createdAtMillis: Long,
    val gains: List<ProjectGain> = emptyList(),
) {
    val remainingCents: Long get() = (targetCents - savedCents).coerceAtLeast(0)
    val isReached: Boolean get() = savedCents >= targetCents

    /** Avancement de 0 à 1. */
    val progress: Float
        get() = if (targetCents <= 0) 0f else (savedCents.toFloat() / targetCents).coerceIn(0f, 1f)
}

/** Résultat d'une partie, une fois converti en bonus. */
data class RunResult(
    val score: Int,
    val bonusCents: Long,
    /** Vrai si le plafond quotidien a réduit (ou supprimé) le bonus. */
    val cappedByDailyLimit: Boolean,
    val projectName: String,
    val savedBeforeCents: Long,
    val savedAfterCents: Long,
    val targetCents: Long,
)

/** 10 points de score = 1 centime de bonus. */
const val POINTS_PER_BONUS_CENT = 10

/** Bonus maximal crédité par jour, tous projets confondus : 1 €. */
const val DAILY_BONUS_CAP_CENTS = 100L

const val MAX_PROJECT_NAME_LENGTH = 30
const val MIN_DEADLINE_MONTHS = 1
const val MAX_DEADLINE_MONTHS = 120
const val DEFAULT_DEADLINE_MONTHS = 6

/** Bonus brut (avant plafond) correspondant à un score. */
fun bonusCentsForScore(score: Int): Long = (score.coerceAtLeast(0) / POINTS_PER_BONUS_CENT).toLong()

/** « 12,50 € » ; le signe moins est typographique, comme sur l'accueil. */
fun formatEuros(cents: Long): String {
    val sign = if (cents < 0) "−" else ""
    val abs = if (cents < 0) -cents else cents
    val euros = abs / 100
    val rest = (abs % 100).toString().padStart(2, '0')
    return "$sign$euros,$rest €"
}

/** « 1 200 € » sans décimales quand le montant est rond, « 12,50 € » sinon. */
fun formatEurosShort(cents: Long): String =
    if (cents % 100 == 0L) "${cents / 100} €" else formatEuros(cents)

/** Convertit « 12,5 » ou « 12.50 » en centimes ; null si invalide (plus de 2 décimales, etc.). */
fun parseAmountCents(amount: String): Long? {
    val normalized = amount.trim().replace(',', '.')
    val parts = normalized.split('.')
    if (parts.size > 2 || parts[0].isEmpty()) return null
    val euros = parts[0].toLongOrNull() ?: return null
    val centText = parts.getOrElse(1) { "" }
    if (centText.length > 2) return null
    val cents = centText.padEnd(2, '0').ifEmpty { "00" }.toLongOrNull() ?: return null
    if (euros < 0 || euros > (Long.MAX_VALUE - cents) / 100) return null
    return euros * 100 + cents
}

/** Mensualité nécessaire pour atteindre [targetCents] en [months] mois (arrondie au centime supérieur). */
fun monthlyCentsFor(targetCents: Long, months: Int): Long {
    val m = months.coerceAtLeast(1)
    return (targetCents + m - 1) / m
}

/** Nombre de mois nécessaires pour atteindre [targetCents] avec [monthlyCents] par mois. */
fun monthsFor(targetCents: Long, monthlyCents: Long): Int {
    if (monthlyCents <= 0) return MAX_DEADLINE_MONTHS
    val months = (targetCents + monthlyCents - 1) / monthlyCents
    return months.coerceIn(MIN_DEADLINE_MONTHS.toLong(), MAX_DEADLINE_MONTHS.toLong()).toInt()
}

private val MONTH_NAMES = listOf(
    "janvier", "février", "mars", "avril", "mai", "juin",
    "juillet", "août", "septembre", "octobre", "novembre", "décembre",
)

/** Jour civil (année, mois 1..12) d'un instant en millisecondes depuis 1970 (UTC). */
internal fun civilMonth(epochMillis: Long): Pair<Int, Int> {
    // Algorithme « civil_from_days » de Howard Hinnant.
    val z = epochMillis.floorDiv(86_400_000L) + 719_468
    val era = z.floorDiv(146_097L)
    val doe = z - era * 146_097
    val yoe = (doe - doe / 1_460 + doe / 36_524 - doe / 146_096) / 365
    val doy = doe - (365 * yoe + yoe / 4 - yoe / 100)
    val mp = (5 * doy + 2) / 153
    val month = (if (mp < 10) mp + 3 else mp - 9).toInt()
    var year = (yoe + era * 400).toInt()
    if (month <= 2) year++
    return year to month
}

/** « mars 2027 » : le mois atteint [months] mois après [fromMillis]. */
fun formatTargetMonth(fromMillis: Long, months: Int): String {
    val (year, month) = civilMonth(fromMillis)
    val index = (year * 12 + (month - 1)) + months
    return "${MONTH_NAMES[index % 12]} ${index / 12}"
}

@OptIn(ExperimentalTime::class)
internal fun currentTimeMillis(): Long = Clock.System.now().toEpochMilliseconds()
