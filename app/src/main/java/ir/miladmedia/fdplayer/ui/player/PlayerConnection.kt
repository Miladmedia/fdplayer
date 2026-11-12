package ir.miladmedia.fdplayer.ui.player

import android.content.ComponentName
import android.content.Context
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.ListenableFuture
import dagger.hilt.android.qualifiers.ApplicationContext
import ir.miladmedia.fdplayer.service.PlaybackService
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.Dispatchers

data class PlayerUiState(
    val mediaId: String? = null,
    val title: String = "",
    val artist: String = "",
    val isPlaying: Boolean = false,
    val positionMs: Long = 0L,
    val durationMs: Long = 0L,
    val shuffle: Boolean = false,
    val repeatMode: Int = Player.REPEAT_MODE_OFF,
    val hasMedia: Boolean = false
)

/**
 * Singleton wrapper around a MediaController bound to PlaybackService.
 * Exposes state as flows so screens never touch the controller directly.
 */
@Singleton
class PlayerConnection @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var controller: MediaController? = null
    private val connectLock = Mutex()
    private var ticker: Job? = null

    private val _state = MutableStateFlow(PlayerUiState())
    val state: StateFlow<PlayerUiState> = _state.asStateFlow()

    private val listener = object : Player.Listener {
        override fun onEvents(player: Player, events: Player.Events) = refresh(player)
    }

    suspend fun ensureConnected(): MediaController? = connectLock.withLock {
        controller?.let { return@withLock it }
        try {
            val token = SessionToken(context, ComponentName(context, PlaybackService::class.java))
            val built = awaitFuture(MediaController.Builder(context, token).buildAsync())
            built.addListener(listener)
            controller = built
            refresh(built)
            startTicker(built)
            built
        } catch (e: Exception) {
            null
        }
    }

    fun play(items: List<MediaItem>, startIndex: Int = 0, shuffle: Boolean = false) {
        scope.launch {
            val c = ensureConnected() ?: return@launch
            c.shuffleModeEnabled = shuffle
            c.setMediaItems(items, startIndex, 0L)
            c.prepare()
            c.play()
        }
    }

    fun togglePlay() {
        scope.launch {
            val c = ensureConnected() ?: return@launch
            if (c.isPlaying) c.pause() else c.play()
        }
    }

    fun next() = scope.launch { ensureConnected()?.seekToNextMediaItem() }
    fun previous() = scope.launch { ensureConnected()?.seekToPreviousMediaItem() }

    fun seekTo(positionMs: Long) = scope.launch { ensureConnected()?.seekTo(positionMs) }

    fun toggleShuffle() = scope.launch {
        val c = ensureConnected() ?: return@launch
        c.shuffleModeEnabled = !c.shuffleModeEnabled
    }

    fun cycleRepeat() = scope.launch {
        val c = ensureConnected() ?: return@launch
        c.repeatMode = when (c.repeatMode) {
            Player.REPEAT_MODE_OFF -> Player.REPEAT_MODE_ALL
            Player.REPEAT_MODE_ALL -> Player.REPEAT_MODE_ONE
            else -> Player.REPEAT_MODE_OFF
        }
    }

    /**
     * The player only emits `onEvents` on state changes, so without this the seek
     * bar would freeze while a track plays. Poll the position twice a second.
     */
    private fun startTicker(c: MediaController) {
        ticker?.cancel()
        ticker = scope.launch {
            while (isActive) {
                delay(500L)
                if (c.isPlaying) {
                    val position = c.currentPosition.coerceAtLeast(0L)
                    if (position != _state.value.positionMs) {
                        _state.update { it.copy(positionMs = position) }
                    }
                }
            }
        }
    }

    private fun refresh(player: Player) {
        val item = player.currentMediaItem
        _state.update {
            PlayerUiState(
                mediaId = item?.mediaId,
                title = item?.mediaMetadata?.title?.toString().orEmpty(),
                artist = item?.mediaMetadata?.artist?.toString().orEmpty(),
                isPlaying = player.isPlaying,
                positionMs = player.currentPosition.coerceAtLeast(0L),
                durationMs = player.duration.coerceAtLeast(0L),
                shuffle = player.shuffleModeEnabled,
                repeatMode = player.repeatMode,
                hasMedia = item != null
            )
        }
    }

    private suspend fun <T> awaitFuture(future: ListenableFuture<T>): T =
        suspendCoroutine { cont ->
            future.addListener(
                { cont.resume(future.get()) },
                { it.run() }
            )
        }
}
