package fr.mobilune.componentgallery.core.designsystem.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
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

private val CardText = Color(0xFF000000)

@Composable
fun StableAmountCard(
    modifier: Modifier = Modifier,
    amount: String = "1474€",
    status: String = "Stable",
    backgroundColor: Color = ThemeConstants.incomeBackground,
    statusColor: Color = ThemeConstants.statusIndicator,
) {
    Column(
        modifier = modifier
            .heightIn(min = 170.dp)
            .background(backgroundColor, RoundedCornerShape(24.dp))
            .padding(16.dp)
            .animateContentSize()
            // « Dépenses, −20000,00 € » lu d'un bloc ; le disque de couleur est décoratif.
            .semantics(mergeDescendants = true) {},
    ) {
        Box(
            modifier = Modifier
                .size(42.dp)
                .background(statusColor, CircleShape),
        )
        Spacer(modifier = Modifier.weight(1f))
        AnimatedContent(
            targetState = amount,
            transitionSpec = { fadeIn(tween(220)) togetherWith fadeOut(tween(160)) },
            label = "stable_amount",
        ) { displayedAmount ->
            Text(
                text = displayedAmount,
                color = CardText,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
            )
        }
        Text(
            text = status,
            color = CardText,
            fontSize = 14.sp,
            fontWeight = FontWeight.Light,
        )
    }
}
