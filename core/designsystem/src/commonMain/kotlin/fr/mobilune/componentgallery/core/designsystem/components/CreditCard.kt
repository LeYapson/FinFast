package fr.mobilune.componentgallery.core.designsystem.components

import androidx.compose.foundation.background
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import fr.mobilune.componentgallery.core.designsystem.ThemeConstants
import fr.mobilune.componentgallery.core.designsystem.resources.Res
import fr.mobilune.componentgallery.core.designsystem.resources.visa
import org.jetbrains.compose.resources.painterResource

private val CardBackground: Color get() = ThemeConstants.onClassicBackground
private val CardText: Color get() = ThemeConstants.classicBackground

/** [semanticsDescription] : libellé lu par TalkBack à la place du contenu de la carte (nom, Visa, date). */
@Composable
fun CreditCard(
    modifier: Modifier = Modifier,
    name: String = "MARCEL PAGNOL",
    date: String = "08/28",
    semanticsDescription: String? = null,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(1.586f)
            .shadow(4.dp, RoundedCornerShape(24.dp))
            .background(CardBackground, RoundedCornerShape(24.dp))
            .padding(16.dp)
            .then(
                if (semanticsDescription != null) {
                    Modifier.clearAndSetSemantics { contentDescription = semanticsDescription }
                } else {
                    Modifier
                },
            ),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = name,
            color = CardText,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.align(Alignment.Bottom),
        )
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight(),
            horizontalAlignment = Alignment.End,
        ) {
            Image(
                painter = painterResource(Res.drawable.visa),
                contentDescription = "Visa",
                modifier = Modifier.size(width = 56.dp, height = 31.dp),
                contentScale = ContentScale.Fit,
            )
            Spacer(modifier = Modifier.weight(1f))
            Text(
                text = date,
                color = CardText,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}
