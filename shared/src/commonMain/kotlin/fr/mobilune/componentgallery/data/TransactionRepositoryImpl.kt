package fr.mobilune.componentgallery.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import fr.mobilune.componentgallery.components.TransactionUiModel
import fr.mobilune.componentgallery.data.db.TransactionDao
import fr.mobilune.componentgallery.data.db.TransactionEntity
import io.ktor.client.HttpClient
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

private const val TRANSACTIONS_URL = "https://simpleapi.mobilune.fr/transactions"

/**
 * Source de vérité : la base Room. L'UI lit [transactions] ; [refresh] va chercher l'API,
 * remplace le contenu de la base puis mémorise l'heure de la mise à jour.
 */
@OptIn(ExperimentalTime::class)
class TransactionRepositoryImpl(
    private val httpClient: HttpClient,
    private val authSession: AuthSession,
    private val transactionDao: TransactionDao,
    private val dataStore: DataStore<Preferences>,
    private val clock: Clock,
) : TransactionRepository {
    private val lastUpdatedKey = longPreferencesKey("transactions_last_updated")

    override val transactions: Flow<List<TransactionUiModel>> =
        transactionDao.observeAll().map { entities -> entities.map { it.toUiModel() } }

    override val lastUpdatedMillis: Flow<Long?> = dataStore.data.map { it[lastUpdatedKey] }

    override suspend fun refresh() {
        val entities = fetchTransactions()
        transactionDao.replaceAll(entities)
        dataStore.edit { preferences ->
            preferences[lastUpdatedKey] = clock.now().toEpochMilliseconds()
        }
    }

    override suspend fun clear() {
        transactionDao.deleteAll()
        dataStore.edit { preferences -> preferences.remove(lastUpdatedKey) }
    }

    private suspend fun fetchTransactions(): List<TransactionEntity> {
        val response = httpClient.get(TRANSACTIONS_URL) {
            authSession.accessToken?.let(::bearerAuth)
        }
        if (response.status.value.isSessionRejection()) {
            authSession.expire()
            throw SessionExpiredException()
        }
        if (response.status.value !in 200..299) {
            throw IllegalStateException("Erreur HTTP ${response.status.value} lors du chargement des transactions.")
        }

        return Json.parseToJsonElement(response.bodyAsText())
            .jsonArray
            .mapIndexed { index, element ->
                val transaction = element.jsonObject
                val merchant = transaction.getValue("marchand").jsonPrimitive.content
                val date = transaction.getValue("date").jsonPrimitive.content
                val amountInCents = transaction.getValue("montant_centimes").jsonPrimitive.content.toLong()
                val id = transaction["id"]?.jsonPrimitive?.content
                    ?.takeUnless { it == "null" }
                    ?: "$merchant-$date-$index"

                TransactionEntity(
                    id = id,
                    merchant = merchant,
                    isoDate = date,
                    amountCents = amountInCents,
                    position = index,
                )
            }
    }
}

private fun TransactionEntity.toUiModel() = TransactionUiModel(
    id = id,
    store = merchant,
    date = formatTransactionDate(isoDate),
    price = formatTransactionAmount(amountCents),
    isIncome = amountCents > 0,
    amountCents = amountCents,
)

private fun formatTransactionDate(isoDate: String): String {
    val parts = isoDate.split('-')
    if (parts.size != 3) return isoDate

    val month = parts[1].toIntOrNull()?.let { monthNumber ->
        listOf(
            "janv.", "févr.", "mars", "avr.", "mai", "juin",
            "juil.", "août", "sept.", "oct.", "nov.", "déc.",
        ).getOrNull(monthNumber - 1)
    } ?: return isoDate

    return "${parts[2]} $month ${parts[0]}"
}

private fun formatTransactionAmount(amountInCents: Long): String {
    val absoluteAmount = if (amountInCents < 0) -amountInCents else amountInCents
    val euros = absoluteAmount / 100
    val cents = (absoluteAmount % 100).toString().padStart(2, '0')
    val sign = if (amountInCents < 0) "−" else "+"
    return "$sign$euros,$cents €"
}
