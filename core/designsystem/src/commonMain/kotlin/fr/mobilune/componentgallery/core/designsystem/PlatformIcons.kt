package fr.mobilune.componentgallery.core.designsystem

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/** `contentDescription = null` quand l'icône est décorative (le bouton qui l'entoure porte déjà le libellé). */
@Composable
expect fun BackIcon(modifier: Modifier, contentDescription: String? = "Retour")

@Composable
expect fun MoreIcon(modifier: Modifier, contentDescription: String? = "Plus d'options")
