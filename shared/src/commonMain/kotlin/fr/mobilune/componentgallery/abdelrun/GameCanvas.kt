package fr.mobilune.componentgallery.abdelrun

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.withTransform
import fr.mobilune.componentgallery.core.designsystem.AccessibilityState
import kotlin.random.Random

private val DARK = Color(15, 15, 25)
private val BLOCK_FILL = Color(25, 25, 40)
private val SPIKE_FILL = Color(20, 20, 30)
private val GOLD = Color(0xFFFFD700)
private val ORANGE = Color(0xFFFFA500)
private val YELLOW = Color(0xFFFFFF00)
private val RED = Color(0xFFE62937)

/**
 * Dessine la scène (1024 x 600) en la mettant à l'échelle pour qu'elle tienne dans le composant.
 * [frame] change à chaque image : il force le redessin.
 */
@Composable
fun GameCanvas(
    engine: GameEngine,
    frame: Int,
    bgPlay: ImageBitmap,
    bgPower: ImageBitmap,
    modifier: Modifier = Modifier,
) {
    Canvas(modifier) {
        @Suppress("UNUSED_VARIABLE") val redraw = frame
        val scale = minOf(size.width / SCREEN_W, size.height / SCREEN_H)
        val ox = (size.width - SCREEN_W * scale) / 2f
        val oy = (size.height - SCREEN_H * scale) / 2f
        clipRect(ox, oy, ox + SCREEN_W * scale, oy + SCREEN_H * scale) {
            withTransform({
                translate(ox, oy)
                scale(scale, scale, pivot = Offset.Zero)
            }) {
                drawScene(engine, if (engine.boosting) bgPower else bgPlay)
            }
        }
    }
}

private fun DrawScope.drawScene(engine: GameEngine, bg: ImageBitmap) {
    // « Réduire les animations » : ni pulsation du décor, ni secousse de l'écran.
    val calm = AccessibilityState.reduceMotion
    val beat = if (calm) 0f else engine.beat()
    val dist = engine.dist

    // Décor : image défilante en parallaxe.
    val off = (dist * 0.15f) % bg.width
    var bx = -off
    while (bx < SCREEN_W) {
        drawImage(bg, topLeft = Offset(bx, 0f))
        bx += bg.width
    }
    // Teinte du thème (fondu progressif) puis flash du tempo.
    drawRect(
        Color(
            (engine.tintR / 255f).coerceIn(0f, 1f), (engine.tintG / 255f).coerceIn(0f, 1f),
            (engine.tintB / 255f).coerceIn(0f, 1f), (engine.tintA / 255f).coerceIn(0f, 1f),
        ),
        size = Size(SCREEN_W, SCREEN_H),
    )
    if (beat > 0f) drawRect(Color.White.copy(alpha = 0.07f * beat), size = Size(SCREEN_W, SCREEN_H))

    var sx = 0f
    var sy = 0f
    if (engine.shake > 0 && !calm) {
        sx = (Random.nextFloat() - 0.5f) * engine.shake
        sy = (Random.nextFloat() - 0.5f) * engine.shake
    }

    val hue = (THEMES[engine.zone % THEMES.size].hue + dist * 0.02f) % 360f
    val accent = Color.hsv(hue, 0.55f, (0.85f + 0.15f * beat).coerceAtMost(1f))

    drawGround(engine, sx, sy, accent, beat)
    if (engine.p.mode != MODE_NORMAL) {
        drawRect(DARK, Offset(0f, sy), Size(SCREEN_W, CEIL_Y))
        drawRect(accent, Offset(0f, CEIL_Y + sy - 4), Size(SCREEN_W, 4f))
    }

    for (o in engine.level.objs) {
        val x = o.x - dist + sx
        if (x > SCREEN_W + 50 || x + o.w < -50) continue
        val y = (if (o.zone) SCREEN_H - o.y - o.h else o.y) + sy
        when (o.kind) {
            K_SPIKE -> drawSpike(x, y, o, accent)
            K_BLOCK -> {
                drawRect(BLOCK_FILL, Offset(x, y), Size(o.w, o.h))
                drawRect(accent, Offset(x + 1.5f, y + 1.5f), Size(o.w - 3, o.h - 3), style = Stroke(3f))
            }
            K_RAIL -> {
                drawRect(BLOCK_FILL, Offset(x, y), Size(o.w, o.h))
                drawRect(accent, Offset(x, y), Size(o.w, 4f))
            }
            K_PAD -> drawRect(YELLOW, Offset(x, y), Size(o.w, o.h))
            K_COIN -> if (!o.taken) {
                drawCircle(GOLD, 11f, Offset(x + 11, y + 11))
                drawCircle(ORANGE, 11f, Offset(x + 11, y + 11), style = Stroke(2f))
            }
            K_ORB -> {
                val col = if (o.x == engine.p.lastOrb) YELLOW.copy(alpha = 0.3f) else YELLOW
                val c = Offset(x + o.w / 2, y + o.h / 2)
                drawCircle(col.copy(alpha = col.alpha * 0.35f), 15f, c)
                drawCircle(col, 9 + 3 * beat, c)
                drawCircle(col, 17 + 3 * beat, c, style = Stroke(2f))
            }
            K_PORTAL -> {
                val col = when (o.arg) {
                    MODE_FLIP -> Color(190, 60, 255)
                    MODE_SHIP -> Color(255, 90, 160)
                    else -> Color(60, 220, 120)
                }
                drawRect(col.copy(alpha = 0.25f + 0.15f * beat), Offset(x, 0f), Size(o.w, o.h))
                drawRect(col, Offset(x + o.w / 2 - 2, 0f), Size(4f, o.h))
            }
        }
    }

    for (q in engine.particles) {
        drawRect(
            q.color.copy(alpha = (q.life / q.maxLife).coerceIn(0f, 1f)),
            Offset(q.x + sx, q.y + sy),
            Size(q.size, q.size),
        )
    }

    if (engine.state == RunState.Playing || engine.paused) drawPlayer(engine, sx, sy)
}

private fun DrawScope.drawSpike(x: Float, y: Float, o: Obstacle, accent: Color) {
    val path = Path().apply {
        if (o.zone) { // pointe vers le bas
            moveTo(x + o.w / 2, y + o.h)
            lineTo(x + o.w, y)
            lineTo(x, y)
        } else {
            moveTo(x + o.w / 2, y)
            lineTo(x, y + o.h)
            lineTo(x + o.w, y + o.h)
        }
        close()
    }
    drawPath(path, SPIKE_FILL)
    drawPath(path, RED, style = Stroke(2f))
}

private fun DrawScope.drawGround(engine: GameEngine, sx: Float, sy: Float, accent: Color, beat: Float) {
    val dist = engine.dist
    val pits = engine.level.objs
        .filter { it.kind == K_PIT }
        .map { (it.x - dist) to (it.x - dist + it.w) }
        .filter { (a, b) -> b > 0 && a < SCREEN_W }
        .sortedBy { it.first }
    fun strip(a: Float, b: Float) {
        if (b <= a) return
        drawRect(DARK, Offset(a + sx, GROUND_Y + sy), Size(b - a + 1, SCREEN_H - GROUND_Y + 20))
        drawRect(accent, Offset(a + sx, GROUND_Y + sy), Size(b - a + 1, 4f + 3f * beat))
    }
    var x = 0f
    for ((a, b) in pits) {
        strip(x, a)
        x = b
    }
    strip(x, SCREEN_W)
}

private fun lighten(c: Float): Float = (c + 90f / 255f).coerceAtMost(1f)

private fun DrawScope.drawPlayer(engine: GameEngine, sx: Float, sy: Float) {
    val col = PLAYER_COLOR
    val inner = Color(lighten(col.red), lighten(col.green), lighten(col.blue))
    val center = Offset(PLAYER_X + PLAYER_SIZE / 2 + sx, engine.p.screenY() + PLAYER_SIZE / 2 + sy)
    fun box(size: Float, color: Color) {
        rotate(engine.rot, pivot = center) {
            drawRect(color, Offset(center.x - size / 2, center.y - size / 2), Size(size, size))
        }
    }
    if (engine.p.mode == MODE_SHIP) {
        // Vaisseau : corps allongé + petit cube dessus.
        rotate(engine.rot, pivot = center) {
            drawRoundRect(Color.Black, Offset(center.x - 29, center.y + 6 - 11), Size(58f, 22f), CornerRadius(4f))
            drawRoundRect(col, Offset(center.x - 27, center.y + 6 - 9), Size(54f, 18f), CornerRadius(4f))
            drawRect(inner, Offset(center.x + 6 - 11, center.y - 8 - 11), Size(22f, 22f))
        }
        return
    }
    box(PLAYER_SIZE + 4, Color.Black)
    box(PLAYER_SIZE, col)
    box(PLAYER_SIZE / 2, inner)
}

/** Rectangle de la scène 1024 x 600 mise à l'échelle dans un composant de taille [width] x [height]. */
fun sceneRect(width: Float, height: Float): Rect {
    val scale = minOf(width / SCREEN_W, height / SCREEN_H)
    val w = SCREEN_W * scale
    val h = SCREEN_H * scale
    return Rect((width - w) / 2f, (height - h) / 2f, (width + w) / 2f, (height + h) / 2f)
}
