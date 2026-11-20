package fr.mobilune.componentgallery.abdelrun

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import componentgallery.shared.generated.resources.Res
import componentgallery.shared.generated.resources.abdelrun_bg_play
import componentgallery.shared.generated.resources.abdelrun_bg_power
import fr.mobilune.componentgallery.rememberAppHaptics
import org.jetbrains.compose.resources.imageResource

private val HudWhite = Color.White
private val ControlFill = Color.Black.copy(alpha = 0.45f)

/**
 * Écran de partie : la scène, le HUD et les commandes tactiles.
 * [onDead] est appelé une fois quand le cube a fini d'exploser ; [onExit] quand le joueur quitte.
 */
@Composable
fun GamePlayScreen(
    engine: GameEngine,
    audio: AbdelRunAudio,
    onDead: () -> Unit,
    onExit: () -> Unit,
) {
    val controls = remember(engine) { GameControls() }
    var frame by remember { mutableIntStateOf(0) }
    var runState by remember { mutableStateOf(engine.state) }
    var boosting by remember { mutableStateOf(false) }
    var paused by remember { mutableStateOf(engine.paused) }
    val haptic = rememberAppHaptics()
    val focusRequester = remember { FocusRequester() }
    val bgPlay = imageResource(Res.drawable.abdelrun_bg_play)
    val bgPower = imageResource(Res.drawable.abdelrun_bg_power)

    // Boucle de jeu : une mise à jour par image affichée.
    LaunchedEffect(engine) {
        var last = 0L
        while (true) {
            withFrameNanos { now ->
                val dt = if (last == 0L) 0f else (now - last) / 1_000_000_000f
                last = now
                runState = engine.update(dt, controls)
                engine.updateTint(dt)
                boosting = engine.boosting
                paused = engine.paused
                frame++
            }
        }
    }
    LaunchedEffect(Unit) { runCatching { focusRequester.requestFocus() } }

    // Musique : normale, « pouvoir du temps », silence à la mort ou en pause.
    LaunchedEffect(runState, boosting, paused) {
        when {
            runState != RunState.Playing -> audio.stop()
            paused -> audio.pause()
            else -> {
                audio.resume()
                audio.play(if (boosting) AudioTrack.Power else AudioTrack.Play)
            }
        }
    }
    // Retour haptique à la mort.
    LaunchedEffect(runState) {
        if (runState == RunState.Dying) haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        if (runState == RunState.Dead) onDead()
    }
    // Application en arrière-plan : la partie se met en pause.
    LifecycleEventEffect(Lifecycle.Event.ON_PAUSE) {
        if (engine.state == RunState.Playing) engine.paused = true
        audio.pause()
    }

    fun togglePause() {
        if (engine.state == RunState.Playing) engine.paused = !engine.paused
    }

    SceneBox(background = null) { s ->
        GameCanvas(engine, frame, bgPlay, bgPower, Modifier.fillMaxSize())

        // Zone de saut : tout l'écran. Les boutons au-dessus consomment leurs propres appuis.
        Box(
            Modifier
                .fillMaxSize()
                .focusRequester(focusRequester)
                .focusable()
                .onKeyEvent { event -> handleKey(event, controls, ::togglePause) }
                .pointerInput(controls) {
                    awaitEachGesture {
                        awaitFirstDown(requireUnconsumed = true)
                        controls.jumpHeld = true
                        controls.jumpPressed = true
                        do {
                            val event = awaitPointerEvent()
                        } while (event.changes.any { it.pressed })
                        controls.jumpHeld = false
                    }
                }
                .semantics { contentDescription = "Zone de jeu : touche l'écran pour sauter" },
        )

        // --- HUD ---
        Column(Modifier.align(Alignment.TopStart).padding(s.dp(20f, 10f))) {
            HudText("Score ${engine.scoreInt()}", s, 36f, FontWeight.Bold)
            HudText("Record ${engine.save.best}", s, 18f, color = Color.LightGray)
            HudText("Morts : ${engine.save.deaths}", s, 18f, color = Color.LightGray)
        }
        Column(Modifier.align(Alignment.TopEnd).padding(s.dp(20f, 10f)), horizontalAlignment = Alignment.End) {
            HudText("Niveau ${(engine.director.skill * 10).toInt() + 1}", s, 24f, color = Color(0xFFFFEB3B))
            HudText("POUVOIR DU TEMPS", s, 14f)
            Box(
                Modifier
                    .width(s.dp(170f, 110f))
                    .height(s.dp(10f, 7f))
                    .clip(RoundedCornerShape(50))
                    .background(Color.White.copy(alpha = 0.25f))
                    .semantics { contentDescription = "Énergie du pouvoir du temps ${(engine.energy * 100).toInt()} pour cent" },
            ) {
                Box(
                    Modifier
                        .fillMaxWidth(engine.energy.coerceIn(0f, 1f))
                        .height(s.dp(10f, 7f))
                        .background(if (boosting) Color(0xFFFF4081) else Color(0xFF40C4FF)),
                )
            }
        }
        Row(
            Modifier.align(Alignment.TopCenter).padding(top = s.dp(12f, 6f)),
            horizontalArrangement = Arrangement.spacedBy(s.dp(10f, 8f)),
        ) {
            SceneButton("Pause", { togglePause() }, s, Modifier.width(s.dp(110f, 80f)), container = Color.White.copy(alpha = 0.85f))
            SceneButton("Quitter", onExit, s, Modifier.width(s.dp(110f, 80f)))
        }

        if (engine.noticeT > 0f && engine.notice.isNotEmpty()) {
            HudText(
                engine.notice, s, 32f, FontWeight.Bold,
                modifier = Modifier.align(Alignment.Center).padding(bottom = s.dp(160f)),
                color = Color.White.copy(alpha = engine.noticeT.coerceIn(0f, 1f)),
            )
        }
        if (engine.elapsed < 6f) {
            HudText(
                "Touche : sauter   ▲ grand saut   ▼ descendre   ⚡ pouvoir du temps",
                s, 18f, modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = s.dp(14f, 8f)),
                color = Color(0xFFFFEB3B),
            )
        }

        // --- Commandes tactiles (colonne de gauche) ---
        Column(
            Modifier.align(Alignment.BottomStart).padding(start = s.dp(14f, 8f), bottom = s.dp(14f, 8f)),
            verticalArrangement = Arrangement.spacedBy(s.dp(10f, 8f)),
        ) {
            HoldButton("⚡", "Pouvoir du temps", s, onPress = { controls.boostHeld = true }, onRelease = { controls.boostHeld = false })
            HoldButton(
                "▲", "Grand saut vers la voie du haut", s,
                onPress = {
                    controls.upHeld = true
                    controls.jumpPressed = true
                },
                onRelease = { controls.upHeld = false },
            )
            HoldButton("▼", "Descendre", s, onPress = { controls.downPressed = true }, onRelease = {})
        }
        if (paused && runState == RunState.Playing) {
            Box(
                Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.55f)).clickable(enabled = false) {},
                contentAlignment = Alignment.Center,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(s.dp(10f, 8f))) {
                    HudText("PAUSE", s, 60f, FontWeight.Bold)
                    SceneButton("Reprendre", { engine.paused = false }, s, Modifier.width(s.dp(240f, 180f)))
                    SceneButton("Quitter la partie", onExit, s, Modifier.width(s.dp(240f, 180f)), container = Color.White)
                }
            }
        }
    }
}

@Composable
private fun HudText(
    text: String,
    s: SceneScale,
    size: Float,
    weight: FontWeight = FontWeight.Normal,
    modifier: Modifier = Modifier,
    color: Color = HudWhite,
) {
    Text(text = text, modifier = modifier, color = color, fontSize = s.sp(size), fontWeight = weight, textAlign = TextAlign.Center)
}

/** Bouton rond maintenu (pouvoir du temps) ou touché (grand saut, descente). */
@Composable
private fun HoldButton(
    label: String,
    description: String,
    s: SceneScale,
    onPress: () -> Unit,
    onRelease: () -> Unit,
) {
    Box(
        modifier = Modifier
            .size(s.dp(60f, 52f))
            .clip(CircleShape)
            .background(ControlFill)
            .semantics { contentDescription = description; role = Role.Button }
            .pointerInput(Unit) {
                awaitEachGesture {
                    val down = awaitFirstDown()
                    down.consume()
                    onPress()
                    waitForUpOrCancellation()
                    onRelease()
                }
            },
        contentAlignment = Alignment.Center,
    ) {
        Text(label, color = Color.White, fontSize = s.sp(26f, 20f), fontWeight = FontWeight.Bold)
    }
}

/** Raccourcis clavier (desktop) : Espace saute, Haut grand saut, Bas descend, W/Z pouvoir, P pause. */
private fun handleKey(
    event: androidx.compose.ui.input.key.KeyEvent,
    controls: GameControls,
    onPause: () -> Unit,
): Boolean {
    val down = event.type == KeyEventType.KeyDown
    val up = event.type == KeyEventType.KeyUp
    if (!down && !up) return false
    return when (event.key) {
        Key.Spacebar -> {
            if (down && !controls.jumpHeld) controls.jumpPressed = true
            controls.jumpHeld = down
            true
        }
        Key.DirectionUp -> {
            if (down && !controls.upHeld) controls.jumpPressed = true
            controls.upHeld = down
            true
        }
        Key.DirectionDown -> {
            if (down) controls.downPressed = true
            true
        }
        Key.W, Key.Z -> {
            controls.boostHeld = down
            true
        }
        Key.P -> {
            if (down) onPause()
            true
        }
        else -> false
    }
}
