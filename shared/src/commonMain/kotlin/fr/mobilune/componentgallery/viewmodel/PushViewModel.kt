package fr.mobilune.componentgallery.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import fr.mobilune.componentgallery.data.PushDevice
import fr.mobilune.componentgallery.data.PushRepository
import fr.mobilune.componentgallery.data.SessionExpiredException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class PushUiState(
    val notificationsEnabled: Boolean = false,
    val isSending: Boolean = false,
    val message: String? = null,
    val isError: Boolean = false,
    // Enregistrement de l'appareil sur le serveur (POST /push/devices) et vérification (GET /push/devices).
    val isBusyWithDevices: Boolean = false,
    val deviceMessage: String? = null,
    val deviceMessageIsError: Boolean = false,
    val devices: List<PushDevice> = emptyList(),
)

class PushViewModel(
    private val pushRepository: PushRepository,
    private val getDeviceToken: suspend () -> String?,
    private val getOwnUserId: suspend () -> String?,
) : ViewModel() {
    private val mutableUiState = MutableStateFlow(PushUiState())
    val uiState: StateFlow<PushUiState> = mutableUiState.asStateFlow()

    /** État de la permission relevé sans rien demander à l'utilisateur. */
    fun onPermissionChecked(granted: Boolean) {
        mutableUiState.update { it.copy(notificationsEnabled = granted) }
    }

    /** Résultat de la fenêtre « Autoriser les notifications ». */
    fun onPermissionResult(granted: Boolean) {
        mutableUiState.update {
            it.copy(
                notificationsEnabled = granted,
                message = if (granted) {
                    "Notifications activées."
                } else {
                    "Notifications non activées. Autorise-les dans les réglages du téléphone."
                },
                isError = !granted,
            )
        }
    }

    /** `POST /push/test-token` : notification de test vers cet appareil. */
    fun sendTestPush() {
        launchSend {
            val deviceToken = getDeviceToken()
                ?: throw IllegalStateException("Le jeton de notification de cet appareil est indisponible.")
            val result = pushRepository.sendTestPush(deviceToken)
            if (result.sent > 0) {
                "Notification envoyée. Elle devrait arriver dans un instant." to false
            } else {
                "Le serveur n'a pas pu envoyer la notification (échecs : ${result.failed})." to true
            }
        }
    }

    /**
     * `POST /push/send` : notifie tous les appareils d'un utilisateur. [userId] vide = soi-même ;
     * sinon n'importe quel utilisateur, par exemple son binôme.
     */
    fun sendPushToUser(userId: String, title: String, body: String) {
        launchSend {
            val target = userId.trim().ifEmpty {
                getOwnUserId()
                    ?: throw IllegalStateException("Impossible de retrouver ton identifiant. Reconnecte-toi.")
            }
            val result = pushRepository.sendPush(target, title.trim(), body.trim())
            when {
                result.sent > 0 -> "Notification envoyée à ${result.sent} appareil(s)." to false
                result.failed > 0 -> "Le serveur n'a pas pu envoyer la notification (échecs : ${result.failed})." to true
                else -> "Aucun appareil enregistré pour cet utilisateur : rien n'a été envoyé." to true
            }
        }
    }

    /** Lance un envoi ; [block] renvoie le message à afficher et s'il s'agit d'une erreur. */
    private fun launchSend(block: suspend () -> Pair<String, Boolean>) {
        if (mutableUiState.value.isSending) return
        mutableUiState.update { it.copy(isSending = true, message = null, isError = false) }
        viewModelScope.launch {
            try {
                val (message, isError) = block()
                mutableUiState.update { it.copy(isSending = false, message = message, isError = isError) }
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: SessionExpiredException) {
                // L'UI est renvoyée vers la connexion par AuthSession.expirations ; on nettoie juste l'état.
                mutableUiState.update { it.copy(isSending = false, message = null, isError = false) }
            } catch (exception: Exception) {
                mutableUiState.update {
                    it.copy(
                        isSending = false,
                        message = exception.message ?: "L'envoi de la notification a échoué.",
                        isError = true,
                    )
                }
            }
        }
    }

    /**
     * Enregistre cet appareil sur le serveur. Appelé automatiquement après la connexion ; le bouton
     * des options le relance à la demande ([manual] = true), et dit alors pourquoi rien n'a été fait.
     */
    fun registerDevice(manual: Boolean = false) {
        launchDeviceTask(manual = manual) {
            val registered = pushRepository.registerCurrentDevice()
            when {
                registered -> setDeviceMessage("Appareil enregistré sur le serveur.")
                manual -> setDeviceMessage(
                    "Rien n'a été enregistré : jeton de notification indisponible ou session absente.",
                    isError = true,
                )
            }
        }
    }

    /** Le jeton FCM a changé (`onNewToken`) : on l'enregistre si une session est active. */
    fun onNewToken(token: String) {
        launchDeviceTask(manual = false) {
            if (pushRepository.onNewToken(token)) {
                setDeviceMessage("Nouveau jeton enregistré sur le serveur.")
            }
        }
    }

    /** `GET /push/devices` : vérifie que cet appareil figure bien dans la liste du serveur. */
    fun checkRegisteredDevices() {
        launchDeviceTask(manual = true) {
            val devices = pushRepository.listDevices()
            val ownToken = getDeviceToken()
            val isRegistered = ownToken != null && devices.any { it.token == ownToken }
            mutableUiState.update { it.copy(devices = devices) }
            setDeviceMessage(
                when {
                    ownToken == null -> "Jeton de notification indisponible sur cet appareil."
                    isRegistered -> "Cet appareil est bien enregistré (${devices.size} au total)."
                    else -> "Cet appareil n'est pas dans la liste du serveur (${devices.size} appareil(s) enregistré(s))."
                },
                isError = ownToken == null || !isRegistered,
            )
        }
    }

    private fun setDeviceMessage(message: String, isError: Boolean = false) {
        mutableUiState.update { it.copy(deviceMessage = message, deviceMessageIsError = isError) }
    }

    /** Lance [block] en marquant l'état « occupé » ; les erreurs sont affichées si [manual], sinon silencieuses sauf échec réseau. */
    private fun launchDeviceTask(manual: Boolean, block: suspend () -> Unit) {
        mutableUiState.update { it.copy(isBusyWithDevices = true) }
        viewModelScope.launch {
            try {
                block()
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: SessionExpiredException) {
                // Retour à la connexion géré par AuthSession.expirations.
            } catch (exception: Exception) {
                setDeviceMessage(
                    exception.message ?: "Impossible de joindre le serveur de notifications.",
                    isError = true,
                )
            } finally {
                mutableUiState.update { it.copy(isBusyWithDevices = false) }
            }
        }
    }
}
