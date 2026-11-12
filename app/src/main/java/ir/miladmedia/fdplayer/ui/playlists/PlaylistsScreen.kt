package ir.miladmedia.fdplayer.ui.playlists

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
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
import ir.miladmedia.fdplayer.domain.model.Playlist
import ir.miladmedia.fdplayer.ui.common.EmptyState

/** Dialog state: null means closed; otherwise editing an existing playlist or creating a new one. */
private sealed interface NameTarget {
    data object Create : NameTarget
    data class Rename(val playlist: Playlist) : NameTarget
}

@Composable
fun PlaylistsRoute(
    onOpen: (Long) -> Unit,
    viewModel: PlaylistsViewModel = hiltViewModel()
) {
    val playlists by viewModel.playlistsFlow.collectAsState()
    PlaylistsScreen(
        playlists = playlists,
        onOpen = onOpen,
        onCreate = viewModel::create,
        onRename = viewModel::rename,
        onDelete = viewModel::delete
    )
}

@Composable
fun PlaylistsScreen(
    playlists: List<Playlist>,
    onOpen: (Long) -> Unit,
    onCreate: (String) -> Unit,
    onRename: (Long, String) -> Unit,
    onDelete: (Long) -> Unit
) {
    var target by remember { mutableStateOf<NameTarget?>(null) }
    var pendingDelete by remember { mutableStateOf<Playlist?>(null) }

    Box(modifier = Modifier.fillMaxSize()) {
        if (playlists.isEmpty()) {
            EmptyState(text = stringResource(R.string.no_playlist), modifier = Modifier.fillMaxSize())
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(playlists, key = { it.id }) { playlist ->
                    ListItem(
                        headlineContent = { Text(playlist.name) },
                        supportingContent = { Text("${playlist.trackCount}") },
                        trailingContent = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(onClick = { target = NameTarget.Rename(playlist) }) {
                                    Icon(Icons.Filled.Edit, contentDescription = stringResource(R.string.rename))
                                }
                                IconButton(onClick = { pendingDelete = playlist }) {
                                    Icon(Icons.Filled.Delete, contentDescription = stringResource(R.string.delete))
                                }
                            }
                        },
                        modifier = Modifier.clickable { onOpen(playlist.id) }
                    )
                }
            }
        }
        FloatingActionButton(
            onClick = { target = NameTarget.Create },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
        ) {
            Icon(Icons.Filled.Add, contentDescription = stringResource(R.string.add_playlist))
        }
    }

    target?.let { current ->
        val initial = (current as? NameTarget.Rename)?.playlist?.name.orEmpty()
        NameDialog(
            title = stringResource(
                if (current is NameTarget.Rename) R.string.rename else R.string.add_playlist
            ),
            initial = initial,
            onConfirm = { name ->
                when (current) {
                    is NameTarget.Create -> onCreate(name)
                    is NameTarget.Rename -> onRename(current.playlist.id, name)
                }
                target = null
            },
            onDismiss = { target = null }
        )
    }

    pendingDelete?.let { playlist ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text(stringResource(R.string.delete)) },
            text = { Text(playlist.name) },
            confirmButton = {
                TextButton(onClick = {
                    onDelete(playlist.id)
                    pendingDelete = null
                }) { Text(stringResource(R.string.delete)) }
            },
            dismissButton = {
                TextButton(onClick = { pendingDelete = null }) { Text(stringResource(R.string.cancel)) }
            }
        )
    }
}

@Composable
private fun NameDialog(
    title: String,
    initial: String,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var name by remember { mutableStateOf(initial) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text(stringResource(R.string.playlist_name)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(name) }, enabled = name.isNotBlank()) {
                Text(stringResource(R.string.save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
        }
    )
}
