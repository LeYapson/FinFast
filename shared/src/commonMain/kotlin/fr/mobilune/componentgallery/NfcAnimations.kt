package fr.mobilune.componentgallery

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.lerp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private const val SCAN_SUCCESS_DISPLAY_MILLIS = 2_500L

/** Symbole « sans contact » : quatre arcs qui s'allument l'un après l'autre. */
@Composable
internal fun NfcSymbol(
    modifier: Modifier = Modifier,
    color: Color = ThemeConstants.accentBackground,
) {
    val reduceMotion = AccessibilityState.reduceMotion
    val transition = rememberInfiniteTransition(label = "nfc_symbol")
    val animatedPhase by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(durationMillis = 1_600, easing = LinearEasing)),
        label = "nfc_symbol_phase",
    )
    Canvas(modifier.clearAndSetSemantics {}) {
        val strokeWidth = size.minDimension * 0.07f
        val centerX = size.width * 0.2f
        val centerY = size.height / 2f
        val step = size.width * 0.17f
        repeat(4) { index ->
            val radius = step * (index + 1)
            val sinceLit = ((animatedPhase - index * 0.2f) % 1f + 1f) % 1f
            // Mouvement réduit : les quatre arcs restent allumés, sans clignotement.
            val alpha = if (reduceMotion) 1f else 0.2f + 0.8f * (1f - sinceLit * 1.5f).coerceAtLeast(0f)
            drawArc(
                color = color.copy(alpha = alpha),
                startAngle = -45f,
                sweepAngle = 90f,
                useCenter = false,
                topLeft = Offset(centerX - radius, centerY - radius),
                size = Size(radius * 2, radius * 2),
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
            )
        }
    }
}

/** Une carte qui s'approche du dos d'un téléphone, déclenche des ondes NFC puis s'éloigne. */
@Composable
internal fun NfcApproachAnimation(modifier: Modifier = Modifier) {
    val reduceMotion = AccessibilityState.reduceMotion
    val transition = rememberInfiniteTransition(label = "nfc_approach")
    val animatedProgress by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(durationMillis = 3_400, easing = LinearEasing)),
        label = "nfc_approach_progress",
    )
    val accent = ThemeConstants.accentBackground

    // Décoratif : le texte de l'écran explique déjà quoi faire.
    Canvas(modifier.fillMaxWidth().height(270.dp).clearAndSetSemantics {}) {
        // Mouvement réduit : image fixe de la carte posée sur le téléphone, sans ondes.
        val progress = if (reduceMotion) 0.42f else animatedProgress
        val center = Offset(size.width / 2f, size.height / 2f)

        // Téléphone vu de dos.
        val phoneSize = Size(132.dp.toPx(), 232.dp.toPx())
        val phoneTopLeft = center - Offset(phoneSize.width / 2f, phoneSize.height / 2f)
        val phoneCorner = CornerRadius(28.dp.toPx())
        drawRoundRect(Color.White, phoneTopLeft, phoneSize, phoneCorner)
        drawRoundRect(
            color = ThemeConstants.border,
            topLeft = phoneTopLeft,
            size = phoneSize,
            cornerRadius = phoneCorner,
            style = Stroke(width = 3.dp.toPx()),
        )
        drawCircle(
            color = ThemeConstants.border,
            radius = 7.dp.toPx(),
            center = phoneTopLeft + Offset(30.dp.toPx(), 30.dp.toPx()),
        )

        // Carte : 0–50 % approche, 50–80 % contact, 80–100 % retour.
        val approach = when {
            progress < 0.5f -> FastOutSlowInEasing.transform(progress / 0.5f)
            progress < 0.8f -> 1f
            else -> 1f - FastOutSlowInEasing.transform((progress - 0.8f) / 0.2f)
        }
        val contactCenter = center + Offset(0f, 8.dp.toPx())
        val startCenter = contactCenter + Offset(-size.width * 0.28f, size.height * 0.3f)
        val cardCenter = Offset(
            x = lerp(startCenter.x, contactCenter.x, approach),
            y = lerp(startCenter.y, contactCenter.y, approach),
        )
        val cardScale = lerp(0.72f, 1f, approach)
        val cardRotation = lerp(-16f, 0f, approach)
        val cardAlpha = (approach * 3f).coerceIn(0f, 1f)
        val cardSize = Size(108.dp.toPx(), 68.dp.toPx())
        val cardTopLeft = cardCenter - Offset(cardSize.width / 2f, cardSize.height / 2f)

        // Ondes NFC pendant le contact, dessinées sous la carte.
        if (progress in 0.45f..0.85f) {
            val waves = (progress - 0.45f) / 0.4f
            repeat(3) { index ->
                val local = ((waves * 1.4f) - index * 0.28f).coerceIn(0f, 1f)
                if (local > 0f && local < 1f) {
                    drawCircle(
                        color = accent.copy(alpha = (1f - local) * 0.55f),
                        radius = lerp(30.dp.toPx(), 118.dp.toPx(), local),
                        center = contactCenter,
                        style = Stroke(width = 3.dp.toPx()),
                    )
                }
            }
        }

        withTransform({
            rotate(cardRotation, pivot = cardCenter)
            scale(cardScale, cardScale, pivot = cardCenter)
        }) {
            drawRoundRect(
                color = Color(0xFF060606).copy(alpha = cardAlpha),
                topLeft = cardTopLeft,
                size = cardSize,
                cornerRadius = CornerRadius(14.dp.toPx()),
            )
            // Puce.
            drawRoundRect(
                color = Color(0xFFD9B66B).copy(alpha = cardAlpha),
                topLeft = cardTopLeft + Offset(12.dp.toPx(), 13.dp.toPx()),
                size = Size(20.dp.toPx(), 15.dp.toPx()),
                cornerRadius = CornerRadius(4.dp.toPx()),
            )
            // Bandeau de numéro.
            drawRoundRect(
                color = Color.White.copy(alpha = 0.25f * cardAlpha),
                topLeft = cardTopLeft + Offset(12.dp.toPx(), 49.dp.toPx()),
                size = Size(52.dp.toPx(), 5.dp.toPx()),
                cornerRadius = CornerRadius(3.dp.toPx()),
            )
        }
    }
}

/** Pastille de validation : disque qui rebondit, onde qui part du centre, coche tracée progressivement. */
@Composable
internal fun ScanSuccessBadge(modifier: Modifier = Modifier) {
    val scale = remember { Animatable(0f) }
    val check = remember { Animatable(0f) }
    val burst = remember { Animatable(0f) }
    val accent = ThemeConstants.accentBackground

    LaunchedEffect(Unit) {
        if (AccessibilityState.reduceMotion) {
            burst.snapTo(1f)
            scale.snapTo(1f)
            check.snapTo(1f)
            return@LaunchedEffect
        }
        launch { burst.animateTo(1f, tween(durationMillis = 750)) }
        scale.animateTo(
            targetValue = 1f,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessMedium,
            ),
        )
        check.animateTo(1f, tween(durationMillis = 320))
    }

    Canvas(modifier.clearAndSetSemantics {}) {
        val center = Offset(size.width / 2f, size.height / 2f)
        val radius = size.minDimension * 0.3f

        if (burst.value < 1f) {
            drawCircle(
                color = accent.copy(alpha = (1f - burst.value) * 0.45f),
                radius = lerp(radius, size.minDimension * 0.5f, burst.value),
                center = center,
                style = Stroke(width = 3.dp.toPx()),
            )
        }
        drawCircle(accent, radius * scale.value, center)

        val tick = Path().apply {
            moveTo(center.x - radius * 0.42f, center.y + radius * 0.02f)
            lineTo(center.x - radius * 0.1f, center.y + radius * 0.34f)
            lineTo(center.x + radius * 0.46f, center.y - radius * 0.3f)
        }
        val measure = PathMeasure().apply { setPath(tick, false) }
        val visible = Path()
        measure.getSegment(0f, measure.length * check.value, visible, true)
        drawPath(
            path = visible,
            color = ThemeConstants.onAccentBackground,
            style = Stroke(width = radius * 0.16f, cap = StrokeCap.Round, join = StrokeJoin.Round),
        )
    }
}

/**
 * Écran « carte virtuelle active » : logo NFC en attente de scan, puis validation quand un lecteur
 * a lu la carte ([scanVersion] augmente à chaque lecture), avec retour haptique.
 */
@Composable
internal fun NfcPayPanel(
    scanVersion: Int,
    modifier: Modifier = Modifier,
) {
    val haptic = rememberAppHaptics()
    val initialScanVersion = remember { scanVersion }
    var showSuccess by remember { mutableStateOf(false) }

    LaunchedEffect(scanVersion) {
        if (scanVersion == initialScanVersion) return@LaunchedEffect
        showSuccess = true
        haptic.performHapticFeedback(HapticFeedbackType.Confirm)
        delay(SCAN_SUCCESS_DISPLAY_MILLIS)
        showSuccess = false
    }

    AnimatedContent(
        targetState = showSuccess,
        // Région vivante : TalkBack annonce « Carte lue… » dès que le scan est validé.
        modifier = modifier.semantics(mergeDescendants = true) { liveRegion = LiveRegionMode.Polite },
        transitionSpec = {
            if (AccessibilityState.reduceMotion) {
                ContentTransform(EnterTransition.None, ExitTransition.None)
            } else {
                ContentTransform(
                    targetContentEnter = fadeIn(tween(250)) + scaleIn(tween(250), initialScale = 0.9f),
                    initialContentExit = fadeOut(tween(180)),
                )
            }
        },
        label = "nfc_pay_panel",
    ) { success ->
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Box(Modifier.size(220.dp), contentAlignment = Alignment.Center) {
                if (success) {
                    ScanSuccessBadge(Modifier.size(220.dp))
                } else {
                    NfcWaitingRipples(Modifier.size(220.dp))
                    NfcSymbol(Modifier.size(96.dp))
                }
            }
            Spacer(Modifier.height(16.dp))
            Text(
                text = if (success) "Carte lue" else "Prête à être scannée",
                color = ThemeConstants.onClassicBackground,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = if (success) {
                    "Le lecteur a bien récupéré ton identifiant."
                } else {
                    "Approche l'arrière du téléphone du lecteur NFC."
                },
                color = ThemeConstants.secondaryText,
                fontSize = 15.sp,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun NfcWaitingRipples(modifier: Modifier = Modifier) {
    val reduceMotion = AccessibilityState.reduceMotion
    val transition = rememberInfiniteTransition(label = "nfc_waiting")
    val animatedPhase by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(durationMillis = 2_400, easing = LinearEasing)),
        label = "nfc_waiting_phase",
    )
    val accent = ThemeConstants.accentBackground
    Canvas(modifier.clearAndSetSemantics {}) {
        // Mouvement réduit : anneaux figés.
        val phase = if (reduceMotion) 0.3f else animatedPhase
        val center = Offset(size.width / 2f, size.height / 2f)
        drawCircle(accent.copy(alpha = 0.08f), size.minDimension * 0.28f, center)
        repeat(3) { index ->
            val local = (phase + index / 3f) % 1f
            drawCircle(
                color = accent.copy(alpha = (1f - local) * 0.4f),
                radius = lerp(size.minDimension * 0.22f, size.minDimension * 0.5f, local),
                center = center,
                style = Stroke(width = 2.5.dp.toPx()),
            )
        }
    }
}
