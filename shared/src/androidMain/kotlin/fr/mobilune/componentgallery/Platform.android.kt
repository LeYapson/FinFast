package fr.mobilune.componentgallery

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import fr.mobilune.componentgallery.data.INFAST_AID_HEX
import fr.mobilune.componentgallery.data.VIRTUAL_CARD_AID_HEX
import fr.mobilune.componentgallery.data.VIRTUAL_CARD_SELECT_MARKER
import fr.mobilune.componentgallery.data.AccessTokenStore
import fr.mobilune.componentgallery.data.db.AppDatabase
import androidx.room.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import kotlinx.coroutines.Dispatchers
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.os.Build
import android.nfc.NfcAdapter
import android.nfc.tech.IsoDep
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import android.util.Log
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.flow.first
import java.nio.ByteBuffer
import java.security.KeyStore
import java.util.concurrent.atomic.AtomicBoolean
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import okio.Path.Companion.toPath

class AndroidPlatform : Platform {
    override val name: String = "Android ${Build.VERSION.SDK_INT}"
}

actual fun getPlatform(): Platform = AndroidPlatform()

actual fun pushPlatformName(): String = "android"

private object AndroidPreferencesDataStore {
    private var filePath: String? = null
    private var instance: DataStore<Preferences>? = null

    @Synchronized
    fun get(filePath: String): DataStore<Preferences> {
        require(this.filePath == null || this.filePath == filePath) {
            "DataStore was already initialized with a different path."
        }
        return instance ?: PreferenceDataStoreFactory.createWithPath(
            produceFile = { filePath.toPath() },
        ).also {
            this.filePath = filePath
            instance = it
        }
    }
}

actual fun getPreferencesDataStore(dataStoreFilePath: String): DataStore<Preferences> =
    AndroidPreferencesDataStore.get(dataStoreFilePath)

actual fun createAccessTokenStore(dataStore: DataStore<Preferences>): AccessTokenStore =
    AndroidAccessTokenStore(dataStore)

internal lateinit var appContext: Context

/** À appeler une fois depuis `MainActivity` avant de créer l'App : Room a besoin d'un Context sur Android. */
fun initAndroidAppContext(context: Context) {
    appContext = context.applicationContext
}

actual fun createAppDatabase(databaseFilePath: String): AppDatabase =
    Room.databaseBuilder<AppDatabase>(context = appContext, name = databaseFilePath)
        .setDriver(BundledSQLiteDriver())
        .setQueryCoroutineContext(Dispatchers.IO)
        .build()

@Composable
actual fun SystemBackHandler(enabled: Boolean, onBack: () -> Unit) {
    androidx.activity.compose.BackHandler(enabled = enabled, onBack = onBack)
}

actual fun formatLastUpdate(epochMillis: Long): String =
    SimpleDateFormat("d MMM yyyy 'à' HH:mm", Locale.FRANCE).format(Date(epochMillis))

private class AndroidAccessTokenStore(
    private val dataStore: DataStore<Preferences>,
) : AccessTokenStore {
    override suspend fun read(): String? {
        val encryptedToken = dataStore.data.first()[encryptedAccessTokenKey]
        if (encryptedToken == null) {
            Log.d(AUTH_TOKEN_STORE_TAG, "No persisted access token found.")
            return null
        }
        return try {
            decryptAccessToken(encryptedToken).also {
                Log.d(AUTH_TOKEN_STORE_TAG, "Persisted access token restored.")
            }
        } catch (exception: Exception) {
            // Clé Keystore perdue ou invalidée : le jeton est inutilisable, on repart d'une session vide.
            Log.e(AUTH_TOKEN_STORE_TAG, "Unable to decrypt persisted access token, clearing it.", exception)
            clear()
            null
        }
    }

    override suspend fun write(token: String) {
        val encryptedToken = encryptAccessToken(token)
        dataStore.edit { preferences ->
            preferences[encryptedAccessTokenKey] = encryptedToken
        }
        Log.d(AUTH_TOKEN_STORE_TAG, "Access token persisted.")
    }

    override suspend fun clear() {
        dataStore.edit { preferences ->
            preferences.remove(encryptedAccessTokenKey)
        }
        Log.d(AUTH_TOKEN_STORE_TAG, "Persisted access token cleared.")
    }
}

private val encryptedAccessTokenKey = stringPreferencesKey("encrypted_access_token")
private const val AUTH_TOKEN_STORE_TAG = "AuthTokenStore"
private const val ANDROID_KEY_STORE = "AndroidKeyStore"
private const val ACCESS_TOKEN_KEY_ALIAS = "componentgallery_access_token"
private const val GCM_TAG_LENGTH_BITS = 128
private const val GCM_IV_LENGTH_BYTES = 12

private fun encryptAccessToken(token: String): String {
    val cipher = Cipher.getInstance("AES/GCM/NoPadding")
    cipher.init(Cipher.ENCRYPT_MODE, getOrCreateAccessTokenKey())
    val ciphertext = cipher.doFinal(token.encodeToByteArray())
    val payload = ByteBuffer.allocate(cipher.iv.size + ciphertext.size)
        .put(cipher.iv)
        .put(ciphertext)
        .array()
    return Base64.encodeToString(payload, Base64.NO_WRAP)
}

private fun decryptAccessToken(encryptedToken: String): String {
    val payload = Base64.decode(encryptedToken, Base64.NO_WRAP)
    check(payload.size > GCM_IV_LENGTH_BYTES) { "Le jeton chiffré est invalide." }
    val buffer = ByteBuffer.wrap(payload)
    val iv = ByteArray(GCM_IV_LENGTH_BYTES).also(buffer::get)
    val ciphertext = ByteArray(buffer.remaining()).also(buffer::get)
    val cipher = Cipher.getInstance("AES/GCM/NoPadding")
    cipher.init(
        Cipher.DECRYPT_MODE,
        getOrCreateAccessTokenKey(),
        GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv),
    )
    return cipher.doFinal(ciphertext).decodeToString()
}

@Synchronized
private fun getOrCreateAccessTokenKey(): SecretKey {
    val keyStore = KeyStore.getInstance(ANDROID_KEY_STORE).apply { load(null) }
    (keyStore.getKey(ACCESS_TOKEN_KEY_ALIAS, null) as? SecretKey)?.let { return it }

    val keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEY_STORE)
    keyGenerator.init(
        KeyGenParameterSpec.Builder(
            ACCESS_TOKEN_KEY_ALIAS,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setKeySize(256)
            .build(),
    )
    return keyGenerator.generateKey()
}

@Composable
actual fun NfcCardScanner(
    onTagRead: (String) -> Unit,
    onScanStatus: (String) -> Unit,
    onScanError: (String) -> Unit,
) {
    val context = LocalContext.current
    val activity = context.findActivity()
    val currentOnTagRead by rememberUpdatedState(onTagRead)
    val currentOnScanStatus by rememberUpdatedState(onScanStatus)
    val currentOnScanError by rememberUpdatedState(onScanError)

    Text("Approche ta carte NFC du téléphone…")

    DisposableEffect(activity) {
        val adapter = NfcAdapter.getDefaultAdapter(context)
        when {
            activity == null -> {
                currentOnScanError("Le scan NFC ne peut pas être démarré dans ce contexte.")
            }

            adapter == null -> {
                currentOnScanError("Cet appareil ne possède pas de lecteur NFC.")
            }

            !adapter.isEnabled -> {
                currentOnScanError("Active le NFC dans les paramètres du téléphone puis réessaie.")
            }

            else -> {
                val delivered = AtomicBoolean(false)
                val mainHandler = Handler(Looper.getMainLooper())
                val callback = NfcAdapter.ReaderCallback { tag ->
                    if (delivered.compareAndSet(false, true)) {
                        mainHandler.post { currentOnScanStatus("Carte détectée, lecture NFC en cours…") }
                        try {
                            val virtualCard = readVirtualCardId(tag)
                            if (virtualCard == null && tag.id.isRandomUid()) {
                                throw IllegalStateException(
                                    "Cet identifiant change à chaque lecture (carte bancaire ou téléphone) " +
                                            "et ne peut pas identifier un compte. Active la carte virtuelle ou utilise une autre carte.",
                                )
                            }
                            val cardId = virtualCard ?: tag.id.joinToString("") { byte ->
                                "%02X".format(byte.toInt() and 0xFF)
                            }
                            mainHandler.post {
                                currentOnScanStatus(
                                    if (virtualCard != null) {
                                        "Carte virtuelle reconnue, identifiant reçu."
                                    } else {
                                        "Carte NFC physique détectée."
                                    },
                                )
                            }
                            mainHandler.post { currentOnTagRead(cardId) }
                        } catch (exception: Exception) {
                            mainHandler.post {
                                currentOnScanError(exception.message ?: "Impossible de lire la carte NFC.")
                            }
                        }
                    }
                }
                try {
                    adapter.enableReaderMode(
                        activity,
                        callback,
                        NfcAdapter.FLAG_READER_NFC_A or
                                NfcAdapter.FLAG_READER_NFC_B or
                                NfcAdapter.FLAG_READER_NFC_F or
                                NfcAdapter.FLAG_READER_NFC_V or
                                NfcAdapter.FLAG_READER_SKIP_NDEF_CHECK,
                        Bundle(),
                    )
                } catch (exception: Exception) {
                    currentOnScanError(exception.message ?: "Impossible de démarrer le lecteur NFC.")
                }
            }
        }

        onDispose {
            if (activity != null && adapter?.isEnabled == true) {
                adapter.disableReaderMode(activity)
            }
        }
    }
}

private fun readVirtualCardId(tag: android.nfc.Tag): String? {
    val isoDep = IsoDep.get(tag) ?: return null
    var selectedVirtualCard = false
    return try {
        isoDep.timeout = 2_000
        isoDep.connect()
        val selected = isoDep.transceive(selectAidCommand(VIRTUAL_CARD_AID_HEX))
        if (!selected.hasSuccessStatusWord() || !selected.hasVirtualCardSelectMarker()) {
            // Carte virtuelle de l'app « Ma Carte » : l'identifiant est renvoyé dès le SELECT.
            val infast = isoDep.transceive(selectAidCommand(INFAST_AID_HEX))
            if (!infast.hasSuccessStatusWord() || infast.size <= 2) return null
            // Certaines cartes physiques répondent `90 00` à n'importe quel SELECT : on n'accepte
            // que ce que renvoie une vraie carte virtuelle, un UID hexadécimal (4 à 10 octets).
            return infast.copyOfRange(0, infast.size - 2)
                .toString(Charsets.UTF_8)
                .trim()
                .takeIf { it.matches(INFAST_CARD_ID_REGEX) }
                ?.uppercase()
        }
        selectedVirtualCard = true

        val response = isoDep.transceive(byteArrayOf(0x80.toByte(), 0xCA.toByte(), 0x00, 0x00, 0x00))
        if (!response.hasSuccessStatusWord()) {
            val statusWord = response.takeLast(2).joinToString("") {
                "%02X".format(it.toInt() and 0xFF)
            }
            throw IllegalStateException(
                if (statusWord == "6A82") {
                    "Téléphone détecté, mais sa carte virtuelle est désactivée. " +
                            "Appuie sur la carte dans « Ma carte » pour l'activer."
                } else {
                    "Téléphone détecté, mais la lecture NFC a échoué (code $statusWord)."
                },
            )
        }
        check(response.size > 2) {
            "Téléphone détecté, mais aucun identifiant de carte n'a été transmis."
        }
        response.copyOfRange(0, response.size - 2)
            .toString(Charsets.UTF_8)
            .also { check(it.isNotBlank()) { "L'identifiant de la carte virtuelle est vide." } }
    } catch (exception: Exception) {
        if (selectedVirtualCard) throw exception
        null
    } finally {
        runCatching { isoDep.close() }
    }
}

private val INFAST_CARD_ID_REGEX = Regex("[0-9A-Fa-f]{8,20}")

private fun selectAidCommand(aidHex: String): ByteArray {
    val aid = aidHex.chunked(2).map { it.toInt(16).toByte() }
    return byteArrayOf(0x00, 0xA4.toByte(), 0x04, 0x00, aid.size.toByte()) + aid + 0x00.toByte()
}

/** UID de 4 octets commençant par `08` : change à chaque lecture (ISO 14443-3), inutilisable comme identifiant. */
private fun ByteArray.isRandomUid(): Boolean = size == 4 && this[0] == 0x08.toByte()

private fun ByteArray.hasSuccessStatusWord(): Boolean =
    size >= 2 && this[size - 2] == 0x90.toByte() && this[size - 1] == 0x00.toByte()

private fun ByteArray.hasStatusWord(sw1: Int, sw2: Int): Boolean =
    size >= 2 && this[size - 2] == sw1.toByte() && this[size - 1] == sw2.toByte()

private fun ByteArray.hasVirtualCardSelectMarker(): Boolean {
    val marker = VIRTUAL_CARD_SELECT_MARKER.encodeToByteArray()
    return size == marker.size + 2 &&
            copyOfRange(0, marker.size).contentEquals(marker) &&
            hasSuccessStatusWord()
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}