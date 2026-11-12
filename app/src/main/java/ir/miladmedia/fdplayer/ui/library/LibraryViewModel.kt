package ir.miladmedia.fdplayer.ui.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import ir.miladmedia.fdplayer.domain.model.Playlist
import ir.miladmedia.fdplayer.domain.model.Track
import ir.miladmedia.fdplayer.domain.model.TrackSource
import ir.miladmedia.fdplayer.domain.repository.LibraryRepository
import ir.miladmedia.fdplayer.domain.repository.PlaylistRepository
import ir.miladmedia.fdplayer.ui.player.PlayerConnection
import ir.miladmedia.fdplayer.ui.player.toMediaItem
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class LibraryUiState(
    val source: TrackSource = TrackSource.DEVICE,
    val tracks: List<Track> = emptyList(),
    val scanning: Boolean = false,
    val lastScanCount: Int? = null
)

@HiltViewModel
class LibraryViewModel @Inject constructor(
    private val library: LibraryRepository,
    private val playlists: PlaylistRepository,
    private val player: PlayerConnection
) : ViewModel() {

    private val source = MutableStateFlow(TrackSource.DEVICE)
    private val scanning = MutableStateFlow(false)
    private val lastScan = MutableStateFlow<Int?>(null)

    @OptIn(ExperimentalCoroutinesApi::class)
    val state: StateFlow<LibraryUiState> = source.flatMapLatest { src ->
        combine(library.observeTracks(src), scanning, lastScan) { tracks, s, last ->
            LibraryUiState(src, tracks, s, last)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), LibraryUiState())

    val playlistChoices: StateFlow<List<Playlist>> = playlists.observePlaylists()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun selectSource(value: TrackSource) {
        source.value = value
    }

    fun refresh() {
        if (scanning.value) return
        viewModelScope.launch {
            scanning.value = true
            try {
                lastScan.value = library.scanDeviceAudio()
            } finally {
                scanning.value = false
            }
        }
    }

    /** The whole visible list becomes the queue, starting at [index]. */
    fun playFrom(tracks: List<Track>, index: Int, shuffle: Boolean = false) {
        if (tracks.isEmpty()) return
        player.play(tracks.map { it.toMediaItem() }, index, shuffle)
    }

    fun addToPlaylist(playlistId: Long, trackId: Long) {
        viewModelScope.launch { playlists.addTrack(playlistId, trackId) }
    }
}
