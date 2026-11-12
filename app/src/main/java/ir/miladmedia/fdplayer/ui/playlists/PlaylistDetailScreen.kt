package ir.miladmedia.fdplayer.ui.playlists

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import ir.miladmedia.fdplayer.R
import ir.miladmedia.fdplayer.domain.model.Track
import ir.miladmedia.fdplayer.ui.common.EmptyState
import ir.miladmedia.fdplayer.ui.common.TrackRow

@Composable
fun PlaylistDetailRoute(
    playlistId: Long,
    onBack: () -> Unit,
    viewModel: PlaylistDetailViewModel = hiltViewModel()
) {
    val tracks by viewModel.tracks.collectAsState()
    val name by viewModel.name.collectAsState()
    PlaylistDetailScreen(
        name = name,
        tracks = tracks,
        onBack = onBack,
        onRename = viewModel::rename,
        onPlayAll = { viewModel.playAll(shuffle = false) },
        onPlayFrom = viewModel::playFrom,
        onRemove = viewModel::removeTrack
    )
}

@Composable
fun PlaylistDetailScreen(
    name: String,
    tracks: List<Track>,
    onBack: () -> Unit,
    onRename: (String) -> Unit,
    onPlayAll: () -> Unit,
    onPlayFrom: (Int) -> Unit,
    onRemove: (Long) -> Unit
) {
    var showRename by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = stringResource(R.string.nav_back)
                )
            }
            Text(
                text = name,
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.weight(1f).padding(horizontal = 4.dp)
            )
            IconButton(onClick = { showRename = true }) {
                Icon(Icons.Filled.Edit, contentDescription = stringResource(R.string.rename))
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.Start
        ) {
            Button(onClick = onPlayAll, enabled = tracks.isNotEmpty()) {
                Icon(Icons.Filled.PlayArrow, contentDescription = null)
                Text(stringResource(R.string.play_all), modifier = Modifier.padding(start = 8.dp))
            }
        }

        if (tracks.isEmpty()) {
            EmptyState(text = stringResource(R.string.empty_list), modifier = Modifier.fillMaxSize())
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                itemsIndexed(tracks, key = { _, t -> t.id }) { index, track ->
                    TrackRow(
                        track = track,
                        onClick = { onPlayFrom(index) },
                        onRemove = { onRemove(track.id) }
                    )
                }
            }
        }
    }

    if (showRename) {
        RenameDialog(
            initial = name,
            onConfirm = {
                onRename(it)
                showRename = false
            },
            onDismiss = { showRename = false }
        )
    }
}

@Composable
private fun RenameDialog(initial: String, onConfirm: (String) -> Unit, onDismiss: () -> Unit) {
    var value by remember { mutableStateOf(initial) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.rename)) },
        text = {
            OutlinedTextField(
                value = value,
                onValueChange = { value = it },
                label = { Text(stringResource(R.string.playlist_name)) },
                singleLine = true
            )
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(value) }, enabled = value.isNotBlank()) {
                Text(stringResource(R.string.save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
        }
    )
}
