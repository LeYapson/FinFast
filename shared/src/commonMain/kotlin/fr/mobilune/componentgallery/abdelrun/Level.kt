package fr.mobilune.componentgallery.abdelrun

import kotlin.math.ceil
import kotlin.random.Random

// ---------------------------------------------------------------------------
// Constructeurs d'éléments
// ---------------------------------------------------------------------------

private fun spikeAt(x: Float, base: Float) = Obstacle(K_SPIKE, x, base - 40, 40f, 40f)
internal fun spikeG(x: Float) = spikeAt(x, GROUND_Y)
internal fun blockG(x: Float, w: Float, h: Float) = Obstacle(K_BLOCK, x, GROUND_Y - h, w, h)
private fun railAt(x: Float, w: Float) = Obstacle(K_RAIL, x, RAIL_Y, w, RAIL_H)
internal fun pitAt(x: Float, w: Float) = Obstacle(K_PIT, x, GROUND_Y, w, SCREEN_H - GROUND_Y)
private fun padG(x: Float) = Obstacle(K_PAD, x, GROUND_Y - 12, 40f, 12f)
private fun coinAt(x: Float, y: Float) = Obstacle(K_COIN, x, y, 22f, 22f)
private fun orbAt(x: Float, y: Float) = Obstacle(K_ORB, x, y, 30f, 30f)
private fun portalAt(x: Float, mode: Int) = Obstacle(K_PORTAL, x, 0f, 24f, GROUND_Y, arg = mode)

/** Bloc accroché au plafond (bas à la hauteur [bottom]). */
private fun ceilBlock(x: Float, w: Float, bottom: Float) = Obstacle(K_BLOCK, x, 0f, w, bottom)

/** Marque des objets écrits comme du terrain normal mais joués en miroir. */
private fun inZone(vararg os: Obstacle): List<Obstacle> {
    for (o in os) o.zone = true
    return os.toList()
}

// ---------------------------------------------------------------------------
// Patterns : les « briques » assemblées par le générateur
// ---------------------------------------------------------------------------

const val P_SPIKE1 = 0
const val P_SPIKE2 = 1
const val P_SPIKE3 = 2
const val P_RHYTHM = 3
const val P_BLOCK = 4
const val P_BLOCK_SPIKE = 5
const val P_STAIRS = 6
const val P_PIT = 7
const val P_PIT_SPIKE = 8
const val P_PAD = 9
const val P_PAD_RAIL = 10
const val P_FORK_GROUND = 11 // danger au sol, rail libre + pièces
const val P_FORK_RAIL = 12 // danger sur le rail, sol libre
const val P_ORB_SPIKES = 13
const val P_ORB_PIT = 14
const val P_TUNNEL = 15
const val P_FLIP = 16
const val P_SHIP = 17
const val NUM_PATTERNS = 18

internal class GenCtx(
    val rng: Random,
    val jumpDist: Float, // distance parcourue pendant un saut normal
    val preferRail: Boolean, // le joueur est prédit sur la voie du haut
)

internal class Pattern(
    var id: Int,
    var objs: List<Obstacle>, // x relatif au début du pattern
    val length: Float,
    val tail: Float = 0f, // espace de sécurité supplémentaire après le pattern
)

internal class PatDef(val minTier: Int, val weight: Float, val build: (GenCtx) -> Pattern)

internal val AIR_TIME = 2 * JUMP_V / GRAVITY

internal val patDefs: List<PatDef> = listOf(
    /* P_SPIKE1 */ PatDef(0, 3f) { Pattern(0, listOf(spikeG(0f)), 40f) },
    /* P_SPIKE2 */ PatDef(0, 3f) { Pattern(0, listOf(spikeG(0f), spikeG(40f)), 80f) },
    /* P_SPIKE3 */ PatDef(1, 2.5f) { Pattern(0, listOf(spikeG(0f), spikeG(40f), spikeG(80f)), 120f) },
    /* P_RHYTHM */ PatDef(2, 2f) { c ->
        val gap = c.jumpDist * 0.95f + 20 + c.rng.nextFloat() * 60
        Pattern(0, listOf(spikeG(0f), spikeG(40 + gap)), 80 + gap)
    },
    /* P_BLOCK */ PatDef(0, 3f) { Pattern(0, listOf(blockG(0f, 40f, 40f)), 40f) },
    /* P_BLOCK_SPIKE */ PatDef(2, 2f) {
        Pattern(0, listOf(blockG(0f, 80f, 80f), spikeG(80f + 50), spikeG(80f + 90)), 210f)
    },
    /* P_STAIRS */ PatDef(1, 2.5f) {
        Pattern(0, listOf(blockG(0f, 80f, 40f), blockG(140f, 80f, 80f)), 220f)
    },
    /* P_PIT */ PatDef(0, 2f) { c ->
        val w = clampF(c.jumpDist * (0.45f + c.rng.nextFloat() * 0.15f), 110f, 230f)
        Pattern(0, listOf(pitAt(0f, w)), w)
    },
    /* P_PIT_SPIKE */ PatDef(2, 1.5f) { c ->
        val w = clampF(c.jumpDist * 0.4f, 100f, 190f)
        Pattern(0, listOf(pitAt(0f, w), spikeG(w + c.jumpDist * 0.45f)), w + c.jumpDist * 0.45f + 40)
    },
    /* P_PAD */ PatDef(0, 1.5f) { c -> Pattern(0, listOf(padG(0f)), 40f, tail = c.jumpDist * 1.5f) },
    /* P_PAD_RAIL */ PatDef(1, 2f) {
        Pattern(
            0,
            listOf(
                padG(0f), railAt(150f, 480f),
                coinAt(260f, RAIL_Y - 50), coinAt(330f, RAIL_Y - 50), coinAt(400f, RAIL_Y - 50),
                spikeAt(520f, RAIL_Y),
            ),
            630f, tail = 120f,
        )
    },
    /* P_FORK_GROUND */ PatDef(1, 2f) {
        Pattern(
            0,
            listOf(
                railAt(0f, 520f),
                spikeG(170f), spikeG(210f), spikeG(250f),
                coinAt(110f, RAIL_Y - 50), coinAt(190f, RAIL_Y - 50),
                coinAt(270f, RAIL_Y - 50), coinAt(350f, RAIL_Y - 50),
            ),
            520f, tail = 60f,
        )
    },
    /* P_FORK_RAIL */ PatDef(2, 2f) {
        Pattern(
            0,
            listOf(
                railAt(0f, 600f),
                spikeAt(190f, RAIL_Y), spikeAt(390f, RAIL_Y),
                coinAt(120f, RAIL_Y - 50), coinAt(290f, RAIL_Y - 90), coinAt(480f, RAIL_Y - 50),
            ),
            600f, tail = 60f,
        )
    },
    /* P_ORB_SPIKES */ PatDef(1, 2f) {
        Pattern(
            0,
            listOf(
                spikeG(0f), spikeG(40f), spikeG(80f), spikeG(120f), spikeG(160f), spikeG(200f), spikeG(240f),
                orbAt(95f, GROUND_Y - 125),
            ),
            280f, tail = 60f,
        )
    },
    /* P_ORB_PIT */ PatDef(2, 1.5f) { c ->
        val w = clampF(c.jumpDist * 1.25f, 260f, 380f)
        Pattern(0, listOf(pitAt(0f, w), orbAt(w * 0.38f, GROUND_Y - 120)), w, tail = 40f)
    },
    /* P_TUNNEL */ PatDef(2, 1.5f) {
        Pattern(0, listOf(ceilBlock(0f, 360f, 320f), spikeG(130f), spikeG(170f)), 360f, tail = 40f)
    },
    /* P_FLIP */ PatDef(1, 1.5f) { c ->
        Pattern(
            0,
            listOf(portalAt(0f, MODE_FLIP), portalAt(1000f, MODE_NORMAL)) +
                inZone(spikeG(330f), spikeG(370f), blockG(560f, 40f, 40f), spikeG(740f), spikeG(780f), spikeG(820f)),
            1024f, tail = c.jumpDist * 1.6f,
        )
    },
    /* P_SHIP */ PatDef(2, 1.2f) { c ->
        Pattern(
            0,
            listOf(
                portalAt(0f, MODE_SHIP),
                blockG(500f, 80f, 150f),
                ceilBlock(900f, 80f, 310f),
                blockG(1300f, 80f, 170f),
                ceilBlock(1650f, 80f, 300f),
                portalAt(2000f, MODE_NORMAL),
            ),
            2024f, tail = c.jumpDist * 1.6f,
        )
    },
)

// ---------------------------------------------------------------------------
// Directeur : difficulté dynamique + prédiction du comportement du joueur
// ---------------------------------------------------------------------------

/** Survit d'une partie à l'autre pour que le jeu « retienne » le niveau du joueur. */
class Director(
    var skill: Float = 0.15f, // 0..1, niveau estimé du joueur
    var railShare: Float = 0.3f, // part du temps passée sur la voie du haut (moyenne glissante)
) {
    internal val weights = FloatArray(NUM_PATTERNS) { 1f }

    fun observeLane(onRail: Boolean, dt: Float) {
        val target = if (onRail) 1f else 0f
        railShare += (target - railShare) * clampF(dt * 0.15f, 0f, 1f)
    }

    fun passed(id: Int) {
        skill = clampF(skill + 0.03f * (1 - skill * 0.5f), 0.05f, 1f)
        weights[id] = clampF(weights[id] * 1.1f, 0.3f, 1f)
    }

    fun died(id: Int, hadChunk: Boolean) {
        skill = clampF(skill - 0.12f, 0.05f, 1f)
        if (hadChunk) {
            // le pattern fatal devient moins probable (sans disparaître)
            weights[id] = clampF(weights[id] * 0.7f, 0.3f, 1f)
        }
    }

    private fun tier(): Int = minOf((skill * 3.2f).toInt(), 2)

    /** Vitesse de défilement (px/s) pour une distance donnée. */
    fun targetSpeed(dist: Float): Float = 340 + 240 * skill + clampF(dist / 180, 0f, 110f)

    internal fun pick(c: GenCtx, exclude: Int): Int {
        val tier = tier()
        var total = 0f
        val w = FloatArray(NUM_PATTERNS)
        for (i in patDefs.indices) {
            val def = patDefs[i]
            if (def.minTier > tier || i == exclude) continue
            w[i] = def.weight * weights[i]
            // Prédiction : on place le danger sur la voie que le joueur préfère.
            if (i == P_FORK_RAIL && c.preferRail) w[i] *= 2.5f
            if (i == P_FORK_GROUND && !c.preferRail) w[i] *= 2.5f
            if (i == P_FORK_RAIL && !c.preferRail) w[i] *= 0.4f
            if (i == P_FORK_GROUND && c.preferRail) w[i] *= 0.4f
            total += w[i]
        }
        var r = c.rng.nextFloat() * total
        for (i in w.indices) {
            if (r < w[i]) return i
            r -= w[i]
        }
        return P_SPIKE1
    }
}

// ---------------------------------------------------------------------------
// Générateur de niveau
// ---------------------------------------------------------------------------

class Chunk(val id: Int, val x0: Float, val x1: Float, var passed: Boolean = false)

class Level(private val rng: Random = Random.Default) {
    val objs = ArrayList<Obstacle>()
    val chunks = ArrayList<Chunk>()
    private var nextX = 0f
    private var last = -1

    init {
        reset()
    }

    fun reset() {
        objs.clear()
        chunks.clear()
        nextX = PLAYER_X + 1000 // zone d'échauffement
        last = -1
    }

    /** Génère du niveau jusqu'à ce que l'écran soit couvert, et nettoie ce qui est sorti. */
    fun ensure(dist: Float, keepFrom: Float, speed: Float, d: Director) {
        while (nextX < dist + SCREEN_W + 500) spawn(speed, d)
        if (objs.size > 64) {
            objs.removeAll { it.x + it.w <= keepFrom - 100 }
            chunks.removeAll { it.x1 <= keepFrom - 300 }
        }
    }

    private fun spawn(speed: Float, d: Director) {
        val ctx = GenCtx(rng, speed * AIR_TIME, d.railShare > 0.45f)

        // On tire un pattern puis on VÉRIFIE qu'il est franchissable par recherche de chemin.
        var pat: Pattern? = null
        for (attempt in 0 until 8) {
            val id = d.pick(ctx, last)
            val candidate = patDefs[id].build(ctx)
            candidate.id = id
            val abs = candidate.objs.map { it.copyAt(it.x + nextX) }
            if (solvable(abs, speed, nextX, nextX + candidate.length)) {
                candidate.objs = abs
                pat = candidate
                break
            }
        }
        val chosen = pat ?: patDefs[P_SPIKE1].build(ctx).also {
            it.id = P_SPIKE1
            it.objs = it.objs.map { o -> o.copyAt(o.x + nextX) }
        }

        objs.addAll(chosen.objs)
        chunks.add(Chunk(chosen.id, nextX, nextX + chosen.length))
        last = chosen.id

        // Espace de récupération : plus la difficulté est haute, plus il est court.
        var gap = speed * (0.85f - 0.3f * d.skill + ctx.rng.nextFloat() * 0.25f)
        if (gap < speed * 0.55f) gap = speed * 0.55f
        nextX += chosen.length + chosen.tail + gap
    }

    /** Pattern dans lequel (ou juste avant lequel) se trouve [x]. */
    fun chunkAt(x: Float): Int? {
        for (i in chunks.indices.reversed()) if (chunks[i].x0 - 120 <= x) return chunks[i].id
        return null
    }
}

// ---------------------------------------------------------------------------
// Recherche de chemin : le pattern est-il franchissable ?
// ---------------------------------------------------------------------------

private data class SimKey(
    val y: Int, val vy: Int, val grounded: Boolean, val drop: Boolean, val mode: Int, val orb: Int,
)

private fun keyOf(p: PState) =
    SimKey((p.y / 5).toInt(), (p.vy / 40).toInt(), p.grounded, p.dropT > 0, p.mode, p.lastOrb.toInt())

/** Un joueur arrivant au sol avant le pattern peut-il le franchir ? */
internal fun solvable(objs: List<Obstacle>, speed: Float, from: Float, to: Float): Boolean {
    val start = from - PLAYER_X - speed * 0.8f
    val end = to - PLAYER_X + 60
    return search(PState(), start, end, objs, speed)
}

private class Node(val p: PState, val dist: Float)

private val ACT_NONE = Input()
private val ACT_JUMP = Input(jump = true, press = true)
private val ACT_LANE = Input(lane = true, jump = true, press = true)
private val ACT_DROP = Input(drop = true)
private val ACTS_ALL = listOf(ACT_NONE, ACT_JUMP, ACT_LANE, ACT_DROP)
private val ACTS_SHIP = listOf(ACT_NONE, ACT_JUMP)
private val ACTS_AIR = listOf(ACT_NONE, ACT_JUMP, ACT_DROP)

/**
 * Explore (parcours en largeur, avec fusion des états équivalents) toutes les séquences d'actions
 * possibles à partir de l'état [p0], avec EXACTEMENT la même physique que le jeu. Vrai si au moins
 * un chemin survit jusqu'à la distance [end].
 */
fun search(p0: PState, start: Float, end: Float, objs: List<Obstacle>, speed: Float): Boolean {
    val decide = 1f / 20f // une décision toutes les 50 ms
    val sub = (decide / STEP).toInt()
    val ticks = ceil((end - start) / (speed * decide)).toInt()

    var frontier = HashMap<SimKey, Node>()
    frontier[keyOf(p0)] = Node(p0.copy(), start)

    for (t in 0 until ticks) {
        val next = HashMap<SimKey, Node>(frontier.size * 2)
        for (n in frontier.values) {
            val acts = when {
                n.p.mode == MODE_SHIP -> ACTS_SHIP
                n.p.grounded -> ACTS_ALL
                else -> ACTS_AIR
            }
            for (act in acts) {
                val p = n.p.copy()
                var dist = n.dist
                for (s in 0 until sub) {
                    dist += speed * STEP
                    val input = when {
                        s == 0 -> act
                        p.mode == MODE_SHIP -> Input(act.jump, act.lane, act.drop, press = false)
                        else -> ACT_NONE
                    }
                    stepPlayer(p, objs, dist, STEP, input)
                    if (p.dead) break
                }
                if (p.dead) continue
                val k = keyOf(p)
                if (k !in next && next.size < 120) next[k] = Node(p, dist)
            }
        }
        if (next.isEmpty()) return false
        frontier = next
    }
    return true
}
