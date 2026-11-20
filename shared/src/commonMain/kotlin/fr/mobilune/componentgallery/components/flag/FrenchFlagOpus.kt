package fr.mobilune.componentgallery.components.flag

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// Couleurs officielles (charte graphique de l'État)
private val BleuFrance = Color(0xFF000091)
private val BlancFrance = Color(0xFFFFFFFF)
private val RougeFrance = Color(0xFFE1000F)

/** 👩‍💻 Prompt : Opus
 * Je travaille sur Compose Multiplatform. Je souhaite réaliser un Drapeau français, et je maitrise les Column, Row et Box. Propose moi une interface compréhensible avec ces notions.
 */
@Composable
fun FrenchFlagOpus(modifier: Modifier = Modifier) {
    // Row : place les trois bandes côte à côte
    Row(
        modifier = modifier
            .aspectRatio(3f / 2f)                 // proportion officielle 3:2
            .border(1.dp, Color.LightGray)        // pour voir la bande blanche sur fond blanc
    ) {
        // Box : chaque bande est un simple rectangle coloré
        Box(
            modifier = Modifier
                .weight(1f)        // chaque bande prend 1/3 de la largeur
                .fillMaxHeight()
                .background(BleuFrance)
        )
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .background(BlancFrance)
        )
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .background(RougeFrance)
        )
    }
}

/**
 * Pour aller plus loin, comme les trois Box sont identiques à la couleur près, vous pouvez factoriser avec une petite fonction :
 */
@Composable
private fun RowScope.Bande(couleur: Color) {
    Box(
        modifier = Modifier
            .weight(1f)
            .fillMaxHeight()
            .background(couleur)
    )
}