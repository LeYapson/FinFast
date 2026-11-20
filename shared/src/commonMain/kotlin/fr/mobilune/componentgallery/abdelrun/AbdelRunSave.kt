package fr.mobilune.componentgallery.abdelrun

import androidx.compose.ui.graphics.Color

/** Couleur du cube (le « Classic » du jeu d'origine). */
val PLAYER_COLOR = Color(60, 120, 255)

/** Volume de la musique, fixe. */
const val GAME_VOLUME = 0.6f

/** Ce qui persiste d'une session à l'autre : le record et le nombre de parties et de morts. */
class AbdelRunSave(
    var best: Int = 0,
    var runs: Int = 0,
    var deaths: Int = 0,
) {
    fun encode(): String = "best $best\nruns $runs\ndeaths $deaths\n"

    companion object {
        /** Lit une sauvegarde ; les clés inconnues (anciennes versions) sont ignorées. */
        fun decode(text: String?): AbdelRunSave {
            val s = AbdelRunSave()
            if (text.isNullOrBlank()) return s
            for (line in text.lines()) {
                val f = line.trim().split(' ', '\t').filter { it.isNotEmpty() }
                if (f.size < 2) continue
                val n = f[1].toDoubleOrNull()?.toInt() ?: continue
                when (f[0]) {
                    "best" -> s.best = n.coerceAtLeast(0)
                    "runs" -> s.runs = n.coerceAtLeast(0)
                    "deaths" -> s.deaths = n.coerceAtLeast(0)
                }
            }
            return s
        }
    }
}
