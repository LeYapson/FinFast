package fr.mobilune.componentgallery.abdelrun

import androidx.compose.runtime.Composable

/** Desktop : le JDK ne décode pas l'OGG sans bibliothèque supplémentaire, le jeu y est muet. */
actual fun createPlatformAudioPlayer(): PlatformAudioPlayer = object : PlatformAudioPlayer {
    override fun play(track: AudioTrack, bytes: ByteArray, loop: Boolean) = Unit
    override fun stop(track: AudioTrack) = Unit
    override fun pauseAll() = Unit
    override fun resumeAll() = Unit
    override fun setVolume(volume: Float) = Unit
    override fun release() = Unit
}

@Composable
actual fun ForceLandscape(enabled: Boolean) = Unit
