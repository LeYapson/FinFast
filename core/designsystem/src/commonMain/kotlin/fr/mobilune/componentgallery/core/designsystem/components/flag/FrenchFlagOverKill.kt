package fr.mobilune.componentgallery.core.designsystem.components.flag

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin


/** 👩‍💻 Prompt : Opus
 * Je travaille sur Compose Multiplatform. Déssine un joli grand drapeau français
 */

// Couleurs officielles de la marque État (Bleu France / Rouge Marianne)
private val BleuFrance = Color(0xFF000091)
private val Blanc = Color(0xFFFFFFFF)
private val RougeMarianne = Color(0xFFE1000F)

private const val TWO_PI = (2 * PI).toFloat()

/**
 * Drapeau français animé qui ondule au vent.
 *
 * @param waves nombre d'ondulations visibles sur la largeur
 * @param amplitudeRatio amplitude de la vague, en fraction de la hauteur
 * @param durationMillis durée d'un cycle complet de vague
 */
@Composable
fun FrenchFlagOverKill(
    modifier: Modifier = Modifier,
    waves: Float = 1.5f,
    amplitudeRatio: Float = 0.06f,
    durationMillis: Int = 800,
) {
    val transition = rememberInfiniteTransition(label = "flag")
    val phase by transition.animateFloat(
        initialValue = 0f,
        targetValue = TWO_PI,
        animationSpec = infiniteRepeatable(tween(durationMillis, easing = LinearEasing)),
        label = "phase",
    )

    Canvas(modifier = modifier.aspectRatio(3f / 2f)) {
        val slices = 90 // multiple de 3 pour des bandes nettes
        val amplitude = size.height * amplitudeRatio
        val top = amplitude
        val flagHeight = size.height - 2 * amplitude
        val sliceWidth = size.width / slices
        val path = Path()

        // t = position horizontale normalisée (0 = côté hampe, 1 = bord libre).
        // Le facteur t "accroche" le drapeau à gauche : il ondule davantage vers la droite.
        fun offsetAt(t: Float) = amplitude * t * sin(t * waves * TWO_PI - phase)
        fun slopeAt(t: Float) = t * cos(t * waves * TWO_PI - phase)

        fun buildSlice(i: Int, dx: Float = 0f, dy: Float = 0f): Path {
            val x0 = i * sliceWidth
            val x1 = x0 + sliceWidth
            val y0 = offsetAt(x0 / size.width)
            val y1 = offsetAt(x1 / size.width)
            val overlap = 0.6f // évite les fines lignes entre les tranches
            path.reset()
            path.moveTo(x0 + dx, top + y0 + dy)
            path.lineTo(x1 + overlap + dx, top + y1 + dy)
            path.lineTo(x1 + overlap + dx, top + flagHeight + y1 + dy)
            path.lineTo(x0 + dx, top + flagHeight + y0 + dy)
            path.close()
            return path
        }

        // 1) Ombre portée douce
        val shadowDx = size.width * 0.012f
        val shadowDy = size.height * 0.025f
        for (i in 0 until slices) {
            drawPath(buildSlice(i, shadowDx, shadowDy), Color.Black, alpha = 0.12f)
        }

        // 2) Le drapeau + ombrage des plis
        for (i in 0 until slices) {
            val t = (i + 0.5f) / slices
            val color = when {
                i < slices / 3 -> BleuFrance
                i < 2 * slices / 3 -> Blanc
                else -> RougeMarianne
            }
            val slice = buildSlice(i)
            drawPath(slice, color)

            // Lumière sur les pentes montantes, ombre dans les creux
            val shade = slopeAt(t) * 0.22f
            if (shade > 0f) {
                drawPath(slice, Color.White, alpha = shade * 0.6f)
            } else {
                drawPath(slice, Color.Black, alpha = -shade)
            }
        }
    }
}