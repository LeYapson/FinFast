package fr.mobilune.componentgallery.data

import kotlinx.coroutines.flow.Flow

/** Tailles de texte proposées, en pourcentage de la taille système. */
val TEXT_SCALE_OPTIONS = listOf(100, 125, 150)

data class AccessibilitySettings(
    val highContrast: Boolean = false,
    val reduceMotion: Boolean = false,
    val textScalePercent: Int = 100,
    val hapticsEnabled: Boolean = true,
)

interface AccessibilityRepository {
    val settings: Flow<AccessibilitySettings>

    suspend fun setHighContrast(enabled: Boolean)

    suspend fun setReduceMotion(enabled: Boolean)

    suspend fun setTextScalePercent(percent: Int)

    suspend fun setHapticsEnabled(enabled: Boolean)
}
