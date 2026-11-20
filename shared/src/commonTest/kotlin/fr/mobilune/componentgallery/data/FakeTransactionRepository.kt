package fr.mobilune.componentgallery.data

import fr.mobilune.componentgallery.components.TransactionUiModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

/**
 * Double de test de [TransactionRepository] : le « cache » (la base Room) est un [MutableStateFlow],
 * et [refresh] simule l'API en y publiant [remoteTransactions].
 */
class FakeTransactionRepository(
    cachedTransactions: List<TransactionUiModel> = emptyList(),
    cachedLastUpdatedMillis: Long? = null,
) : TransactionRepository {
    private val cache = MutableStateFlow(cachedTransactions)
    private val lastUpdated = MutableStateFlow(cachedLastUpdatedMillis)

    override val transactions: Flow<List<TransactionUiModel>> = cache
    override val lastUpdatedMillis: Flow<Long?> = lastUpdated

    /** Ce que « le serveur » renvoie au prochain [refresh]. */
    var remoteTransactions: List<TransactionUiModel> = emptyList()

    /** Instant enregistré comme « dernière mise à jour » quand [refresh] réussit. */
    var refreshTimeMillis: Long = 1_000

    /** Durée simulée de l'appel réseau, en temps virtuel dans `runTest`. */
    var refreshDelayMillis: Long = 0

    /** Si renseignée, [refresh] la lance sans toucher au cache (comme un échec réseau). */
    var refreshError: Throwable? = null

    var refreshCount = 0
        private set
    var clearCount = 0
        private set

    override suspend fun refresh() {
        refreshCount++
        delay(refreshDelayMillis)
        refreshError?.let { throw it }
        cache.value = remoteTransactions
        lastUpdated.value = refreshTimeMillis
    }

    override suspend fun clear() {
        clearCount++
        cache.value = emptyList()
        lastUpdated.value = null
    }
}

fun fakeTransaction(id: String, amountCents: Long) = TransactionUiModel(
    id = id,
    store = "Store $id",
    date = "01 janv. 2026",
    price = "$amountCents",
    isIncome = amountCents > 0,
    amountCents = amountCents,
)
