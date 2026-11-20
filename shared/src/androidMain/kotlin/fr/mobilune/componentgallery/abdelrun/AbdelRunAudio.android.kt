package fr.mobilune.componentgallery.abdelrun

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.pm.ActivityInfo
import android.media.MediaPlayer
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalContext
import fr.mobilune.componentgallery.appContext
import java.io.File

actual fun createPlatformAudioPlayer(): PlatformAudioPlayer = AndroidAudioPlayer(appContext)

/** Une instance de MediaPlayer par musique, créée à la demande à partir d'un fichier du cache. */
private class AndroidAudioPlayer(private val context: Context) : PlatformAudioPlayer {
    private val players = HashMap<AudioTrack, MediaPlayer>()
    private val playing = HashSet<AudioTrack>()
    private var volume = 1f

    private fun playerFor(track: AudioTrack, bytes: ByteArray): MediaPlayer? =
        players[track] ?: try {
            val file = File(context.cacheDir, "abdelrun_${track.name.lowercase()}.ogg")
            if (!file.exists() || file.length() != bytes.size.toLong()) file.writeBytes(bytes)
            MediaPlayer().apply {
                setDataSource(file.absolutePath)
                prepare()
            }.also { players[track] = it }
        } catch (_: Exception) {
            null // Le jeu reste jouable sans musique.
        }

    override fun play(track: AudioTrack, bytes: ByteArray, loop: Boolean) {
        val player = playerFor(track, bytes) ?: return
        try {
            player.isLooping = loop
            player.setVolume(volume, volume)
            player.seekTo(0)
            player.start()
            playing.add(track)
        } catch (_: IllegalStateException) {
            players.remove(track)
        }
    }

    override fun stop(track: AudioTrack) {
        val player = players[track] ?: return
        try {
            if (player.isPlaying) player.pause()
            player.seekTo(0)
        } catch (_: IllegalStateException) {
            players.remove(track)
        }
        playing.remove(track)
    }

    override fun pauseAll() {
        for (track in playing) {
            try {
                players[track]?.takeIf { it.isPlaying }?.pause()
            } catch (_: IllegalStateException) {
            }
        }
    }

    override fun resumeAll() {
        for (track in playing) {
            try {
                players[track]?.start()
            } catch (_: IllegalStateException) {
            }
        }
    }

    override fun setVolume(volume: Float) {
        this.volume = volume
        for (player in players.values) {
            try {
                player.setVolume(volume, volume)
            } catch (_: IllegalStateException) {
            }
        }
    }

    override fun release() {
        for (player in players.values) {
            try {
                player.release()
            } catch (_: IllegalStateException) {
            }
        }
        players.clear()
        playing.clear()
    }
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

@Composable
actual fun ForceLandscape(enabled: Boolean) {
    val activity = LocalContext.current.findActivity()
    DisposableEffect(activity, enabled) {
        val previous = activity?.requestedOrientation
        if (enabled) activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
        onDispose {
            if (enabled && previous != null) activity.requestedOrientation = previous
        }
    }
}
