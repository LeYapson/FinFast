package fr.mobilune.componentgallery

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import fr.mobilune.componentgallery.data.AccessibilitySettings
import fr.mobilune.componentgallery.data.TEXT_SCALE_OPTIONS
import fr.mobilune.componentgallery.viewmodel.PushUiState

@Composable
internal fun OptionsScreen(
    settings: AccessibilitySettings,
    onHighContrastChange: (Boolean) -> Unit,
    onTextScaleChange: (Int) -> Unit,
    onReduceMotionChange: (Boolean) -> Unit,
    onHapticsChange: (Boolean) -> Unit,
    push: PushUiState,
    onEnableNotifications: () -> Unit,
    onSendTestPush: () -> Unit,
    onSendPushToUser: (userId: String, title: String, body: String) -> Unit,
    onRegisterDevice: () -> Unit,
    onCheckDevices: () -> Unit,
    onBack: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ThemeConstants.classicBackground)
            .screenTitle("Options")
            .safeContentPadding()
            .padding(horizontal = 12.dp, vertical = 12.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconAction(description = "Retour", onClick = onBack) { BackIcon(it, contentDescription = null) }
            Text(
                text = "Options",
                modifier = Modifier.weight(1f).heading(),
                textAlign = TextAlign.Center,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = ThemeConstants.onClassicBackground,
            )
            // Équilibre visuellement le bouton retour pour centrer le titre.
            Spacer(Modifier.width(48.dp))
        }
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
        ) {
            Spacer(Modifier.height(24.dp))
            SectionTitle("Affichage")
            SwitchRow(
                title = "Contraste élevé",
                description = "Noir sur blanc, bordures marquées et couleurs renforcées pour faciliter la lecture.",
                checked = settings.highContrast,
                onCheckedChange = onHighContrastChange,
            )
            Spacer(Modifier.height(12.dp))
            TextSizeChoice(selectedPercent = settings.textScalePercent, onSelect = onTextScaleChange)

            Spacer(Modifier.height(20.dp))
            SectionTitle("Mouvement et vibrations")
            SwitchRow(
                title = "Réduire les animations",
                description = "Supprime les animations de cartes, d'ondes NFC et les transitions entre écrans.",
                checked = settings.reduceMotion,
                onCheckedChange = onReduceMotionChange,
            )
            Spacer(Modifier.height(12.dp))
            SwitchRow(
                title = "Retours haptiques",
                description = "Vibrations lors des scans NFC, des validations et des erreurs.",
                checked = settings.hapticsEnabled,
                onCheckedChange = onHapticsChange,
            )

            Spacer(Modifier.height(20.dp))
            NotificationsSection(
                push = push,
                onEnableNotifications = onEnableNotifications,
                onSendTestPush = onSendTestPush,
                onSendPushToUser = onSendPushToUser,
                onRegisterDevice = onRegisterDevice,
                onCheckDevices = onCheckDevices,
            )

            Spacer(Modifier.height(20.dp))
            SectionTitle("Lecteur d'écran")
            Text(
                text = "L'application est compatible avec TalkBack : les boutons, titres, erreurs et " +
                        "résultats de scan NFC sont annoncés à voix haute, et chaque écran annonce son nom.",
                fontSize = 14.sp,
                color = ThemeConstants.secondaryText,
            )
            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun NotificationsSection(
    push: PushUiState,
    onEnableNotifications: () -> Unit,
    onSendTestPush: () -> Unit,
    onSendPushToUser: (userId: String, title: String, body: String) -> Unit,
    onRegisterDevice: () -> Unit,
    onCheckDevices: () -> Unit,
) {
    var targetUserId by remember { mutableStateOf("") }
    var pushTitle by remember { mutableStateOf("") }
    var pushBody by remember { mutableStateOf("") }
    var showRecipientScan by remember { mutableStateOf(false) }

    if (showRecipientScan) {
        RecipientScanDialog(
            onCardScanned = { cardId ->
                targetUserId = cardId
                showRecipientScan = false
            },
            onDismiss = { showRecipientScan = false },
        )
    }

    SectionTitle("Notifications")
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .optionCardBorder()
            .padding(horizontal = 16.dp, vertical = 14.dp),
    ) {
        Text(
            text = if (push.notificationsEnabled) "Notifications activées" else "Notifications désactivées",
            fontSize = 17.sp,
            fontWeight = FontWeight.Bold,
            color = ThemeConstants.onClassicBackground,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = "Autorise les notifications pour en recevoir sur cet appareil, puis envoie-toi une notification de test.",
            fontSize = 14.sp,
            color = ThemeConstants.secondaryText,
        )
        Spacer(Modifier.height(12.dp))
        Button(
            onClick = onEnableNotifications,
            enabled = !push.notificationsEnabled,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(
                containerColor = ThemeConstants.accentBackground,
                contentColor = ThemeConstants.onAccentBackground,
            ),
        ) {
            Text("Activer les notifications")
        }
        Spacer(Modifier.height(8.dp))
        Button(
            onClick = onSendTestPush,
            enabled = push.notificationsEnabled && !push.isSending,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(
                containerColor = ThemeConstants.accentBackground,
                contentColor = ThemeConstants.onAccentBackground,
            ),
        ) {
            if (push.isSending) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    color = ThemeConstants.onAccentBackground,
                    strokeWidth = 2.dp,
                )
            } else {
                Text("Envoyer une notification de test")
            }
        }

        Spacer(Modifier.height(20.dp))
        Text(
            text = "Notifier un utilisateur",
            modifier = Modifier.heading(),
            fontSize = 17.sp,
            fontWeight = FontWeight.Bold,
            color = ThemeConstants.onClassicBackground,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = "Envoie une notification à tous les appareils d'un utilisateur : toi-même ou ton binôme.",
            fontSize = 14.sp,
            color = ThemeConstants.secondaryText,
        )
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = targetUserId,
            onValueChange = { targetUserId = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Identifiant du destinataire (vide = moi)") },
            singleLine = true,
            enabled = !push.isSending,
        )
        TextButton(
            onClick = { showRecipientScan = true },
            enabled = !push.isSending,
        ) {
            Text("Scanner la carte du destinataire", color = ThemeConstants.accentBackground)
        }
        Spacer(Modifier.height(4.dp))
        OutlinedTextField(
            value = pushTitle,
            onValueChange = { pushTitle = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Titre") },
            singleLine = true,
            enabled = !push.isSending,
        )
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = pushBody,
            onValueChange = { pushBody = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Message") },
            enabled = !push.isSending,
        )
        Spacer(Modifier.height(8.dp))
        Button(
            onClick = { onSendPushToUser(targetUserId, pushTitle, pushBody) },
            enabled = !push.isSending && pushTitle.isNotBlank() && pushBody.isNotBlank(),
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(
                containerColor = ThemeConstants.accentBackground,
                contentColor = ThemeConstants.onAccentBackground,
            ),
        ) {
            Text("Envoyer à cet utilisateur")
        }
        if (push.message != null) {
            Spacer(Modifier.height(12.dp))
            if (push.isError) {
                ErrorText(push.message)
            } else {
                Text(text = push.message, fontSize = 14.sp, color = ThemeConstants.accentBackground)
            }
        }

        Spacer(Modifier.height(20.dp))
        Text(
            text = "Cet appareil sur le serveur",
            modifier = Modifier.heading(),
            fontSize = 17.sp,
            fontWeight = FontWeight.Bold,
            color = ThemeConstants.onClassicBackground,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = "L'appareil est enregistré automatiquement après la connexion et à chaque nouveau jeton.",
            fontSize = 14.sp,
            color = ThemeConstants.secondaryText,
        )
        Spacer(Modifier.height(12.dp))
        Button(
            onClick = onRegisterDevice,
            enabled = !push.isBusyWithDevices,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(
                containerColor = ThemeConstants.accentBackground,
                contentColor = ThemeConstants.onAccentBackground,
            ),
        ) {
            Text("Enregistrer cet appareil")
        }
        Spacer(Modifier.height(8.dp))
        Button(
            onClick = onCheckDevices,
            enabled = !push.isBusyWithDevices,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(
                containerColor = ThemeConstants.accentBackground,
                contentColor = ThemeConstants.onAccentBackground,
            ),
        ) {
            if (push.isBusyWithDevices) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    color = ThemeConstants.onAccentBackground,
                    strokeWidth = 2.dp,
                )
            } else {
                Text("Vérifier les appareils enregistrés")
            }
        }
        if (push.deviceMessage != null) {
            Spacer(Modifier.height(12.dp))
            if (push.deviceMessageIsError) {
                ErrorText(push.deviceMessage)
            } else {
                Text(text = push.deviceMessage, fontSize = 14.sp, color = ThemeConstants.accentBackground)
            }
        }
        push.devices.forEach { device ->
            Spacer(Modifier.height(6.dp))
            // Le jeton est long et sensible : on n'en montre que la fin pour reconnaître l'appareil.
            Text(
                text = "${device.platform} · …${device.token.takeLast(6)} · ${device.createdAt}",
                fontSize = 13.sp,
                color = ThemeConstants.secondaryText,
            )
        }
    }
}

/** Lit une carte NFC (physique ou virtuelle) et en renvoie l'identifiant, qui sert de `userId`. */
@Composable
private fun RecipientScanDialog(
    onCardScanned: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var scanAttempt by remember { mutableIntStateOf(0) }
    var scanStatus by remember { mutableStateOf<String?>(null) }
    var scanError by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Scanne la carte du destinataire") },
        text = {
            Column {
                Text("Approche la carte NFC, ou le téléphone présentant une carte virtuelle, de ton téléphone.")
                Spacer(Modifier.height(12.dp))
                // Un nouveau scanner par tentative : chaque lecture réussie ou ratée est à usage unique.
                key(scanAttempt) {
                    NfcCardScanner(
                        onTagRead = onCardScanned,
                        onScanStatus = { scanStatus = it },
                        onScanError = { scanError = it },
                    )
                }
                scanStatus?.let {
                    Spacer(Modifier.height(8.dp))
                    Text(it, color = ThemeConstants.accentBackground, modifier = Modifier.politeLiveRegion())
                }
                scanError?.let { message ->
                    Spacer(Modifier.height(8.dp))
                    ErrorText(message)
                    TextButton(
                        onClick = {
                            scanError = null
                            scanStatus = null
                            scanAttempt += 1
                        },
                    ) {
                        Text("Réessayer", color = ThemeConstants.accentBackground)
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Annuler") }
        },
    )
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text,
        modifier = Modifier.heading().padding(bottom = 12.dp),
        fontSize = 18.sp,
        fontWeight = FontWeight.Bold,
        color = ThemeConstants.onClassicBackground,
    )
}

/** Toute la ligne est l'interrupteur : zone tactile large, annoncée « <titre>, interrupteur, activé ». */
@Composable
private fun SwitchRow(
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .optionCardBorder()
            .toggleable(value = checked, role = Role.Switch, onValueChange = onCheckedChange)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                color = ThemeConstants.onClassicBackground,
            )
            Spacer(Modifier.height(4.dp))
            Text(text = description, fontSize = 14.sp, color = ThemeConstants.secondaryText)
        }
        Switch(
            checked = checked,
            onCheckedChange = null,
            colors = SwitchDefaults.colors(
                checkedThumbColor = ThemeConstants.onAccentBackground,
                checkedTrackColor = ThemeConstants.accentBackground,
                uncheckedThumbColor = ThemeConstants.componentBorder,
                uncheckedTrackColor = ThemeConstants.classicBackground,
                uncheckedBorderColor = ThemeConstants.componentBorder,
            ),
        )
    }
}

/** Choix exclusif : TalkBack annonce « Grande, bouton radio, sélectionné, 2 sur 3 ». */
@Composable
private fun TextSizeChoice(selectedPercent: Int, onSelect: (Int) -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .optionCardBorder()
            .selectableGroup()
            .padding(horizontal = 16.dp, vertical = 14.dp),
    ) {
        Text(
            text = "Taille du texte",
            modifier = Modifier.heading(),
            fontSize = 17.sp,
            fontWeight = FontWeight.Bold,
            color = ThemeConstants.onClassicBackground,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = "S'ajoute à la taille choisie dans les réglages du téléphone.",
            fontSize = 14.sp,
            color = ThemeConstants.secondaryText,
        )
        Spacer(Modifier.height(8.dp))
        TEXT_SCALE_OPTIONS.forEach { percent ->
            val label = when (percent) {
                100 -> "Normale"
                125 -> "Grande"
                else -> "Très grande"
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .selectable(
                        selected = percent == selectedPercent,
                        role = Role.RadioButton,
                        onClick = { onSelect(percent) },
                    )
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                RadioButton(
                    selected = percent == selectedPercent,
                    onClick = null,
                    colors = RadioButtonDefaults.colors(
                        selectedColor = ThemeConstants.accentBackground,
                        unselectedColor = ThemeConstants.componentBorder,
                    ),
                )
                Text(
                    text = "$label ($percent %)",
                    fontSize = 16.sp,
                    color = ThemeConstants.onClassicBackground,
                )
            }
        }
    }
}

private fun Modifier.optionCardBorder(): Modifier = this.border(
    width = ThemeConstants.borderWidthDp.dp,
    color = ThemeConstants.border,
    shape = RoundedCornerShape(20.dp),
)
