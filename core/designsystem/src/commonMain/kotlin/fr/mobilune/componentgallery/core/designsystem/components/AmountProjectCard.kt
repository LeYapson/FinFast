package fr.mobilune.componentgallery.core.designsystem.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.layout.heightIn
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import fr.mobilune.componentgallery.core.designsystem.ThemeConstants
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val CardBackground: Color get() = ThemeConstants.accentBackground
private val CardText: Color get() = ThemeConstants.onAccentBackground

@Composable
fun AmountProjectCard(
    modifier: Modifier = Modifier,
    epargne: String = "Solde",
    amount: String = "872€",
    projectButton: String = "Voir les projets",
    onProjectsClick: (() -> Unit)? = null,
) {
    Column(
        modifier = modifier
            // Hauteur minimale plutôt que ratio fixe : la carte grandit avec la taille du texte.
            .heightIn(min = 190.dp)
            .background(CardBackground, RoundedCornerShape(24.dp))
            .then(
                if (onProjectsClick != null) {
                    Modifier
                        .clip(RoundedCornerShape(24.dp))
                        .clickable(role = Role.Button, onClickLabel = projectButton, onClick = onProjectsClick)
                } else {
                    Modifier
                },
            )
            .padding(16.dp)
            .animateContentSize()
            // Une seule étape TalkBack : « Solde du compte, −19990,00 €, Voir les projets ».
            .semantics(mergeDescendants = true) {},
    ) {
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = epargne,
            color = CardText,
            fontSize = 16.sp,
            fontWeight = FontWeight.Light,
        )
        Spacer(modifier = Modifier.height(16.dp))
        AnimatedContent(
            targetState = amount,
            transitionSpec = { fadeIn(tween(220)) togetherWith fadeOut(tween(160)) },
            label = "account_balance",
        ) { displayedAmount ->
            Text(
                text = displayedAmount,
                color = CardText,
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
            )
        }
        Spacer(modifier = Modifier.weight(1f))
        Text(
            text = projectButton,
            color = CardText,
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium,
        )
    }
}
