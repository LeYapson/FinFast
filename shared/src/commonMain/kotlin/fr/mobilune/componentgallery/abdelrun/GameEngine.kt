package fr.mobilune.componentgallery.abdelrun

import androidx.compose.ui.graphics.Color
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.round
import kotlin.math.sin
import kotlin.random.Random

enum class RunState { Playing, Dying, Dead }

private const val BOOST_FACTOR = 1.5f
private const val DEATH_DURATION = 0.8f
private const val ZONE_LENGTH = 5000 // points entre deux changements de thème
private const val ENERGY_DRAIN = 0.35f // par seconde de « pouvoir du temps »
private const val ENERGY_REGEN = 0.2f

/** Tempo supposé de la musique pour les pulsations visuelles (approximation : pas d'analyse audio). */
const val MUSIC_BPM = 128f

class Particle(
    var x: Float, var y: Float, var vx: Float, var vy: Float,
    var life: Float, val maxLife: Float, val size: Float, val color: Color,
)

class Theme(val name: String, val tint: Color, val hue: Float)

val THEMES = listOf(
    Theme("Neon City", Color(0, 0, 0, 70), 200f),
    Theme("Magma", Color(120, 20, 0, 110), 15f),
    Theme("Toxic", Color(10, 80, 20, 110), 110f),
    Theme("Void", Color(50, 0, 90, 120), 280f),
    Theme("Ice", Color(0, 60, 110, 100), 185f),
)

/** Entrées du joueur pour une image : tactile ou clavier, le moteur ne fait pas la différence. */
class GameControls {
    /** Saut maintenu (enchaîne les sauts, monte avec le vaisseau). */
    var jumpHeld = false

    /** Appui frais : consommé par le moteur à chaque mise à jour. */
    var jumpPressed = false

    /** Saut haut vers la voie du haut (maintenu). */
    var upHeld = false

    /** Descente / traversée du rail (appui frais, consommé). */
    var downPressed = false

    /** Pouvoir du temps maintenu. */
    var boostHeld = false

    fun clearPresses() {
        jumpPressed = false
        downPressed = false
    }
}

/**
 * État complet d'une partie. Le moteur est indépendant de l'affichage : [update] fait avancer la
 * simulation, le rendu se contente de lire les champs.
 */
class GameEngine(
    val director: Director = Director(),
    val save: AbdelRunSave = AbdelRunSave(),
    private val random: Random = Random.Default,
    /** Appelé quand [save] a changé et doit être écrit. */
    var onSaveChanged: () -> Unit = {},
) {
    val level = Level(random)

    var p = PState()
        private set
    var dist = 0f
        private set
    var speed = 0f
        private set
    var rot = 0f
        private set

    var score = 0f
        private set

    var state = RunState.Playing
        private set
    private var deathTimer = 0f
    private var acc = 0f
    private var jumpBuf = 0f

    var boosting = false
        private set

    /** Jauge du pouvoir du temps, de 0 à 1. */
    var energy = 1f
        private set

    var paused = false
    val particles = ArrayList<Particle>()
    var shake = 0f
        private set
    var elapsed = 0f
        private set

    var notice = ""
        private set
    var noticeT = 0f
        private set

    var tintR = 0f
    var tintG = 0f
    var tintB = 0f
    var tintA = 0f
    var zone = 0
        private set

    init {
        reset()
    }

    /** Prépare une nouvelle partie. Le directeur (niveau estimé du joueur) est conservé. */
    fun reset() {
        level.reset()
        p = PState()
        dist = 0f
        rot = 0f
        score = 0f
        speed = director.targetSpeed(0f)
        state = RunState.Playing
        deathTimer = 0f
        acc = 0f
        jumpBuf = 0f
        boosting = false
        energy = 1f
        paused = false
        particles.clear()
        shake = 0f
        elapsed = 0f
        notice = ""
        noticeT = 0f
        zone = 0
        val t = THEMES[0].tint
        tintR = t.red * 255
        tintG = t.green * 255
        tintB = t.blue * 255
        tintA = t.alpha * 255
    }

    fun scoreInt(): Int = score.toInt()

    /** Enregistre la partie en cours quand le joueur quitte avant la fin. */
    fun abandon() {
        if (state == RunState.Playing && elapsed > 0) recordRun(died = false)
    }

    private fun recordRun(died: Boolean) {
        save.runs++
        if (score.toInt() > save.best) save.best = score.toInt()
        if (died) save.deaths++
        onSaveChanged()
    }

    private fun say(msg: String) {
        notice = msg
        noticeT = 2.5f
    }

    /** Fait avancer le jeu de [dtIn] secondes et renvoie son état. */
    fun update(dtIn: Float, c: GameControls): RunState {
        val dt = if (dtIn > 0.05f) 0.05f else dtIn
        if (paused) {
            c.clearPresses()
            return state
        }
        if (noticeT > 0) noticeT -= dt

        updateParticles(dt)
        if (shake > 0) shake -= dt * 30

        if (state == RunState.Dying) {
            deathTimer += dt
            if (deathTimer >= DEATH_DURATION) state = RunState.Dead
            c.clearPresses()
            return state
        }
        if (state == RunState.Dead) {
            c.clearPresses()
            return state
        }

        // ---- Entrées ----
        if (c.jumpPressed) jumpBuf = 0.12f
        val jumpHeld = c.jumpHeld
        val upHeld = c.upHeld
        var downPressed = c.downPressed

        // Pouvoir du temps : consomme la jauge, qui se recharge au repos.
        boosting = c.boostHeld && energy > 0.02f
        energy = if (boosting) {
            (energy - ENERGY_DRAIN * dt).coerceAtLeast(0f)
        } else {
            (energy + ENERGY_REGEN * dt).coerceAtMost(1f)
        }
        val mult = if (boosting) BOOST_FACTOR else 1f

        // La vitesse suit doucement la cible choisie par le directeur.
        val target = director.targetSpeed(dist)
        speed += (target - speed) * clampF(dt * 1.5f, 0f, 1f)
        elapsed += dt
        c.clearPresses()

        // ---- Simulation à pas fixe ----
        acc += dt
        while (acc >= STEP && state == RunState.Playing) {
            acc -= STEP
            jumpBuf = clampF(jumpBuf - STEP, 0f, 1f)

            val input = Input(
                jump = jumpHeld || jumpBuf > 0,
                lane = upHeld,
                drop = downPressed,
                press = jumpBuf > 0,
            )
            downPressed = false

            val dx = speed * mult * STEP
            dist += dx
            level.ensure(dist, dist, speed, director)
            stepPlayer(p, level.objs, dist, STEP, input)
            score += dx * 0.1f * mult

            if (p.usedPress) jumpBuf = 0f
            collectCoins(mult)
            trackProgress()

            // rotation du cube
            when {
                p.mode == MODE_SHIP -> rot = (atan2(p.vy, speed) * 180f / PI.toFloat())
                p.grounded -> {
                    val t = round(rot / 90f) * 90f
                    rot += (t - rot) * clampF(STEP * 25, 0f, 1f)
                    if (random.nextFloat() < 0.25f) {
                        emit(PLAYER_X + 4, p.screenY() + PLAYER_SIZE, 1f, Color(0xCC87CEEB))
                    }
                }
                else -> rot += 330 * STEP
            }
            director.observeLane(p.mode == MODE_NORMAL && onRail(), STEP)

            val z = score.toInt() / ZONE_LENGTH
            if (z != zone) {
                zone = z
                say("Zone ${z + 1} - ${THEMES[z % THEMES.size].name}")
            }
            if (p.dead) die()
        }
        return state
    }

    private fun onRail(): Boolean {
        val px = dist + PLAYER_X
        for (o in level.objs) {
            if (o.kind == K_RAIL && px + PLAYER_SIZE > o.x && px < o.x + o.w &&
                abs(o.y - (p.y + PLAYER_SIZE)) < 2
            ) return true
        }
        return false
    }

    /** Les pièces rapportent 100 points (x2 avec le pouvoir du temps). */
    private fun collectCoins(mult: Float) {
        val px = dist + PLAYER_X
        val py = p.screenY()
        for (o in level.objs) {
            if (o.kind != K_COIN || o.taken || o.x > px + PLAYER_SIZE + 40) continue
            if (overlap(px, py, PLAYER_SIZE, PLAYER_SIZE, o.x - 4, o.y - 4, o.w + 8, o.h + 8)) {
                o.taken = true
                score += 100 * mult
                repeat(8) { emit(PLAYER_X + 20, o.y + 11, 3f, Color(0xFFFFD700)) }
            }
        }
    }

    /** Informe le directeur des patterns franchis. */
    private fun trackProgress() {
        val px = dist + PLAYER_X
        for (c in level.chunks) {
            if (!c.passed && px > c.x1 + 20) {
                c.passed = true
                director.passed(c.id)
            }
        }
    }

    private fun die() {
        state = RunState.Dying
        shake = 12f
        val id = level.chunkAt(dist + PLAYER_X + PLAYER_SIZE)
        director.died(id ?: 0, id != null)
        recordRun(died = true)
        repeat(40) {
            val a = random.nextDouble() * 2 * PI
            val sp = 100 + random.nextFloat() * 500
            particles.add(
                Particle(
                    x = PLAYER_X + 20, y = p.screenY() + 20,
                    vx = cos(a).toFloat() * sp, vy = sin(a).toFloat() * sp,
                    life = 0.8f, maxLife = 0.8f, size = 4 + random.nextFloat() * 8, color = PLAYER_COLOR,
                ),
            )
        }
    }

    private fun emit(x: Float, y: Float, size: Float, col: Color) {
        particles.add(
            Particle(
                x = x, y = y, vx = -80 - random.nextFloat() * 120, vy = -random.nextFloat() * 120,
                life = 0.4f, maxLife = 0.4f, size = size + random.nextFloat() * 3, color = col,
            ),
        )
    }

    private fun updateParticles(dt: Float) {
        var k = 0
        for (i in particles.indices) {
            val q = particles[i]
            q.life -= dt
            if (q.life <= 0) continue
            q.x += q.vx * dt
            q.y += q.vy * dt
            q.vy += 900 * dt
            particles[k++] = q
        }
        while (particles.size > k) particles.removeAt(particles.size - 1)
    }

    /** Pulsation 0..1 calée sur le tempo supposé de la musique. */
    fun beat(): Float {
        val phase = (elapsed * MUSIC_BPM / 60f).toDouble() % 1.0
        return exp(-phase * 5).toFloat()
    }

    /** Avance le fondu de la teinte du thème vers la teinte cible de la zone. */
    fun updateTint(dt: Float) {
        val th = THEMES[zone % THEMES.size].tint
        val k = clampF(dt * 1.2f, 0f, 1f)
        tintR += (th.red * 255 - tintR) * k
        tintG += (th.green * 255 - tintG) * k
        tintB += (th.blue * 255 - tintB) * k
        tintA += (th.alpha * 255 - tintA) * k
    }
}
