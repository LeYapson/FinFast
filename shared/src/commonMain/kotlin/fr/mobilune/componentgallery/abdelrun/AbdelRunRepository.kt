package fr.mobilune.componentgallery.abdelrun

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.first

/** Sauvegarde du jeu (pièces, skins, statistiques, volume), au même format que le save.txt d'origine. */
interface AbdelRunRepository {
    suspend fun load(): AbdelRunSave
    suspend fun write(save: AbdelRunSave)
}

class AbdelRunRepositoryImpl(
    private val dataStore: DataStore<Preferences>,
) : AbdelRunRepository {
    private val saveKey = stringPreferencesKey("abdelrun_save")

    override suspend fun load(): AbdelRunSave = AbdelRunSave.decode(dataStore.data.first()[saveKey])

    override suspend fun write(save: AbdelRunSave) {
        val text = save.encode()
        dataStore.edit { it[saveKey] = text }
    }
}
