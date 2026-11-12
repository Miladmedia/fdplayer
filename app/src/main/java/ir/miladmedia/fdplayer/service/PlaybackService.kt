package ir.miladmedia.fdplayer.service

import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.common.audio.AudioProcessor
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.audio.AudioSink
import androidx.media3.exoplayer.audio.DefaultAudioSink
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import dagger.hilt.android.AndroidEntryPoint
import ir.miladmedia.fdplayer.domain.model.PlaybackSnapshot
import ir.miladmedia.fdplayer.domain.model.Track
import ir.miladmedia.fdplayer.domain.model.TrackSource
import ir.miladmedia.fdplayer.domain.repository.LibraryRepository
import ir.miladmedia.fdplayer.domain.repository.PlaybackStateRepository
import ir.miladmedia.fdplayer.domain.repository.PlaylistRepository
import ir.miladmedia.fdplayer.domain.repository.SettingsRepository
import ir.miladmedia.fdplayer.service.audio.AudioLevels
import ir.miladmedia.fdplayer.service.audio.FftAudioProcessor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import javax.inject.Inject
import android.util.Log

@AndroidEntryPoint
class PlaybackService : MediaSessionService() {

    @Inject lateinit var settings: SettingsRepository
    @Inject lateinit var playbackState: PlaybackStateRepository
    @Inject lateinit var library: LibraryRepository
    @Inject lateinit var playlists: PlaylistRepository

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val fft = FftAudioProcessor()
    private lateinit var player: ExoPlayer
    private var session: MediaSession? = null
    private var modeListener: AudioManager.OnModeChangedListener? = null

    private val playerListener = object : Player.Listener {
        override fun onIsPlayingChanged(isPlaying: Boolean) {
            if (!isPlaying) {
                persist()
                if (!player.playWhenReady) AudioLevels.reset()
            }
        }

        override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
            Log.e("PlaybackService", "Player error: ${error.errorCodeName}", error)
        }
    }

    override fun onCreate() {
        super.onCreate()
        val focus = runBlocking { settings.settings.first().audioFocus }
        val attributes = AudioAttributes.Builder()
            .setUsage(C.USAGE_MEDIA)
            .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
            .build()

        player = ExoPlayer.Builder(this, FftRenderersFactory(this))
            .setAudioAttributes(attributes, focus)
            .setHandleAudioBecomingNoisy(true)
            .build()
        player.addListener(playerListener)

        val callback = object : MediaSession.Callback {
            override fun onAddMediaItems(
                mediaSession: MediaSession,
                controller: MediaSession.ControllerInfo,
                mediaItems: MutableList<MediaItem>
            ): ListenableFuture<MutableList<MediaItem>> {
                val resolved = mediaItems.mapNotNull { item ->
                    val uri = item.localConfiguration?.uri
                        ?: item.requestMetadata.mediaUri
                        ?: return@mapNotNull null
                    item.buildUpon().setUri(uri).build()
                }
                return Futures.immediateFuture(resolved.toMutableList())
            }
        }
        session = MediaSession.Builder(this, player)
            .setCallback(callback)
            .build()
        registerCallWatcher()
        restoreLastTrack()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_AUTOPLAY -> autoplay(
                playlistId = intent.getLongExtra(EXTRA_PLAYLIST_ID, -1L),
                shuffle = intent.getBooleanExtra(EXTRA_SHUFFLE, false)
            )
            ACTION_PAUSE -> player.pause()
        }
        return super.onStartCommand(intent, flags, startId)
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? = session

    override fun onTaskRemoved(rootIntent: Intent?) {
        if (!player.playWhenReady || player.mediaItemCount == 0) stopSelf()
    }

    override fun onDestroy() {
        runBlocking { playbackState.save(snapshot()) }
        unregisterCallWatcher()
        player.removeListener(playerListener)
        session?.release()
        session = null
        player.release()
        scope.cancel()
        super.onDestroy()
    }

    private fun autoplay(playlistId: Long, shuffle: Boolean) {
        scope.launch {
            val tracks: List<Track> = if (playlistId < 0) {
                library.observeTracks(TrackSource.DEVICE).first()
            } else {
                playlists.getPlaylistTracks(playlistId)
            }
            val playable = tracks.filter { !it.missing }.let { if (shuffle) it.shuffled() else it }
            if (playable.isEmpty()) return@launch
            player.shuffleModeEnabled = false
            player.setMediaItems(playable.map { it.toMediaItem() })
            player.prepare()
            player.play()
        }
    }

    private fun restoreLastTrack() {
        scope.launch {
            val snap = playbackState.load()
            val uri = snap.trackUri ?: return@launch
            if (player.mediaItemCount > 0) return@launch
            val track = library.getTrackByUri(uri) ?: return@launch
            if (track.missing) return@launch
            player.setMediaItem(track.toMediaItem())
            player.seekTo(snap.positionMs)
            player.repeatMode = snap.repeatMode
            player.prepare()
        }
    }

    private fun persist() {
        val snap = snapshot()
        scope.launch { playbackState.save(snap) }
    }

    private fun snapshot(): PlaybackSnapshot = PlaybackSnapshot(
        trackUri = player.currentMediaItem?.localConfiguration?.uri?.toString(),
        positionMs = player.currentPosition,
        shuffle = player.shuffleModeEnabled,
        repeatMode = player.repeatMode
    )

    @RequiresApi(Build.VERSION_CODES.S)
    private fun registerCallWatcher() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return
        val audio = getSystemService(AudioManager::class.java) ?: return
        val listener = AudioManager.OnModeChangedListener { mode ->
            if (mode == AudioManager.MODE_IN_CALL || mode == AudioManager.MODE_IN_COMMUNICATION) {
                player.pause()
            }
        }
        modeListener = listener
        audio.addOnModeChangedListener(ContextCompat.getMainExecutor(this), listener)
    }

    @RequiresApi(Build.VERSION_CODES.S)
    private fun unregisterCallWatcher() {
        val listener = modeListener ?: return
        getSystemService(AudioManager::class.java)?.removeOnModeChangedListener(listener)
        modeListener = null
    }

    /** Builds the FFT tap into the audio sink without changing any other sink behaviour. */
    private inner class FftRenderersFactory(context: Context) : DefaultRenderersFactory(context) {
        override fun buildAudioSink(
            context: Context,
            enableFloatOutput: Boolean,
            enableAudioTrackPlaybackParams: Boolean
        ): AudioSink = DefaultAudioSink.Builder(context)
            .setAudioProcessors(arrayOf<AudioProcessor>(fft))
            .build()
    }

    companion object {
        const val ACTION_AUTOPLAY = "ir.miladmedia.fdplayer.action.AUTOPLAY"
        const val ACTION_PAUSE = "ir.miladmedia.fdplayer.action.PAUSE"
        const val EXTRA_PLAYLIST_ID = "playlist_id"
        const val EXTRA_SHUFFLE = "shuffle"
    }
}

private fun Track.toMediaItem(): MediaItem = MediaItem.Builder()
    .setUri(uri)
    .setMediaId(id.toString())
    .setMediaMetadata(
        MediaMetadata.Builder()
            .setTitle(title)
            .setArtist(artist)
            .build()
    )
    .build()
