package fr.mobilune.componentgallery.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.first

class LastVisitedMenuRepositoryImpl(
    private val dataStore: DataStore<Preferences>,
) : LastVisitedMenuRepository {
    private val lastVisitedMenuKey = stringPreferencesKey("last_visited_menu")

    override suspend fun getLastVisitedMenu(): MainMenu =
        when (dataStore.data.first()[lastVisitedMenuKey]) {
            MainMenu.Card.name -> MainMenu.Card
            else -> MainMenu.Home
        }

    override suspend fun saveLastVisitedMenu(menu: MainMenu) {
        dataStore.edit { preferences ->
            preferences[lastVisitedMenuKey] = menu.name
        }
    }
}
