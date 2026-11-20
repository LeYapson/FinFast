package fr.mobilune.componentgallery.components.flag
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * Prompt (Haiku):
 * Je travaille sur Compose Multiplatform. Je souhaite réaliser un Drapeau français,
 * et je maitrise les Column, Row et Box. Propose moi une interface compréhensible avec ces notions.
 */


@Composable
fun DrapeauFrancaisHaiku(modifier: Modifier = Modifier) {
    // Box : le cadre extérieur qui fixe la taille du drapeau (ratio 3:2)
    Box(
        modifier = modifier.size(width = 300.dp, height = 200.dp)
    ) {
        // Row : place les trois bandes côte à côte, de gauche à droite
        Row(modifier = Modifier.fillMaxSize()) {
            // Chaque bande prend une part égale de la largeur
            Bande(couleur = Color(0xFF002395), modifier = Modifier.weight(1f)) // Bleu
            Bande(couleur = Color.White, modifier = Modifier.weight(1f))       // Blanc
            Bande(couleur = Color(0xFFED2939), modifier = Modifier.weight(1f)) // Rouge
        }
    }
}

// Une bande est simplement une Box pleine hauteur avec une couleur de fond
@Composable
private fun Bande(couleur: Color, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxHeight()
            .background(couleur)
    )
}