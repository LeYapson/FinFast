package fr.mobilune.componentgallery.abdelrun

/*
 * Moteur de jeu d'AbdelRun (port Kotlin du jeu Go/raylib « Abdel_Run ») : physique du cube.
 * Les coordonnées sont celles de la scène d'origine (1024 x 600) ; l'écran les met à l'échelle.
 */

// Constantes de la scène (pixels, secondes).
const val SCREEN_W = 1024f
const val SCREEN_H = 600f

const val GROUND_Y = 520f // surface du sol (voie du bas)
const val RAIL_Y = 340f // surface des rails (voie du haut)
const val RAIL_H = 16f

const val PLAYER_X = 200f // position horizontale fixe du cube à l'écran
const val PLAYER_SIZE = 40f

const val GRAVITY = 2300f
const val JUMP_V = 720f // saut normal
const val LANE_JUMP_V = 1010f // saut haut pour rejoindre la voie du haut
const val PAD_V = 1050f // propulsion d'un jump pad
const val MAX_FALL = 1400f

/** Pas de simulation fixe : le jeu est identique quel que soit le FPS. */
const val STEP = 1f / 120f

const val CEIL_Y = 80f // plafond (mode vaisseau / miroir du sol)
const val SHIP_MAX_V = 520f

// Types d'éléments du niveau.
const val K_SPIKE = 0
const val K_BLOCK = 1
const val K_RAIL = 2
const val K_PIT = 3
const val K_PAD = 4
const val K_COIN = 5
const val K_ORB = 6
const val K_PORTAL = 7

// Modes de jeu du cube.
const val MODE_NORMAL = 0
const val MODE_FLIP = 1 // gravité inversée : physique calculée en miroir
const val MODE_SHIP = 2 // vaisseau : on maintient le saut pour monter

/**
 * Élément du niveau. (x, y) est le coin haut-gauche en coordonnées monde ; x défile vers la
 * gauche quand la distance augmente.
 */
class Obstacle(
    val kind: Int,
    var x: Float,
    val y: Float,
    val w: Float,
    val h: Float,
    var taken: Boolean = false, // pièce ramassée
    val arg: Int = 0, // portail : mode cible
    var zone: Boolean = false, // objet de zone miroir (mode gravité inversée)
) {
    fun copyAt(newX: Float) = Obstacle(kind, newX, y, w, h, taken, arg, zone)
}

/** État physique du cube. Copiable : le validateur de niveau simule des futurs possibles. */
data class PState(
    var y: Float = GROUND_Y - PLAYER_SIZE, // haut du cube
    var vy: Float = 0f,
    var grounded: Boolean = true,
    var dropT: Float = 0f, // temps restant pendant lequel les rails sont traversés
    var dead: Boolean = false,
    var mode: Int = MODE_NORMAL,
    var lastOrb: Float = 0f, // x de la dernière orbe utilisée
    var usedPress: Boolean = false, // l'appui a été consommé ce pas (saut, orbe)
) {
    /** Position écran (haut du cube) quel que soit le mode. */
    fun screenY(): Float = if (mode == MODE_FLIP) SCREEN_H - y - PLAYER_SIZE else y
}

class Input(
    val jump: Boolean = false, // saut normal
    val lane: Boolean = false, // saut haut (changement de voie)
    val drop: Boolean = false, // descendre / traverser le rail
    val press: Boolean = false, // appui frais (orbes)
)

internal fun overlap(
    ax: Float, ay: Float, aw: Float, ah: Float,
    bx: Float, by: Float, bw: Float, bh: Float,
): Boolean = ax < bx + bw && ax + aw > bx && ay < by + bh && ay + ah > by

internal fun clampF(v: Float, lo: Float, hi: Float): Float = if (v < lo) lo else if (v > hi) hi else v

private fun inPit(objs: List<Obstacle>, cx: Float): Boolean {
    for (o in objs) if (o.kind == K_PIT && cx >= o.x && cx < o.x + o.w) return true
    return false
}

/** Un objet participe-t-il à la physique dans le mode courant ? */
private fun active(o: Obstacle, mode: Int): Boolean {
    if (o.kind == K_PORTAL) return true
    return if (mode == MODE_FLIP) o.zone else !o.zone
}

/**
 * Avance la physique d'un pas [dt]. [dist] est la distance parcourue : le bord gauche du cube est
 * donc à dist + PLAYER_X en coordonnées monde. En mode gravité inversée, p.y est exprimé dans un
 * repère miroir : les objets de « zone » sont écrits comme du terrain normal et affichés en miroir.
 */
fun stepPlayer(p: PState, objs: List<Obstacle>, dist: Float, dt: Float, input: Input) {
    if (p.dead) return
    val px = dist + PLAYER_X
    p.usedPress = false

    // --- Portails ---
    for (o in objs) {
        if (o.kind != K_PORTAL || o.arg == p.mode || o.x > px + PLAYER_SIZE || o.x + o.w < px) continue
        if (p.mode == MODE_FLIP || o.arg == MODE_FLIP) {
            p.y = SCREEN_H - p.y - PLAYER_SIZE // changement de repère
            p.vy = -p.vy
        }
        p.mode = o.arg
        p.grounded = false
    }

    if (p.mode == MODE_SHIP) {
        stepShip(p, objs, px, dt, input)
        return
    }

    if (input.drop) {
        p.dropT = 0.25f
        if (p.grounded && p.y + PLAYER_SIZE < GROUND_Y - 1) {
            p.grounded = false
        } else if (!p.grounded && p.vy < 600f) {
            p.vy = 600f // chute rapide
        }
    }
    if (p.grounded) {
        if (input.lane && p.mode == MODE_NORMAL) {
            p.vy = -LANE_JUMP_V
            p.grounded = false
            p.usedPress = input.press
        } else if (input.jump) {
            p.vy = -JUMP_V
            p.grounded = false
            p.usedPress = input.press
        }
    } else if (input.press) {
        // orbe : saut en l'air
        for (o in objs) {
            if (o.kind != K_ORB || !active(o, p.mode) || o.x == p.lastOrb) continue
            if (overlap(px, p.y, PLAYER_SIZE, PLAYER_SIZE, o.x - 6, o.y - 6, o.w + 12, o.h + 12)) {
                p.vy = -JUMP_V
                p.lastOrb = o.x
                p.usedPress = true
                break
            }
        }
    }

    val prevBottom = p.y + PLAYER_SIZE
    p.vy += GRAVITY * dt
    if (p.vy > MAX_FALL) p.vy = MAX_FALL
    p.y += p.vy * dt
    if (p.dropT > 0) p.dropT -= dt
    val bottom = p.y + PLAYER_SIZE
    p.grounded = false

    // --- Atterrissage sur une surface (sol, bloc, rail) ---
    if (p.vy >= 0) {
        val eps = 6f
        var support = 1e9f
        val cx = px + PLAYER_SIZE / 2
        if (!inPit(objs, cx) && prevBottom <= GROUND_Y + eps && bottom >= GROUND_Y) {
            support = GROUND_Y
        }
        for (o in objs) {
            if ((o.kind != K_BLOCK && o.kind != K_RAIL) || !active(o, p.mode) ||
                o.x > px + PLAYER_SIZE || o.x + o.w < px
            ) continue
            if (px + PLAYER_SIZE - 4 <= o.x || px + 4 >= o.x + o.w) continue
            if (o.kind == K_RAIL && p.dropT > 0) continue
            val tol = if (o.kind == K_RAIL) 2f else eps
            if (prevBottom <= o.y + tol && bottom >= o.y && o.y < support) {
                support = o.y
            }
        }
        if (support < 1e9f) {
            p.y = support - PLAYER_SIZE
            p.vy = 0f
            p.grounded = true
        }
    }

    collide(p, objs, px)
    if (p.y > SCREEN_H) p.dead = true
}

/** Objets mortels ou à effet (pics, blocs, pads). */
private fun collide(p: PState, objs: List<Obstacle>, px: Float) {
    val hx = px + 3
    val hy = p.y + 3
    val hs = PLAYER_SIZE - 6
    for (o in objs) {
        if (o.x > px + PLAYER_SIZE + 50 || o.x + o.w < px - 50 || !active(o, p.mode)) continue
        when (o.kind) {
            K_SPIKE -> {
                // hitbox du pic : un rectangle plus étroit que son sprite
                if (overlap(hx, hy, hs, hs, o.x + 11, o.y + 14, o.w - 22, o.h - 14)) {
                    p.dead = true
                    return
                }
            }
            K_BLOCK -> {
                if (overlap(hx, hy, hs, hs, o.x, o.y, o.w, o.h)) {
                    p.dead = true
                    return
                }
            }
            K_PAD -> {
                if (p.vy >= 0 && overlap(hx, hy, hs, hs, o.x, o.y, o.w, o.h)) {
                    p.vy = -PAD_V
                    p.grounded = false
                }
            }
        }
    }
}

/** Vaisseau : il monte tant qu'on maintient le saut, sinon il tombe. */
private fun stepShip(p: PState, objs: List<Obstacle>, px: Float, dt: Float, input: Input) {
    val acc = if (input.jump || input.lane) -1700f else 1500f
    p.vy += acc * dt
    p.vy = clampF(p.vy, -SHIP_MAX_V, SHIP_MAX_V)
    p.y += p.vy * dt
    if (p.y < CEIL_Y) {
        p.y = CEIL_Y
        p.vy = 0f
    }
    if (p.y > GROUND_Y - PLAYER_SIZE) {
        p.y = GROUND_Y - PLAYER_SIZE
        p.vy = 0f
    }
    p.grounded = false
    collide(p, objs, px)
}
