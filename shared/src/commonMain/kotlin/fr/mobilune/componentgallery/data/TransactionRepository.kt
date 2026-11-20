package fr.mobilune.componentgallery.data

import fr.mobilune.componentgallery.components.TransactionUiModel
import kotlinx.coroutines.flow.Flow

interface TransactionRepository {
    val transactions: Flow<List<TransactionUiModel>>

    /** Instant de la dernière synchro réussie, `null` si aucune n'a eu lieu depuis la dernière purge. */
    val lastUpdatedMillis: Flow<Long?>

    suspend fun refresh()

    suspend fun clear()
}
