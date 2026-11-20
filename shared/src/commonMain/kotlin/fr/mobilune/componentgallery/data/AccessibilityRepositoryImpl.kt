package fr.mobilune.componentgallery.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class AccessibilityRepositoryImpl(
    private val dataStore: DataStore<Preferences>,
) : AccessibilityRepository {
    private val highContrastKey = booleanPreferencesKey("high_contrast")
    private val reduceMotionKey = booleanPreferencesKey("reduce_motion")
    private val textScaleKey = intPreferencesKey("text_scale_percent")
    private val hapticsKey = booleanPreferencesKey("haptics_enabled")

    override val settings: Flow<AccessibilitySettings> = dataStore.data.map { preferences ->
        AccessibilitySettings(
            highContrast = preferences[highContrastKey] ?: false,
            reduceMotion = preferences[reduceMotionKey] ?: false,
            textScalePercent = (preferences[textScaleKey] ?: 100).takeIf { it in TEXT_SCALE_OPTIONS } ?: 100,
            hapticsEnabled = preferences[hapticsKey] ?: true,
        )
    }

    override suspend fun setHighContrast(enabled: Boolean) {
        dataStore.edit { it[highContrastKey] = enabled }
    }

    override suspend fun setReduceMotion(enabled: Boolean) {
        dataStore.edit { it[reduceMotionKey] = enabled }
    }

    override suspend fun setTextScalePercent(percent: Int) {
        dataStore.edit { it[textScaleKey] = percent }
    }

    override suspend fun setHapticsEnabled(enabled: Boolean) {
        dataStore.edit { it[hapticsKey] = enabled }
    }
}
