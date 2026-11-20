package fr.mobilune.componentgallery.abdelrun

import androidx.compose.runtime.Composable
import componentgallery.shared.generated.resources.Res
import org.jetbrains.compose.resources.ExperimentalResourceApi

/** Les quatre musiques du jeu d'origine. */
enum class AudioTrack(val path: String) {
    /** AbdelRunSong.ogg : menus. */
    Menu("files/abdelrun_menu.ogg"),

    /** playSong.ogg : partie en cours. */
    Play("files/abdelrun_play.ogg"),

    /** toucheW.ogg : « pouvoir du temps » (remplace la musique de partie). */
    Power("files/abdelrun_power.ogg"),

    /** gameoverMusic.ogg : écran de fin de partie. */
    GameOver("files/abdelrun_gameover.ogg"),
}

/** Lecteur audio de la plateforme (Android : MediaPlayer ; desktop : muet). */
interface PlatformAudioPlayer {
    fun play(track: AudioTrack, bytes: ByteArray, loop: Boolean)
    fun stop(track: AudioTrack)
    fun pauseAll()
    fun resumeAll()
    fun setVolume(volume: Float)
    fun release()
}

expect fun createPlatformAudioPlayer(): PlatformAudioPlayer

/** Verrouille l'écran en paysage tant que [enabled] (le jeu est une scène 1024 x 600). */
@Composable
expect fun ForceLandscape(enabled: Boolean)

/** Charge les musiques depuis les ressources et les joue via le lecteur de la plateforme. */
class AbdelRunAudio(private val platform: PlatformAudioPlayer = createPlatformAudioPlayer()) {
    private val cache = HashMap<AudioTrack, ByteArray>()
    private var current: AudioTrack? = null

    @OptIn(ExperimentalResourceApi::class)
    private suspend fun bytes(track: AudioTrack): ByteArray =
        cache[track] ?: Res.readBytes(track.path).also { cache[track] = it }

    /** Joue [track] depuis le début (en boucle par défaut) ; ne relance pas une musique déjà en cours. */
    suspend fun play(track: AudioTrack, loop: Boolean = true) {
        if (current == track) return
        current?.let { platform.stop(it) }
        current = track
        platform.play(track, bytes(track), loop)
    }

    fun stop() {
        current?.let { platform.stop(it) }
        current = null
    }

    fun pause() = platform.pauseAll()
    fun resume() = platform.resumeAll()
    fun setVolume(volume: Float) = platform.setVolume(volume.coerceIn(0f, 1f))

    fun release() {
        current = null
        platform.release()
    }
}
