package ir.miladmedia.fdplayer.ui.library

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.Button
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import ir.miladmedia.fdplayer.R
import ir.miladmedia.fdplayer.domain.model.Track
import ir.miladmedia.fdplayer.domain.model.TrackSource
import ir.miladmedia.fdplayer.ui.common.AddToPlaylistDialog
import ir.miladmedia.fdplayer.ui.common.EmptyState
import ir.miladmedia.fdplayer.ui.common.LoadingState
import ir.miladmedia.fdplayer.ui.common.TrackRow

@Composable
fun LibraryRoute(viewModel: LibraryViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsState()
    val playlists by viewModel.playlistChoices.collectAsState()
    LibraryScreen(
        state = state,
        playlists = playlists,
        onSelectSource = viewModel::selectSource,
        onRefresh = viewModel::refresh,
        onPlay = { index -> viewModel.playFrom(state.tracks, index) },
        onAddToPlaylist = viewModel::addToPlaylist
    )
}

@Composable
fun LibraryScreen(
    state: LibraryUiState,
    playlists: List<ir.miladmedia.fdplayer.domain.model.Playlist>,
    onSelectSource: (TrackSource) -> Unit,
    onRefresh: () -> Unit,
    onPlay: (Int) -> Unit,
    onAddToPlaylist: (Long, Long) -> Unit
) {
    var pendingTrack by remember { mutableStateOf<Track?>(null) }
    val selectedIndex = if (state.source == TrackSource.DEVICE) 0 else 1

    Column(modifier = Modifier.fillMaxSize()) {
        TabRow(selectedTabIndex = selectedIndex) {
            Tab(
                selected = selectedIndex == 0,
                onClick = { onSelectSource(TrackSource.DEVICE) },
                text = { Text(stringResource(R.string.device_music)) }
            )
            Tab(
                selected = selectedIndex == 1,
                onClick = { onSelectSource(TrackSource.DOWNLOAD) },
                text = { Text(stringResource(R.string.downloaded_music)) }
            )
        }

        Row(modifier = Modifier.fillMaxWidth().padding(12.dp)) {
            Button(onClick = onRefresh, enabled = !state.scanning) {
                Text(stringResource(R.string.library_refresh))
            }
        }

        when {
            state.scanning && state.tracks.isEmpty() -> LoadingState()
            state.tracks.isEmpty() -> EmptyState(stringResource(R.string.empty_list))
            else -> LazyColumn(modifier = Modifier.fillMaxSize()) {
                itemsIndexed(state.tracks, key = { _, t -> t.id }) { index, track ->
                    TrackRow(
                        track = track,
                        onClick = { onPlay(index) },
                        onAdd = { pendingTrack = track }
                    )
                }
            }
        }
    }

    pendingTrack?.let { track ->
        AddToPlaylistDialog(
            playlists = playlists,
            onPick = { p ->
                onAddToPlaylist(p.id, track.id)
                pendingTrack = null
            },
            onDismiss = { pendingTrack = null }
        )
    }
}
