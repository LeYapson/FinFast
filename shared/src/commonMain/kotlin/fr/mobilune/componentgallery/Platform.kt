package fr.mobilune.componentgallery

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.compose.runtime.Composable
import fr.mobilune.componentgallery.core.network.AccessTokenStore
import fr.mobilune.componentgallery.data.db.AppDatabase

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform

/** Valeur du champ `platform` envoyé à `POST /push/devices` : « android » ou « desktop ». */
expect fun pushPlatformName(): String

expect fun getPreferencesDataStore(dataStoreFilePath: String): DataStore<Preferences>

expect fun createAccessTokenStore(dataStore: DataStore<Preferences>): AccessTokenStore

expect fun createAppDatabase(databaseFilePath: String): AppDatabase

/** Bouton / geste « retour » du système. Sans effet sur desktop. */
@Composable
expect fun SystemBackHandler(enabled: Boolean, onBack: () -> Unit)

@Composable
expect fun NfcCardScanner(
    onTagRead: (String) -> Unit,
    onScanStatus: (String) -> Unit,
    onScanError: (String) -> Unit,
)