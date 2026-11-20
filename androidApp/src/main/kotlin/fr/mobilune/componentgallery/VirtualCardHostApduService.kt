package fr.mobilune.componentgallery

import android.nfc.cardemulation.HostApduService
import android.os.Bundle
import android.util.Log
import fr.mobilune.componentgallery.data.INFAST_AID_HEX
import fr.mobilune.componentgallery.data.PREFERENCES_FILE_NAME
import fr.mobilune.componentgallery.data.VIRTUAL_CARD_AID_HEX
import fr.mobilune.componentgallery.data.VIRTUAL_CARD_SELECT_MARKER
import fr.mobilune.componentgallery.data.VirtualCardScanEvents
import fr.mobilune.componentgallery.data.isStoredVirtualCardEnabled
import fr.mobilune.componentgallery.data.readStoredVirtualCardId
import kotlinx.coroutines.runBlocking

class VirtualCardHostApduService : HostApduService() {
    override fun processCommandApdu(commandApdu: ByteArray, extras: Bundle?): ByteArray {
        if (commandApdu.isSelectAid()) {
            when (commandApdu.selectedAid()) {
                // Protocole de cette app : le SELECT annonce l'app, l'identifiant vient avec `80 CA`.
                OWN_AID -> {
                    Log.d(TAG, "Virtual card application selected.")
                    return VIRTUAL_CARD_SELECT_MARKER.encodeToByteArray() + SUCCESS
                }

                // Protocole « Ma Carte » : l'identifiant est renvoyé directement dans la réponse au SELECT.
                INFAST_AID -> {
                    Log.d(TAG, "INFAST application selected.")
                    return cardIdResponse()
                }
            }
        }

        if (commandApdu.isGetCardId()) return cardIdResponse()

        return INSTRUCTION_NOT_SUPPORTED
    }

    override fun onDeactivated(reason: Int) = Unit

    /** `<identifiant UTF-8> 90 00`, ou un code d'erreur ISO 7816 si la carte ne peut pas répondre. */
    private fun cardIdResponse(): ByteArray = try {
        val dataStorePath = filesDir.resolve(PREFERENCES_FILE_NAME).absolutePath
        val cardId = runBlocking {
            if (isStoredVirtualCardEnabled(dataStorePath)) {
                readStoredVirtualCardId(dataStorePath)
            } else {
                null
            }
        }
        if (cardId == null) {
            Log.w(TAG, "Virtual card is disabled or has no stored identifier.")
            FILE_NOT_FOUND
        } else {
            val payload = cardId.encodeToByteArray()
            if (payload.size > MAX_APDU_PAYLOAD_SIZE) {
                WRONG_LENGTH
            } else {
                Log.d(TAG, "Virtual card identifier requested.")
                VirtualCardScanEvents.notifyScanned()
                payload + SUCCESS
            }
        }
    } catch (exception: Exception) {
        Log.e(TAG, "Unable to read the virtual card identifier.", exception)
        INTERNAL_ERROR
    }

    private fun ByteArray.isSelectAid(): Boolean =
        size >= 5 && this[0] == 0x00.toByte() && this[1] == 0xA4.toByte() && this[2] == 0x04.toByte()

    /** AID demandé par un SELECT, en hexadécimal majuscule ; `null` si la commande est tronquée. */
    private fun ByteArray.selectedAid(): String? {
        val aidLength = this[4].toInt() and 0xFF
        if (size < 5 + aidLength) return null
        return copyOfRange(5, 5 + aidLength).joinToString("") { "%02X".format(it.toInt() and 0xFF) }
    }

    private fun ByteArray.isGetCardId(): Boolean =
        size >= 4 &&
                this[0] == 0x80.toByte() &&
                this[1] == 0xCA.toByte() &&
                this[2] == 0x00.toByte() &&
                this[3] == 0x00.toByte()

    private companion object {
        const val TAG = "VirtualCardService"
        const val MAX_APDU_PAYLOAD_SIZE = 255
        const val OWN_AID = VIRTUAL_CARD_AID_HEX
        const val INFAST_AID = INFAST_AID_HEX
        val SUCCESS = byteArrayOf(0x90.toByte(), 0x00)
        val FILE_NOT_FOUND = byteArrayOf(0x6A, 0x82.toByte())
        val INSTRUCTION_NOT_SUPPORTED = byteArrayOf(0x6D, 0x00)
        val WRONG_LENGTH = byteArrayOf(0x67, 0x00)
        val INTERNAL_ERROR = byteArrayOf(0x6F, 0x00)
    }
}
