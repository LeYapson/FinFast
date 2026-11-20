package fr.mobilune.componentgallery.components

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import fr.mobilune.componentgallery.ThemeConstants
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val CardBackground: Color get() = ThemeConstants.accentBackground
private val CardText: Color get() = ThemeConstants.onAccentBackground

@Composable
fun BudgetCard(
    modifier: Modifier = Modifier,
    budget: String = "Budget de Octobre",
    value: String = "12% de plus que Septembre",
    price: String = "$478",
) {

    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(CardBackground, RoundedCornerShape(24.dp))
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .animateContentSize()
            .semantics(mergeDescendants = true) {},
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(
            modifier = Modifier.weight(1f),
        ) {
            Text(
                text = budget,
                color = CardText,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = value,
                color = CardText,
                fontSize = 14.sp,
                fontWeight = FontWeight.Light,
            )
        }
        Text(
            text = price,
            color = CardText,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
        )
    }
}
