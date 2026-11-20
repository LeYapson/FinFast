package fr.mobilune.componentgallery.data

const val PREFERENCES_FILE_NAME = "preferences.preferences_pb"
const val VIRTUAL_CARD_AID_HEX = "F04D4F42494C01"
const val VIRTUAL_CARD_SELECT_MARKER = "MOBILUNE"

/**
 * AID de l'app « Ma Carte » (`F0` + « INFAST » en ASCII). Protocole : le SELECT renvoie directement
 * l'identifiant en UTF-8 suivi de `90 00`, ou `6A 82` si la carte virtuelle est désactivée.
 */
const val INFAST_AID_HEX = "F046494E46415354"

interface VirtualCardRepository {
    suspend fun getCardId(): String?

    suspend fun isCardEnabled(): Boolean

    suspend fun saveCardId(cardId: String)

    suspend fun setCardEnabled(enabled: Boolean)
}
