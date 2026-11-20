package fr.mobilune.componentgallery

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color

/**
 * Couleurs de l'app. Les valeurs dépendent de [highContrast], un état Compose : toute lecture
 * pendant la composition ou le dessin est donc observée et l'UI se met à jour au changement d'option.
 *
 * Mode normal : tous les textes respectent WCAG AA (4,5:1). Mode contraste élevé : noir sur blanc,
 * accent assombri, bordures noires et icônes foncées, pour atteindre AAA (7:1) sur les textes
 * et au moins 3:1 sur les éléments d'interface.
 */
object ThemeConstants {
    var highContrast: Boolean by mutableStateOf(false)

    val classicBackground: Color get() = if (highContrast) Color(0xFFFFFFFF) else Color(0xFFFEFEFE)
    val onClassicBackground: Color get() = if (highContrast) Color(0xFF000000) else Color(0xFF060606)
    val accentBackground: Color get() = if (highContrast) Color(0xFF0A3D2D) else Color(0xFF135E45)
    val onAccentBackground: Color get() = if (highContrast) Color(0xFFFFFFFF) else Color(0xFFEDFCF8)
    val incomeBackground: Color get() = Color(0xFFD9F4D6)
    val incomeBackgroundStrong: Color get() = if (highContrast) Color(0xFF0B5A12) else Color(0xFF53D447)
    val outcomeBackground: Color get() = Color(0xFFF2EBDC)
    val outcomeBackgroundStrong: Color get() = if (highContrast) Color(0xFF5C4A12) else Color(0xFFD5C5A1)
    val statusIndicator: Color get() = if (highContrast) Color(0xFF0B5A12) else Color(0xFF7FE574)

    /** Bordure décorative (conteneurs). */
    val border: Color get() = if (highContrast) Color(0xFF000000) else Color(0xFFCBCBCB)

    /** Bordure d'un élément qui porte une information (points du code secret, étapes) : au moins 3:1. */
    val componentBorder: Color get() = if (highContrast) Color(0xFF000000) else Color(0xFF767676)

    val error: Color get() = if (highContrast) Color(0xFF8A1C14) else Color(0xFFB3261E)
    val secondaryText: Color get() = if (highContrast) Color(0xFF333333) else Color(0xFF6B6B6B)

    /** Épaisseur des contours des conteneurs : plus marquée en contraste élevé. */
    val borderWidthDp: Float get() = if (highContrast) 2f else 1f
}

/** Palette Material utilisée en contraste élevé (champs de saisie, focus, interrupteurs). */
internal fun highContrastColorScheme(): ColorScheme = lightColorScheme(
    primary = Color(0xFF0A3D2D),
    onPrimary = Color(0xFFFFFFFF),
    background = Color(0xFFFFFFFF),
    onBackground = Color(0xFF000000),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF000000),
    onSurfaceVariant = Color(0xFF000000),
    outline = Color(0xFF000000),
    outlineVariant = Color(0xFF000000),
    error = Color(0xFF8A1C14),
)
