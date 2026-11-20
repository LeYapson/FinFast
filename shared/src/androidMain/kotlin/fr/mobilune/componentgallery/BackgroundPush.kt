package fr.mobilune.componentgallery

import android.content.Context
import fr.mobilune.componentgallery.core.network.AuthSession
import fr.mobilune.componentgallery.core.network.PushApiImpl
import fr.mobilune.componentgallery.core.network.SimulateTransactionResult
import fr.mobilune.componentgallery.core.network.createHttpClient
import fr.mobilune.componentgallery.data.PREFERENCES_FILE_NAME

/**
 * `POST /push/simulate-transaction` sans l'interface ni Koin : sert au traitement programmé
 * (WorkManager), qui peut s'exécuter alors que l'app est fermée.
 *
 * Le jeton de session est relu dans le même DataStore que l'app (une seule instance par processus),
 * avec le même chemin de fichier que `MainActivity`.
 */
suspend fun simulateTransactionInBackground(context: Context): SimulateTransactionResult {
    val dataStore = getPreferencesDataStore(
        context.applicationContext.filesDir.resolve(PREFERENCES_FILE_NAME).absolutePath,
    )
    val session = AuthSession(createAccessTokenStore(dataStore))
    session.restoreAccessToken()
    val httpClient = createHttpClient()
    try {
        return PushApiImpl(httpClient, session).simulateTransaction()
    } finally {
        httpClient.close()
    }
}
