package ir.miladmedia.fdplayer.ui.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import ir.miladmedia.fdplayer.data.preferences.TipsPreferences
import ir.miladmedia.fdplayer.domain.model.Playlist
import ir.miladmedia.fdplayer.domain.model.Track
import ir.miladmedia.fdplayer.domain.repository.AudioRepository
import ir.miladmedia.fdplayer.domain.repository.PlaybackController
import ir.miladmedia.fdplayer.domain.repository.PlaylistRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class LibraryUiState(
    val tracks: List<Track> = emptyList(),
    val filteredTracks: List<Track> = emptyList(),
    val playlists: List<Playlist> = emptyList(),
    val searchQuery: String = "",
    val selectedCategory: Int = 0, // 0: All Songs, 1: Albums, 2: Artists
    val showFirstTimeTip: Boolean = false
)

@HiltViewModel
class LibraryViewModel @Inject constructor(
    private val audioRepository: AudioRepository,
    private val playlistRepository: PlaylistRepository,
    private val playbackController: PlaybackController,
    private val tipsPreferences: TipsPreferences
) : ViewModel() {

    private val searchQuery = MutableStateFlow("")
    private val selectedCategory = MutableStateFlow(0)
    private val showTip = MutableStateFlow(!tipsPreferences.isTipDismissed("library"))

    val uiState: StateFlow<LibraryUiState> = combine(
        audioRepository.observeTracks(),
        playlistRepository.observePlaylists(),
        searchQuery,
        selectedCategory,
        showTip
    ) { tracks, playlists, query, category, tip ->
        val filtered = if (query.isBlank()) {
            tracks
        } else {
            tracks.filter {
                it.title.contains(query, ignoreCase = true) ||
                    it.artist.contains(query, ignoreCase = true) ||
                    it.album.contains(query, ignoreCase = true)
            }
        }
        LibraryUiState(
            tracks = tracks,
            filteredTracks = filtered,
            playlists = playlists,
            searchQuery = query,
            selectedCategory = category,
            showFirstTimeTip = tip
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = LibraryUiState()
    )

    init {
        refreshLibrary()
    }

    fun refreshLibrary() {
        viewModelScope.launch {
            audioRepository.refreshTracks()
        }
    }

    fun onSearchQueryChanged(query: String) {
        searchQuery.value = query
    }

    fun onCategorySelected(index: Int) {
        selectedCategory.value = index
    }

    fun playTrackFromList(tracks: List<Track>, index: Int) {
        playbackController.playTracks(tracks, startIndex = index, shuffle = false)
    }

    fun addTrackToPlaylist(playlistId: Long, trackId: Long) {
        viewModelScope.launch {
            playlistRepository.addTrackToPlaylist(playlistId, trackId)
        }
    }

    fun dismissTip() {
        tipsPreferences.dismissTip("library")
        showTip.value = false
    }
}
