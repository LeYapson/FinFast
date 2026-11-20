package fr.mobilune.componentgallery.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringSetPreferencesKey
import kotlinx.coroutines.flow.first

class RegisteredAccountRepositoryImpl(
    private val dataStore: DataStore<Preferences>,
) : RegisteredAccountRepository {
    private val registeredUserIdsKey = stringSetPreferencesKey("registered_user_ids")

    override suspend fun isRegistered(userId: String): Boolean =
        userId in (dataStore.data.first()[registeredUserIdsKey] ?: emptySet())

    override suspend fun markRegistered(userId: String) {
        dataStore.edit { preferences ->
            preferences[registeredUserIdsKey] =
                preferences[registeredUserIdsKey].orEmpty() + userId
        }
    }
}
