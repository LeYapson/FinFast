package fr.mobilune.componentgallery.core.designsystem

import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import fr.mobilune.componentgallery.core.designsystem.resources.Res
import fr.mobilune.componentgallery.core.designsystem.resources.arrow_left_android
import fr.mobilune.componentgallery.core.designsystem.resources.more_android
import org.jetbrains.compose.resources.painterResource

@Composable
actual fun BackIcon(modifier: Modifier, contentDescription: String?) {
    Image(
        painter = painterResource(Res.drawable.arrow_left_android),
        contentDescription = contentDescription,
        modifier = modifier,
        contentScale = ContentScale.Fit,
    )
}

@Composable
actual fun MoreIcon(modifier: Modifier, contentDescription: String?) {
    Image(
        painter = painterResource(Res.drawable.more_android),
        contentDescription = contentDescription,
        modifier = modifier,
        contentScale = ContentScale.Fit,
    )
}
