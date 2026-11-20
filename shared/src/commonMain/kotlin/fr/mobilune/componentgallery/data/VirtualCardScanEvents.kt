package fr.mobilune.componentgallery.data

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/**
 * Prévient l'UI qu'un lecteur NFC vient de lire la carte virtuelle.
 * Le service HCE Android tourne dans le même processus que l'app, d'où ce bus en mémoire.
 */
object VirtualCardScanEvents {
    private val mutableEvents = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val events: SharedFlow<Unit> = mutableEvents.asSharedFlow()

    fun notifyScanned() {
        mutableEvents.tryEmit(Unit)
    }
}
