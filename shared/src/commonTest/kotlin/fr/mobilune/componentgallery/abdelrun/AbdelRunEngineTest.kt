package fr.mobilune.componentgallery.abdelrun

import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AbdelRunEngineTest {
    // --- Port des tests du jeu d'origine (level_test.go) ---

    @Test
    fun `Chaque pattern est franchissable aux vitesses normales du jeu`() {
        val rng = Random(42)
        for (speed in listOf(340f, 450f, 600f, 750f)) {
            for ((id, def) in patDefs.withIndex()) {
                for (rail in listOf(false, true)) {
                    val pattern = def.build(GenCtx(rng, speed * AIR_TIME, rail))
                    val abs = pattern.objs.map { it.copyAt(it.x + 2000f) }
                    val ok = solvable(abs, speed, 2000f, 2000f + pattern.length)
                    // Le vaisseau est parfois rejeté à haute vitesse : le générateur retente.
                    if (!ok && !(id == P_SHIP && speed >= 600f)) {
                        throw AssertionError("pattern $id insoluble à $speed px/s (rail=$rail)")
                    }
                }
            }
        }
    }

    @Test
    fun `Le validateur rejette l'impossible`() {
        assertFalse(solvable(listOf(pitAt(2000f, 600f)), 400f, 2000f, 2600f), "fosse de 600 px")
        assertFalse(solvable(listOf(blockG(2000f, 40f, 260f)), 400f, 2000f, 2040f), "mur de 260 px")
    }

    @Test
    fun `Le générateur produit un niveau long sans planter`() {
        val director = Director(skill = 1f)
        val level = Level(Random(7))
        var dist = 0f
        while (dist < 60_000f) {
            level.ensure(dist, dist, director.targetSpeed(dist), director)
            dist += 20f
        }
        assertTrue(level.chunks.isNotEmpty())
        // Le nettoyage garde la liste bornée.
        assertTrue(level.objs.size < 500, "objets en mémoire : ${level.objs.size}")
    }

    // --- Moteur ---

    private fun runUntil(engine: GameEngine, controls: GameControls, maxSeconds: Float, stop: () -> Boolean) {
        var t = 0f
        while (t < maxSeconds && !stop()) {
            engine.update(1f / 60f, controls)
            t += 1f / 60f
        }
    }

    @Test
    fun `Sans rien toucher le cube finit par mourir et la partie est comptée`() {
        val engine = GameEngine(random = Random(1))
        var saved = 0
        engine.onSaveChanged = { saved++ }
        runUntil(engine, GameControls(), 30f) { engine.state == RunState.Dead }
        assertEquals(RunState.Dead, engine.state)
        assertEquals(1, engine.save.deaths)
        assertEquals(1, engine.save.runs)
        assertEquals(1, saved)
        assertTrue(engine.scoreInt() > 0)
    }

    @Test
    fun `Le score augmente avec la distance parcourue`() {
        val engine = GameEngine(random = Random(1))
        runUntil(engine, GameControls(), 2f) { false }
        assertTrue(engine.dist > 0f)
        assertTrue(engine.scoreInt() > 0)
    }

    @Test
    fun `Un saut décolle le cube du sol`() {
        val engine = GameEngine(random = Random(1))
        val controls = GameControls()
        engine.update(1f / 60f, controls)
        assertTrue(engine.p.grounded)
        controls.jumpPressed = true
        controls.jumpHeld = true
        engine.update(1f / 60f, controls)
        assertFalse(engine.p.grounded)
        assertTrue(engine.p.y < GROUND_Y - PLAYER_SIZE)
    }

    @Test
    fun `Abandonner enregistre la partie sans compter de mort`() {
        val engine = GameEngine(random = Random(1))
        runUntil(engine, GameControls(), 1f) { false }
        engine.abandon()
        assertEquals(1, engine.save.runs)
        assertEquals(0, engine.save.deaths)
    }

    @Test
    fun `Le pouvoir du temps accélère le jeu et vide la jauge`() {
        val engine = GameEngine(random = Random(1))
        val controls = GameControls().also { it.boostHeld = true }
        engine.update(1f / 60f, controls)
        assertTrue(engine.boosting)
        runUntil(engine, controls, 0.5f) { false }
        assertTrue(engine.energy < 1f, "la jauge se vide pendant le pouvoir")
        controls.boostHeld = false
        engine.update(1f / 60f, controls)
        assertFalse(engine.boosting)
    }

    @Test
    fun `Pause fige la simulation`() {
        val engine = GameEngine(random = Random(1))
        engine.paused = true
        engine.update(1f, GameControls())
        assertEquals(0f, engine.dist)
    }

    // --- Sauvegarde ---

    @Test
    fun `La sauvegarde survit à un aller-retour`() {
        val copy = AbdelRunSave.decode(AbdelRunSave(best = 1234, runs = 9, deaths = 4).encode())
        assertEquals(1234, copy.best)
        assertEquals(9, copy.runs)
        assertEquals(4, copy.deaths)
    }

    @Test
    fun `Une sauvegarde absente ou illisible donne une sauvegarde neuve`() {
        assertEquals(0, AbdelRunSave.decode(null).best)
        assertEquals(0, AbdelRunSave.decode("n'importe quoi\n???").best)
    }

    @Test
    fun `Une ancienne sauvegarde avec des clés inconnues reste lisible`() {
        val old = "coins 55\nbest 900\nskin 3\nunlock1 1\nkill4 2\ndeaths 7\n"
        val save = AbdelRunSave.decode(old)
        assertEquals(900, save.best)
        assertEquals(7, save.deaths)
    }
}
