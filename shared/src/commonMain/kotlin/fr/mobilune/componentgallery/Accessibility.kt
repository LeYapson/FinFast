package fr.mobilune.componentgallery

import androidx.compose.animation.ContentTransform
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.paneTitle
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Réglages d'accessibilité lus par les composants ; mis à jour depuis les options. */
object AccessibilityState {
    /** Supprime les animations décoratives et les transitions d'écran. */
    var reduceMotion: Boolean by mutableStateOf(false)

    /** Retours haptiques de l'app (scan, validation, erreurs). */
    var hapticsEnabled: Boolean by mutableStateOf(true)
}

/** Titre de section : permet à TalkBack de naviguer par titres. */
internal fun Modifier.heading(): Modifier = semantics { heading() }

/** Contenu qui change : TalkBack l'annonce sans que l'utilisateur ait à y revenir. */
internal fun Modifier.politeLiveRegion(): Modifier = semantics { liveRegion = LiveRegionMode.Polite }

/**
 * Nom de l'écran, annoncé par TalkBack quand il apparaît (sans cela, un changement d'écran
 * ne produit aucune annonce et l'utilisateur ne sait pas où il est).
 */
internal fun Modifier.screenTitle(title: String): Modifier = semantics { paneTitle = title }

/** Comme [LocalHapticFeedback], mais muet quand l'utilisateur a désactivé les retours haptiques. */
@Composable
internal fun rememberAppHaptics(): HapticFeedback {
    val delegate = LocalHapticFeedback.current
    return remember(delegate) {
        object : HapticFeedback {
            override fun performHapticFeedback(hapticFeedbackType: HapticFeedbackType) {
                if (AccessibilityState.hapticsEnabled) delegate.performHapticFeedback(hapticFeedbackType)
            }
        }
    }
}

/** Fondu entre deux contenus ; instantané si « réduire les animations » est actif. */
internal fun fadeTransition(enterMillis: Int = 250, exitMillis: Int = 150, delayMillis: Int = 0): ContentTransform =
    if (AccessibilityState.reduceMotion) {
        EnterTransition.None togetherWith ExitTransition.None
    } else {
        fadeIn(tween(enterMillis, delayMillis)) togetherWith fadeOut(tween(exitMillis))
    }

/** Apparition en fondu + glissement ; aucune animation si « réduire les animations » est actif. */
internal fun slideFadeIn(delayMillis: Int = 0, offsetDivisor: Int = 6): EnterTransition =
    if (AccessibilityState.reduceMotion) {
        EnterTransition.None
    } else {
        fadeIn(tween(350, delayMillis)) + slideInVertically(tween(350, delayMillis)) { it / offsetDivisor }
    }

/**
 * Bouton-icône avec une zone tactile de 48 dp (minimum recommandé), un rôle « bouton » et un
 * libellé parlant. L'icône elle-même est décorative : seul [description] est lu.
 */
@Composable
internal fun IconAction(
    description: String,
    onClick: () -> Unit,
    icon: @Composable (Modifier) -> Unit,
) {
    Box(
        modifier = Modifier
            .size(48.dp)
            .clickable(role = Role.Button, onClick = onClick)
            .semantics { contentDescription = description },
        contentAlignment = Alignment.Center,
    ) {
        icon(Modifier.size(32.dp))
    }
}

/** Message d'erreur annoncé immédiatement par TalkBack. */
@Composable
internal fun ErrorText(message: String, modifier: Modifier = Modifier) {
    Text(
        text = message,
        color = ThemeConstants.error,
        fontSize = 14.sp,
        modifier = modifier.semantics { liveRegion = LiveRegionMode.Assertive },
    )
}

@Composable
internal fun LoadingIndicator(modifier: Modifier = Modifier) {
    CircularProgressIndicator(
        modifier = modifier.semantics { contentDescription = "Chargement en cours" },
        color = ThemeConstants.accentBackground,
    )
}
