package fr.mobilune.componentgallery.abdelrun

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import componentgallery.shared.generated.resources.Res
import componentgallery.shared.generated.resources.abdelrun_game_over
import componentgallery.shared.generated.resources.abdelrun_logo
import componentgallery.shared.generated.resources.abdelrun_title
import fr.mobilune.componentgallery.projects.DAILY_BONUS_CAP_CENTS
import fr.mobilune.componentgallery.projects.RunResult
import fr.mobilune.componentgallery.projects.formatEuros
import fr.mobilune.componentgallery.projects.formatEurosShort
import fr.mobilune.componentgallery.core.designsystem.rememberAppHaptics
import kotlinx.coroutines.delay

private val Panel = Color.Black.copy(alpha = 0.6f)
private val GoldColor = Color(0xFFFFD700)

@Composable
private fun SceneText(
    text: String,
    scale: SceneScale,
    size: Float,
    modifier: Modifier = Modifier,
    color: Color = Color.White,
    weight: FontWeight = FontWeight.Normal,
    align: TextAlign = TextAlign.Start,
) {
    Text(
        text = text,
        modifier = modifier,
        color = color,
        fontSize = scale.sp(size),
        fontWeight = weight,
        textAlign = align,
    )
}

/** Logo « Abdelou Games » affiché 2 secondes (touche l'écran pour passer). */
@Composable
fun GameSplashScreen(onFinished: () -> Unit) {
    LaunchedEffect(Unit) {
        delay(2_000)
        onFinished()
    }
    SceneBox(Res.drawable.abdelrun_logo, modifier = Modifier.clickable(onClick = onFinished)) { }
}

/** Menu principal : Jouer, ou retour au projet. */
@Composable
fun GameMenuScreen(
    projectName: String,
    dailyRemainingCents: Long,
    errorMessage: String?,
    onPlay: () -> Unit,
    onBack: () -> Unit,
) {
    SceneBox(Res.drawable.abdelrun_title) { s ->
        Column(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = s.dp(40f, 12f), top = s.dp(40f))
                .width(s.dp(360f, 280f)),
            verticalArrangement = Arrangement.spacedBy(s.dp(10f, 8f)),
        ) {
            SceneButton("Jouer", onPlay, s, Modifier.fillMaxWidth())
            SceneButton("Retour au projet", onBack, s, Modifier.fillMaxWidth(), container = Color.White)
            // Sous les boutons (et non superposé) : le projet et le plafond du jour.
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(s.dp(12f, 8f)))
                    .background(Panel)
                    .padding(horizontal = s.dp(14f, 10f), vertical = s.dp(8f, 6f)),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                SceneText("Projet : $projectName", s, 16f, color = Color.White, align = TextAlign.Center)
                SceneText(
                    text = if (dailyRemainingCents > 0) {
                        "Gain possible aujourd'hui : ${formatEuros(dailyRemainingCents)}"
                    } else {
                        "Plafond du jour atteint (reviens demain)"
                    },
                    scale = s,
                    size = 14f,
                    color = if (dailyRemainingCents > 0) GoldColor else Color(0xFFFFA726),
                    align = TextAlign.Center,
                )
            }
        }
        if (errorMessage != null) {
            SceneText(
                errorMessage, s, 16f,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = s.dp(12f, 8f))
                    .clip(RoundedCornerShape(s.dp(8f, 6f)))
                    .background(Color(0xCCB3261E))
                    .padding(horizontal = s.dp(12f, 8f), vertical = s.dp(6f, 4f)),
            )
        }
    }
}

/** Fin de partie : score, conversion en bonus pour le projet et jauge d'avancement. */
@Composable
fun GameOverScreen(
    result: RunResult,
    onRestart: () -> Unit,
    onMenu: () -> Unit,
    onBackToProject: () -> Unit,
) {
    val haptic = rememberAppHaptics()
    LaunchedEffect(result) {
        haptic.performHapticFeedback(
            if (result.bonusCents > 0) HapticFeedbackType.Confirm else HapticFeedbackType.Reject,
        )
    }
    SceneBox(Res.drawable.abdelrun_game_over) { s ->
        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .padding(top = s.dp(70f))
                .width(s.dp(400f, 300f))
                .clip(RoundedCornerShape(s.dp(16f, 10f)))
                .background(Color.White.copy(alpha = 0.88f))
                .padding(s.dp(14f, 10f)),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(s.dp(6f, 4f)),
        ) {
            SceneText("Score : ${result.score}", s, 36f, color = Color.Black, weight = FontWeight.Bold)
            val gain = if (result.bonusCents > 0) {
                "+${formatEuros(result.bonusCents)} pour « ${result.projectName} »"
            } else {
                "Aucun gain cette fois"
            }
            SceneText(
                gain, s, 22f,
                color = if (result.bonusCents > 0) Color(0xFF135E45) else Color(0xFF6B6B6B),
                weight = FontWeight.Bold,
                align = TextAlign.Center,
            )
            if (result.cappedByDailyLimit) {
                SceneText(
                    "Plafond du jour atteint : le gain est limité à ${formatEurosShort(DAILY_BONUS_CAP_CENTS)} par jour.",
                    s, 15f, color = Color(0xFFB3261E), align = TextAlign.Center,
                )
            }
            val target = result.targetCents.coerceAtLeast(1)
            val before = (result.savedBeforeCents.toFloat() / target).coerceIn(0f, 1f)
            val after = (result.savedAfterCents.toFloat() / target).coerceIn(0f, 1f)
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(s.dp(14f, 10f))
                    .clip(RoundedCornerShape(50))
                    .background(Color(0xFFDDDDDD)),
            ) {
                Box(Modifier.fillMaxWidth(after).fillMaxHeight().background(Color(0xFF53D447)))
                Box(Modifier.fillMaxWidth(before).fillMaxHeight().background(Color(0xFF135E45)))
            }
            SceneText(
                "${formatEurosShort(result.savedAfterCents)} / ${formatEurosShort(result.targetCents)}",
                s, 18f, color = Color.Black,
            )
            Spacer(Modifier.height(s.dp(4f)))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(s.dp(10f, 8f))) {
                SceneButton("Rejouer", onRestart, s, Modifier.weight(1f))
                SceneButton("Menu", onMenu, s, Modifier.weight(1f))
            }
            SceneButton(
                "Retour au projet", onBackToProject, s, Modifier.fillMaxWidth(),
                container = Color.Black, contentColor = Color.White,
            )
        }
    }
}
