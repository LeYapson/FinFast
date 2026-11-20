package fr.mobilune.componentgallery.core.designsystem.components

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.animation.core.tween

private val AccentColor = Color(0xFF135E45)

@Composable
fun ExpandableButton(
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }
    val backgroundColor by animateColorAsState(
        targetValue = if (expanded) AccentColor else Color.White,
        animationSpec = tween(durationMillis = 300),
        label = "buttonBackground",
    )
    val textColor by animateColorAsState(
        targetValue = if (expanded) Color.White else Color.Black,
        animationSpec = tween(durationMillis = 300),
        label = "buttonText",
    )

    Box(
        modifier = modifier
            .animateContentSize(animationSpec = tween(durationMillis = 300))
            .background(
                color = backgroundColor,
                shape = RoundedCornerShape(32.dp),
            )
            .clickable { expanded = !expanded }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = if (expanded) "Home" else "H",
            color = textColor,
            fontWeight = FontWeight.Bold,
        )
    }
}
