package fr.mobilune.componentgallery

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
internal fun WelcomeScreen(
    onLogin: () -> Unit,
    onRegister: () -> Unit,
    errorMessage: String? = null,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ThemeConstants.classicBackground)
            .screenTitle("Bienvenue")
            .safeContentPadding()
            .padding(horizontal = 12.dp, vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = "Bienvenue !",
            modifier = Modifier.heading(),
            color = ThemeConstants.onClassicBackground,
            fontSize = 30.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
        )
        if (errorMessage != null) {
            Spacer(Modifier.height(12.dp))
            AuthError(errorMessage)
        }
        Spacer(Modifier.height(40.dp))
        Button(
            onClick = onLogin,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(
                containerColor = ThemeConstants.accentBackground,
                contentColor = ThemeConstants.onAccentBackground,
            ),
        ) {
            Text("J'ai un compte")
        }
        Spacer(Modifier.height(12.dp))
        Button(
            onClick = onRegister,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(
                containerColor = ThemeConstants.onClassicBackground,
                contentColor = ThemeConstants.classicBackground,
            ),
        ) {
            Text("Je m'inscris")
        }
    }
}

@Composable
internal fun NfcScanScreen(
    title: String,
    onBack: () -> Unit,
    onTagRead: (String) -> Unit,
    isSubmitting: Boolean,
    errorMessage: String?,
    onRetry: () -> Unit,
) {
    var scanError by remember { mutableStateOf<String?>(null) }
    var scanStatus by remember { mutableStateOf<String?>(null) }
    var scanAttempt by remember { mutableIntStateOf(0) }
    val haptic = rememberAppHaptics()
    // La carte vient d'être lue : on passe en validation.
    LaunchedEffect(isSubmitting) {
        if (isSubmitting) haptic.performHapticFeedback(HapticFeedbackType.Confirm)
    }
    val scanTransition = rememberInfiniteTransition(label = "nfc_scan_pulse")
    val scanAlpha by scanTransition.animateFloat(
        initialValue = 0.65f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 900),
            repeatMode = androidx.compose.animation.core.RepeatMode.Reverse,
        ),
        label = "nfc_scan_alpha",
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ThemeConstants.classicBackground)
            .screenTitle(title)
            .safeContentPadding()
            .padding(horizontal = 12.dp, vertical = 16.dp),
    ) {
        TextButton(onClick = onBack, enabled = !isSubmitting) {
            Text(
                "← Retour",
                color = ThemeConstants.onClassicBackground,
                // La flèche serait lue « flèche gauche » : on annonce simplement « Retour ».
                modifier = Modifier.clearAndSetSemantics { contentDescription = "Retour" },
            )
        }
        Spacer(Modifier.height(36.dp))
        Text(
            text = title,
            modifier = Modifier.heading(),
            color = ThemeConstants.onClassicBackground,
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
        )
        Spacer(Modifier.height(24.dp))
        Text(
            text = "Approche une carte NFC ou le dos d'un téléphone présentant sa carte virtuelle NFC.",
            color = ThemeConstants.onClassicBackground,
            fontSize = 16.sp,
            modifier = Modifier.graphicsLayer { alpha = scanAlpha },
        )
        Spacer(Modifier.height(16.dp))
        AnimatedContent(
            targetState = isSubmitting,
            modifier = Modifier.fillMaxWidth(),
            transitionSpec = { fadeTransition() },
            label = "nfc_scan_animation",
        ) { submitting ->
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                if (submitting) {
                    ScanSuccessBadge(Modifier.size(270.dp))
                } else {
                    NfcApproachAnimation()
                }
            }
        }
        Spacer(Modifier.height(8.dp))
        if (!isSubmitting) {
            key(scanAttempt) {
                NfcCardScanner(
                    onTagRead = onTagRead,
                    onScanStatus = { scanStatus = it },
                    onScanError = { scanError = it },
                )
            }
        }
        scanStatus?.let {
            Spacer(Modifier.height(8.dp))
            Text(it, color = ThemeConstants.accentBackground, modifier = Modifier.politeLiveRegion())
        }
        val displayedError = errorMessage ?: scanError
        LaunchedEffect(displayedError) {
            if (displayedError != null) haptic.performHapticFeedback(HapticFeedbackType.Reject)
        }
        if (displayedError != null) {
            Spacer(Modifier.height(12.dp))
            AuthError(displayedError)
            TextButton(
                onClick = {
                    scanError = null
                    scanStatus = null
                    scanAttempt += 1
                    onRetry()
                },
            ) {
                Text("Réessayer", color = ThemeConstants.accentBackground)
            }
        }
        Spacer(Modifier.weight(1f))
    }
}

@Composable
internal fun MoneyAmountScreen(
    isSend: Boolean,
    amount: String,
    errorMessage: String?,
    onAmountChange: (String) -> Unit,
    onBack: () -> Unit,
    onNext: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ThemeConstants.classicBackground)
            .screenTitle("Montant")
            .safeContentPadding()
            .padding(horizontal = 12.dp, vertical = 16.dp),
    ) {
        TextButton(onClick = onBack) {
            Text(
                "← Retour",
                color = ThemeConstants.onClassicBackground,
                // La flèche serait lue « flèche gauche » : on annonce simplement « Retour ».
                modifier = Modifier.clearAndSetSemantics { contentDescription = "Retour" },
            )
        }
        Spacer(Modifier.height(36.dp))
        Text(
            text = if (isSend) "Combien veux-tu envoyer ?" else "Combien veux-tu recevoir ?",
            modifier = Modifier.heading(),
            color = ThemeConstants.onClassicBackground,
            fontSize = 26.sp,
            fontWeight = FontWeight.Bold,
        )
        Spacer(Modifier.height(24.dp))
        OutlinedTextField(
            value = amount,
            onValueChange = onAmountChange,
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Montant en euros") },
            suffix = { Text("€") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        )
        if (errorMessage != null) {
            Spacer(Modifier.height(12.dp))
            AuthError(errorMessage)
        }
        Spacer(Modifier.weight(1f))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
        ) {
            Button(
                onClick = onNext,
                colors = ButtonDefaults.buttonColors(
                    containerColor = ThemeConstants.accentBackground,
                    contentColor = ThemeConstants.onAccentBackground,
                ),
            ) {
                Text("Suivant")
            }
        }
    }
}

@Composable
internal fun MoneyNfcScanScreen(
    isSend: Boolean,
    amountCents: Long,
    isTransferring: Boolean,
    errorMessage: String?,
    onBack: () -> Unit,
    onTagRead: (String) -> Unit,
    onRetry: () -> Unit,
) {
    var scanAttempt by remember { mutableIntStateOf(0) }
    var scanError by remember { mutableStateOf<String?>(null) }
    var scanStatus by remember { mutableStateOf<String?>(null) }
    val haptic = rememberAppHaptics()
    // La carte vient d'être lue : le transfert démarre.
    LaunchedEffect(isTransferring) {
        if (isTransferring) haptic.performHapticFeedback(HapticFeedbackType.Confirm)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ThemeConstants.classicBackground)
            .screenTitle("Scan de la carte")
            .safeContentPadding()
            .padding(horizontal = 12.dp, vertical = 16.dp),
    ) {
        TextButton(onClick = onBack, enabled = !isTransferring) {
            Text(
                "← Retour",
                color = ThemeConstants.onClassicBackground,
                // La flèche serait lue « flèche gauche » : on annonce simplement « Retour ».
                modifier = Modifier.clearAndSetSemantics { contentDescription = "Retour" },
            )
        }
        Spacer(Modifier.height(36.dp))
        Text(
            text = if (isSend) "Scanne la carte du destinataire" else "Scanne la carte de l'expéditeur",
            modifier = Modifier.heading(),
            color = ThemeConstants.onClassicBackground,
            fontSize = 26.sp,
            fontWeight = FontWeight.Bold,
        )
        Spacer(Modifier.height(12.dp))
        Text(
            text = "Montant : ${amountCents.formatTransferEuro()}",
            color = ThemeConstants.accentBackground,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
        )
        Spacer(Modifier.height(24.dp))
        Text(
            text = if (isTransferring) {
                "Transfert en cours…"
            } else {
                "Approche une carte NFC ou le téléphone présentant une carte virtuelle NFC."
            },
            color = ThemeConstants.onClassicBackground,
            fontSize = 16.sp,
        )
        Spacer(Modifier.height(16.dp))
        AnimatedContent(
            targetState = isTransferring,
            modifier = Modifier.fillMaxWidth(),
            transitionSpec = { fadeTransition() },
            label = "money_nfc_scan_animation",
        ) { transferring ->
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                if (transferring) {
                    ScanSuccessBadge(Modifier.size(270.dp))
                } else {
                    NfcApproachAnimation()
                }
            }
        }
        Spacer(Modifier.height(8.dp))
        key(scanAttempt) {
            NfcCardScanner(
                onTagRead = onTagRead,
                onScanStatus = { scanStatus = it },
                onScanError = {
                    scanError = it
                },
            )
        }
        scanStatus?.let {
            Spacer(Modifier.height(8.dp))
            Text(it, color = ThemeConstants.accentBackground, modifier = Modifier.politeLiveRegion())
        }
        val displayedError = errorMessage ?: scanError
        LaunchedEffect(displayedError) {
            if (displayedError != null) haptic.performHapticFeedback(HapticFeedbackType.Reject)
        }
        displayedError?.let { message ->
            Spacer(Modifier.height(12.dp))
            AuthError(message)
            TextButton(
                onClick = {
                    scanError = null
                    scanStatus = null
                    scanAttempt += 1
                    onRetry()
                },
                enabled = !isTransferring,
            ) {
                Text("Réessayer", color = ThemeConstants.accentBackground)
            }
        }
        Spacer(Modifier.weight(1f))
    }
}

@Composable
internal fun SecretCodeScreen(
    title: String,
    progressStep: Int?,
    code: String,
    actionLabel: String,
    errorMessage: String?,
    isSubmitting: Boolean,
    onDigit: (Int) -> Unit,
    onErase: () -> Unit,
    onBack: () -> Unit,
    onAction: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ThemeConstants.classicBackground)
            .screenTitle(title)
            .safeContentPadding()
            .padding(horizontal = 12.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TextButton(onClick = onBack) {
                Text(
                "← Retour",
                color = ThemeConstants.onClassicBackground,
                // La flèche serait lue « flèche gauche » : on annonce simplement « Retour ».
                modifier = Modifier.clearAndSetSemantics { contentDescription = "Retour" },
            )
            }
        }
        if (progressStep != null) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp)
                    .clearAndSetSemantics { contentDescription = "Étape $progressStep sur 3" },
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                repeat(3) { index ->
                    val progressColor by animateColorAsState(
                        targetValue = if (index < progressStep) {
                            ThemeConstants.accentBackground
                        } else {
                            ThemeConstants.componentBorder
                        },
                        animationSpec = spring(),
                        label = "register_progress_$index",
                    )
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(5.dp)
                            .background(
                                color = progressColor,
                                shape = RoundedCornerShape(50),
                            ),
                    )
                }
            }
        }
        Spacer(Modifier.weight(1f))
        Text(
            text = title,
            modifier = Modifier.heading(),
            color = ThemeConstants.onClassicBackground,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(24.dp))
        // Les points sont purement visuels : TalkBack annonce le nombre de chiffres saisis, jamais le code.
        Row(
            modifier = Modifier.clearAndSetSemantics {
                contentDescription = "Code secret : ${code.length} chiffre${if (code.length > 1) "s" else ""} saisi${if (code.length > 1) "s" else ""} sur 4"
                liveRegion = LiveRegionMode.Polite
            },
            horizontalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            repeat(4) { index ->
                val dotColor by animateColorAsState(
                    targetValue = if (index < code.length) {
                        ThemeConstants.accentBackground
                    } else {
                        ThemeConstants.classicBackground
                    },
                    animationSpec = spring(),
                    label = "secret_code_dot_$index",
                )
                val borderColor by animateColorAsState(
                    targetValue = if (index < code.length) {
                        ThemeConstants.accentBackground
                    } else {
                        ThemeConstants.componentBorder
                    },
                    animationSpec = spring(),
                    label = "secret_code_dot_border_$index",
                )
                Box(
                    modifier = Modifier
                        .size(18.dp)
                        .background(
                            color = dotColor,
                            shape = CircleShape,
                        )
                        .border(
                            width = 1.dp,
                            color = borderColor,
                            shape = CircleShape,
                        ),
                )
            }
        }
        Spacer(Modifier.height(28.dp))
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            listOf(
                listOf(1, 2, 3),
                listOf(4, 5, 6),
                listOf(7, 8, 9),
            ).forEach { digits ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    digits.forEach { digit ->
                        TextButton(
                            onClick = { onDigit(digit) },
                            enabled = code.length < 4 && !isSubmitting,
                            modifier = Modifier.weight(1f).height(58.dp),
                        ) {
                            Text(
                                text = digit.toString(),
                                color = ThemeConstants.onClassicBackground,
                                fontSize = 24.sp,
                            )
                        }
                    }
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TextButton(
                    onClick = onErase,
                    enabled = code.isNotEmpty() && !isSubmitting,
                    modifier = Modifier.weight(1f).height(58.dp),
                ) {
                    Text(
                        "←",
                        color = ThemeConstants.onClassicBackground,
                        fontSize = 26.sp,
                        modifier = Modifier.clearAndSetSemantics { contentDescription = "Effacer le dernier chiffre" },
                    )
                }
                TextButton(
                    onClick = { onDigit(0) },
                    enabled = code.length < 4 && !isSubmitting,
                    modifier = Modifier.weight(1f).height(58.dp),
                ) {
                    Text("0", color = ThemeConstants.onClassicBackground, fontSize = 24.sp)
                }
                TextButton(
                    onClick = onAction,
                    enabled = code.length == 4 && !isSubmitting,
                    modifier = Modifier.weight(1f).height(58.dp),
                    colors = ButtonDefaults.textButtonColors(
                        containerColor = ThemeConstants.accentBackground,
                        contentColor = ThemeConstants.onAccentBackground,
                    ),
                ) {
                    Text(
                        text = if (isSubmitting) "…" else actionLabel,
                        fontSize = 12.sp,
                        maxLines = 1,
                    )
                }
            }
        }
        if (errorMessage != null) {
            Spacer(Modifier.height(8.dp))
            AuthError(errorMessage)
        }
        Spacer(Modifier.weight(1f))
    }
}

@Composable
internal fun DisplayedNameScreen(
    displayedName: String,
    errorMessage: String?,
    isSubmitting: Boolean,
    onDisplayedNameChange: (String) -> Unit,
    onBack: () -> Unit,
    onNext: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ThemeConstants.classicBackground)
            .screenTitle("Nom affiché")
            .safeContentPadding()
            .padding(horizontal = 12.dp, vertical = 16.dp),
    ) {
        TextButton(onClick = onBack, enabled = !isSubmitting) {
            Text(
                "← Retour",
                color = ThemeConstants.onClassicBackground,
                // La flèche serait lue « flèche gauche » : on annonce simplement « Retour ».
                modifier = Modifier.clearAndSetSemantics { contentDescription = "Retour" },
            )
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp)
                .clearAndSetSemantics { contentDescription = "Étape 2 sur 3" },
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            repeat(3) { index ->
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(5.dp)
                        .background(
                            color = if (index < 2) {
                                ThemeConstants.accentBackground
                            } else {
                                ThemeConstants.componentBorder
                            },
                            shape = RoundedCornerShape(50),
                        ),
                )
            }
        }
        Spacer(Modifier.height(36.dp))
        Text(
            text = "Comment veux-tu que je t'appelle ?",
            modifier = Modifier.heading(),
            color = ThemeConstants.onClassicBackground,
            fontSize = 26.sp,
            fontWeight = FontWeight.Bold,
        )
        Spacer(Modifier.height(24.dp))
        OutlinedTextField(
            value = displayedName,
            onValueChange = onDisplayedNameChange,
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Nom affiché") },
            singleLine = true,
            enabled = !isSubmitting,
        )
        if (errorMessage != null) {
            Spacer(Modifier.height(12.dp))
            AuthError(errorMessage)
        }
        Spacer(Modifier.weight(1f))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (isSubmitting) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp).semantics { contentDescription = "Création du compte en cours" },
                    color = ThemeConstants.accentBackground,
                    strokeWidth = 2.dp,
                )
            }
            Button(
                onClick = onNext,
                enabled = !isSubmitting,
                colors = ButtonDefaults.buttonColors(
                    containerColor = ThemeConstants.accentBackground,
                    contentColor = ThemeConstants.onAccentBackground,
                ),
            ) {
                Text(if (isSubmitting) "…" else "Suivant")
            }
        }
    }
}

@Composable
private fun AuthError(message: String) {
    ErrorText(message)
}

private fun Long.formatTransferEuro(): String {
    val euros = this / 100
    val cents = (this % 100).toString().padStart(2, '0')
    return "$euros,$cents €"
}
