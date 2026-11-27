package ir.miladmedia.fdplayer.service

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import dagger.hilt.android.qualifiers.ApplicationContext
import ir.miladmedia.fdplayer.domain.model.PlaybackState
import ir.miladmedia.fdplayer.domain.model.RepeatModeOption
import ir.miladmedia.fdplayer.domain.model.Track
import ir.miladmedia.fdplayer.domain.repository.PlaybackController
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ExoPlaybackManager @Inject constructor(
    @ApplicationContext private val context: Context
) : PlaybackController {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var positionTickerJob: Job? = null

    private val _playbackState = MutableStateFlow(PlaybackState())
    override val playbackState: StateFlow<PlaybackState> = _playbackState.asStateFlow()

    val player: ExoPlayer = ExoPlayer.Builder(context)
        .setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(C.USAGE_MEDIA)
                .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                .build(),
            true
        )
        .setHandleAudioBecomingNoisy(true)
        .build()

    init {
        player.addListener(object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                _playbackState.update { it.copy(isPlaying = isPlaying) }
                if (isPlaying) {
                    startPositionTicker()
                } else {
                    stopPositionTicker()
                    syncPositionSnapshot()
                }
            }

            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                syncCurrentTrackFromPlayer()
                syncPositionSnapshot()
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                syncPositionSnapshot()
            }

            override fun onShuffleModeEnabledChanged(shuffleModeEnabled: Boolean) {
                _playbackState.update { it.copy(shuffleEnabled = shuffleModeEnabled) }
            }

            override fun onRepeatModeChanged(repeatMode: Int) {
                val mapped = when (repeatMode) {
                    Player.REPEAT_MODE_ALL -> RepeatModeOption.ALL
                    Player.REPEAT_MODE_ONE -> RepeatModeOption.ONE
                    else -> RepeatModeOption.OFF
                }
                _playbackState.update { it.copy(repeatMode = mapped) }
            }

            override fun onPlayerError(error: PlaybackException) {
                Log.e(TAG, "ExoPlayer playback error [${error.errorCodeName}]: ${error.message}", error)
                _playbackState.update {
                    it.copy(
                        isPlaying = false,
                        lastError = error.localizedMessage ?: error.errorCodeName
                    )
                }
            }
        })
    }

    override fun playTracks(tracks: List<Track>, startIndex: Int, shuffle: Boolean) {
        if (tracks.isEmpty()) return
        ensurePlaybackServiceStarted()

        val safeIndex = startIndex.coerceIn(0, tracks.lastIndex)
        val mediaItems = tracks.map { track ->
            MediaItem.Builder()
                .setMediaId(track.id.toString())
                .setUri(Uri.parse(track.contentUri))
                .setMediaMetadata(
                    MediaMetadata.Builder()
                        .setTitle(track.title)
                        .setArtist(track.artist)
                        .setAlbumTitle(track.album)
                        .setArtworkUri(track.albumArtUri?.let(Uri::parse))
                        .build()
                )
                .build()
        }

        _playbackState.update {
            it.copy(
                queue = tracks,
                currentTrack = tracks[safeIndex],
                shuffleEnabled = shuffle,
                lastError = null
            )
        }

        player.shuffleModeEnabled = shuffle
        player.setMediaItems(mediaItems, safeIndex, 0L)
        player.prepare()
        player.play()
    }

    override fun playOrResumeCurrent() {
        if (player.mediaItemCount > 0) {
            ensurePlaybackServiceStarted()
            if (!player.isPlaying) {
                player.play()
            }
        }
    }

    override fun togglePlayPause() {
        if (player.isPlaying) {
            player.pause()
        } else {
            playOrResumeCurrent()
        }
    }

    override fun seekTo(positionMs: Long) {
        val duration = player.duration.coerceAtLeast(0L)
        player.seekTo(positionMs.coerceIn(0L, duration))
        syncPositionSnapshot()
    }

    override fun skipNext() {
        if (player.hasNextMediaItem()) {
            player.seekToNextMediaItem()
        }
    }

    override fun skipPrevious() {
        if (player.currentPosition > 3000L || !player.hasPreviousMediaItem()) {
            player.seekTo(0L)
        } else {
            player.seekToPreviousMediaItem()
        }
    }

    override fun toggleShuffle() {
        player.shuffleModeEnabled = !player.shuffleModeEnabled
    }

    override fun cycleRepeatMode() {
        val next = when (_playbackState.value.repeatMode) {
            RepeatModeOption.OFF -> RepeatModeOption.ALL
            RepeatModeOption.ALL -> RepeatModeOption.ONE
            RepeatModeOption.ONE -> RepeatModeOption.OFF
        }
        setRepeatMode(next)
    }

    override fun setRepeatMode(mode: RepeatModeOption) {
        player.repeatMode = when (mode) {
            RepeatModeOption.OFF -> Player.REPEAT_MODE_OFF
            RepeatModeOption.ALL -> Player.REPEAT_MODE_ALL
            RepeatModeOption.ONE -> Player.REPEAT_MODE_ONE
        }
    }

    override fun updateDominantColors(colors: List<Int>) {
        _playbackState.update { it.copy(dominantColors = colors) }
    }

    private fun ensurePlaybackServiceStarted() {
        try {
            val intent = Intent(context, FDPlaybackService::class.java)
            ContextCompat.startForegroundService(context, intent)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start FDPlaybackService", e)
        }
    }

    private fun syncCurrentTrackFromPlayer() {
        val index = player.currentMediaItemIndex
        val queue = _playbackState.value.queue
        if (index in queue.indices) {
            _playbackState.update { it.copy(currentTrack = queue[index]) }
        }
    }

    private fun syncPositionSnapshot() {
        val pos = player.currentPosition.coerceAtLeast(0L)
        val dur = player.duration.takeIf { it != C.TIME_UNSET && it > 0L }
            ?: _playbackState.value.currentTrack?.durationMs
            ?: 0L
        _playbackState.update {
            it.copy(
                positionMs = pos,
                durationMs = dur
            )
        }
    }

    private fun startPositionTicker() {
        positionTickerJob?.cancel()
        positionTickerJob = scope.launch {
            while (isActive && player.isPlaying) {
                syncPositionSnapshot()
                delay(500L)
            }
        }
    }

    private fun stopPositionTicker() {
        positionTickerJob?.cancel()
        positionTickerJob = null
    }

    companion object {
        private const val TAG = "ExoPlaybackManager"
    }
}
