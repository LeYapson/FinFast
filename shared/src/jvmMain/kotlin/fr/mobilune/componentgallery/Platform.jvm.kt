package fr.mobilune.componentgallery

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import fr.mobilune.componentgallery.core.network.AccessTokenStore
import fr.mobilune.componentgallery.data.db.AppDatabase
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.room.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import kotlinx.coroutines.Dispatchers
import okio.Path.Companion.toPath
import java.io.File

class JVMPlatform : Platform {
    override val name: String = "Java ${System.getProperty("java.version")}"
}

actual fun getPlatform(): Platform = JVMPlatform()

actual fun pushPlatformName(): String = "desktop"

actual fun getPreferencesDataStore(dataStoreFilePath: String): DataStore<Preferences> =
    PreferenceDataStoreFactory.createWithPath(
        produceFile = { dataStoreFilePath.toPath() },
    )

actual fun createAccessTokenStore(dataStore: DataStore<Preferences>): AccessTokenStore =
    InMemoryAccessTokenStore()

actual fun createAppDatabase(databaseFilePath: String): AppDatabase {
    File(databaseFilePath).absoluteFile.parentFile?.mkdirs()
    return Room.databaseBuilder<AppDatabase>(name = databaseFilePath)
        .setDriver(BundledSQLiteDriver())
        .setQueryCoroutineContext(Dispatchers.IO)
        .build()
}

@Composable
actual fun SystemBackHandler(enabled: Boolean, onBack: () -> Unit) = Unit

private class InMemoryAccessTokenStore : AccessTokenStore {
    private var accessToken: String? = null

    override suspend fun read(): String? = accessToken

    override suspend fun write(token: String) {
        accessToken = token
    }

    override suspend fun clear() {
        accessToken = null
    }
}

@Composable
actual fun NfcCardScanner(
    onTagRead: (String) -> Unit,
    onScanStatus: (String) -> Unit,
    onScanError: (String) -> Unit,
) {
    Text("Le scan NFC est disponible uniquement sur Android.")
    LaunchedEffect(Unit) {
        onScanError("Le scan NFC est disponible uniquement sur Android.")
    }
}
