package fr.mobilune.componentgallery.abdelrun

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource

val ButtonYellow = Color(0xFFFFEB3B)

/**
 * Échelle de la scène 1024 x 600 : 1 pixel de scène = [unit] dp. Les tailles de texte et de bouton
 * sont bornées pour rester lisibles et faciles à toucher sur un petit écran.
 */
@Immutable
class SceneScale(val unit: Float) {
    fun dp(value: Float, min: Float = 0f): Dp = maxOf(value * unit, min).dp
    fun sp(value: Float, min: Float = 13f): TextUnit = maxOf(value * unit, min).sp
}

/**
 * Cadre 1024 x 600 centré sur fond noir (bandes sur les écrans plus larges), avec [background]
 * en fond et un voile sombre optionnel de [dim] (0 à 1) pour la lisibilité du texte.
 */
@Composable
fun SceneBox(
    background: DrawableResource?,
    modifier: Modifier = Modifier,
    dim: Float = 0f,
    content: @Composable BoxScope.(SceneScale) -> Unit,
) {
    Box(
        modifier = modifier.fillMaxSize().background(Color.Black).safeContentPadding(),
        contentAlignment = Alignment.Center,
    ) {
        BoxWithConstraints(Modifier.aspectRatio(SCREEN_W / SCREEN_H)) {
            val scale = SceneScale(maxHeight.value / SCREEN_H)
            if (background != null) {
                Image(
                    painter = painterResource(background),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.FillBounds,
                )
            }
            if (dim > 0f) Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = dim)))
            content(scale)
        }
    }
}

/** Bouton jaune arrondi du jeu d'origine (texte noir). */
@Composable
fun SceneButton(
    text: String,
    onClick: () -> Unit,
    scale: SceneScale,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    container: Color = ButtonYellow,
    contentColor: Color = Color.Black,
) {
    Box(
        modifier = modifier
            .heightIn(min = scale.dp(40f, 48f))
            .clip(RoundedCornerShape(50))
            .background(if (enabled) container else container.copy(alpha = 0.4f))
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            color = contentColor,
            fontSize = scale.sp(20f),
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Center,
            modifier = Modifier.then(Modifier),
        )
    }
}
