package fr.mobilune.componentgallery.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import fr.mobilune.componentgallery.getPreferencesDataStore
import kotlinx.coroutines.flow.first

private val virtualCardIdKey = stringPreferencesKey("virtual_card_id")
private val virtualCardEnabledKey = booleanPreferencesKey("virtual_card_enabled")

class VirtualCardRepositoryImpl(
    private val dataStore: DataStore<Preferences>,
) : VirtualCardRepository {
    override suspend fun getCardId(): String? = dataStore.data.first()[virtualCardIdKey]

    override suspend fun isCardEnabled(): Boolean = dataStore.data.first()[virtualCardEnabledKey] ?: false

    override suspend fun saveCardId(cardId: String) {
        dataStore.edit { preferences ->
            preferences[virtualCardIdKey] = cardId
        }
    }

    override suspend fun setCardEnabled(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[virtualCardEnabledKey] = enabled
        }
    }
}

suspend fun readStoredVirtualCardId(dataStoreFilePath: String): String? =
    VirtualCardRepositoryImpl(getPreferencesDataStore(dataStoreFilePath)).getCardId()

suspend fun isStoredVirtualCardEnabled(dataStoreFilePath: String): Boolean =
    VirtualCardRepositoryImpl(getPreferencesDataStore(dataStoreFilePath)).isCardEnabled()
