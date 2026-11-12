package ir.miladmedia.fdplayer.ui.playlists

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import ir.miladmedia.fdplayer.domain.model.Track
import ir.miladmedia.fdplayer.domain.repository.PlaylistRepository
import ir.miladmedia.fdplayer.ui.player.PlayerConnection
import ir.miladmedia.fdplayer.ui.player.toMediaItem
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class PlaylistDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val playlists: PlaylistRepository,
    private val player: PlayerConnection
) : ViewModel() {

    private val playlistId: Long = savedStateHandle.get<Long>("playlistId") ?: 0L

    val tracks: StateFlow<List<Track>> = playlists.observePlaylistTracks(playlistId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val name: StateFlow<String> = playlists.observePlaylists()
        .map { list -> list.firstOrNull { it.id == playlistId }?.name.orEmpty() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), "")

    fun playAll(shuffle: Boolean) {
        viewModelScope.launch {
            val list = playlists.getPlaylistTracks(playlistId)
            if (list.isEmpty()) return@launch
            val start = if (shuffle) (list.indices).random() else 0
            player.play(list.map { it.toMediaItem() }, startIndex = start, shuffle = shuffle)
        }
    }

    fun playFrom(index: Int) {
        val list = tracks.value
        if (index !in list.indices) return
        player.play(list.map { it.toMediaItem() }, startIndex = index, shuffle = false)
    }

    fun removeTrack(trackId: Long) {
        viewModelScope.launch { playlists.removeTrack(playlistId, trackId) }
    }

    fun rename(name: String) {
        val clean = name.trim()
        if (clean.isEmpty()) return
        viewModelScope.launch { playlists.rename(playlistId, clean) }
    }
}
