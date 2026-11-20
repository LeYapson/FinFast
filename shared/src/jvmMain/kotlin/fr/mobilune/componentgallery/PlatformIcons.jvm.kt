package fr.mobilune.componentgallery

import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import componentgallery.shared.generated.resources.Res
import componentgallery.shared.generated.resources.arrow_left
import componentgallery.shared.generated.resources.more
import org.jetbrains.compose.resources.painterResource

@Composable
actual fun BackIcon(modifier: Modifier, contentDescription: String?) {
    Image(
        painter = painterResource(Res.drawable.arrow_left),
        contentDescription = contentDescription,
        modifier = modifier,
        contentScale = ContentScale.Fit,
    )
}

@Composable
actual fun MoreIcon(modifier: Modifier, contentDescription: String?) {
    Image(
        painter = painterResource(Res.drawable.more),
        contentDescription = contentDescription,
        modifier = modifier,
        contentScale = ContentScale.Fit,
    )
}
