package ir.miladmedia.fdplayer.ui.library

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Album
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.PlaylistAdd
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.SubcomposeAsyncImage
import ir.miladmedia.fdplayer.R
import ir.miladmedia.fdplayer.domain.model.Track
import ir.miladmedia.fdplayer.ui.components.FirstTimeTipCard
import ir.miladmedia.fdplayer.ui.components.GlassCard

@Composable
fun LibraryScreen(
    onNavigateToPlayer: () -> Unit,
    viewModel: LibraryViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var trackForPlaylistDialog by remember { mutableStateOf<Track?>(null) }

    val audioPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        Manifest.permission.READ_MEDIA_AUDIO
    } else {
        Manifest.permission.READ_EXTERNAL_STORAGE
    }

    var hasPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, audioPermission) ==
                PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasPermission = granted
        if (granted) viewModel.refreshLibrary()
    }

    LaunchedEffect(hasPermission) {
        if (hasPermission) viewModel.refreshLibrary()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = stringResource(R.string.tab_library),
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onBackground
            )
            IconButton(onClick = { viewModel.refreshLibrary() }) {
                Icon(
                    imageVector = Icons.Rounded.Refresh,
                    contentDescription = stringResource(R.string.refresh_library),
                    tint = MaterialTheme.colorScheme.onBackground
                )
            }
        }

        FirstTimeTipCard(
            visible = state.showFirstTimeTip,
            tipText = stringResource(R.string.tip_library),
            onDismiss = { viewModel.dismissTip() }
        )

        if (!hasPermission) {
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = stringResource(R.string.permission_audio_desc),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Button(onClick = { permissionLauncher.launch(audioPermission) }) {
                        Text(text = stringResource(R.string.grant_audio_permission))
                    }
                }
            }
            return@Column
        }

        OutlinedTextField(
            value = state.searchQuery,
            onValueChange = { viewModel.onSearchQueryChanged(it) },
            placeholder = { Text(stringResource(R.string.search_tracks_hint)) },
            leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
            trailingIcon = {
                if (state.searchQuery.isNotEmpty()) {
                    IconButton(onClick = { viewModel.onSearchQueryChanged("") }) {
                        Icon(Icons.Rounded.Close, contentDescription = null)
                    }
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(18.dp),
            modifier = Modifier.fillMaxWidth()
        )

        // Category Chips: All Songs, Albums, Artists
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            val categories = listOf(
                stringResource(R.string.all_songs),
                stringResource(R.string.albums),
                stringResource(R.string.artists)
            )
            categories.forEachIndexed { idx, label ->
                FilterChip(
                    selected = state.selectedCategory == idx,
                    onClick = { viewModel.onCategorySelected(idx) },
                    label = { Text(label) }
                )
            }
        }

        when (state.selectedCategory) {
            1 -> {
                // Albums Carousel + Tracks
                val albums = remember(state.filteredTracks) {
                    state.filteredTracks.groupBy { it.album }
                }
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(bottom = 16.dp)
                ) {
                    item {
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            items(albums.entries.toList()) { (albumName, albumTracks) ->
                                GlassCard(
                                    cornerRadius = 20.dp,
                                    contentPadding = 12.dp,
                                    modifier = Modifier
                                        .width(150.dp)
                                        .clickable {
                                            viewModel.playTrackFromList(albumTracks, 0)
                                            onNavigateToPlayer()
                                        }
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Box(
                                            modifier = Modifier
                                                .size(110.dp)
                                                .clip(RoundedCornerShape(16.dp))
                                                .background(MaterialTheme.colorScheme.surfaceVariant),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            val art = albumTracks.firstOrNull()?.albumArtUri
                                            if (art != null) {
                                                SubcomposeAsyncImage(
                                                    model = art,
                                                    contentDescription = albumName,
                                                    contentScale = ContentScale.Crop,
                                                    modifier = Modifier.fillMaxSize(),
                                                    error = { Icon(Icons.Rounded.Album, null) }
                                                )
                                            } else {
                                                Icon(Icons.Rounded.Album, contentDescription = null)
                                            }
                                        }
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(
                                            text = albumName,
                                            style = MaterialTheme.typography.labelLarge,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = stringResource(R.string.songs_count, albumTracks.size),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            2 -> {
                // Artists List
                val artists = remember(state.filteredTracks) {
                    state.filteredTracks.groupBy { it.artist }.entries.toList()
                }
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(bottom = 16.dp)
                ) {
                    items(artists) { (artistName, artistTracks) ->
                        GlassCard(
                            cornerRadius = 18.dp,
                            contentPadding = 12.dp,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    viewModel.playTrackFromList(artistTracks, 0)
                                    onNavigateToPlayer()
                                }
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Rounded.Person,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.secondary,
                                    modifier = Modifier.size(32.dp)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = artistName,
                                        style = MaterialTheme.typography.titleMedium,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = stringResource(R.string.songs_count, artistTracks.size),
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }

            else -> {
                // All Songs List
                if (state.filteredTracks.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            text = stringResource(R.string.no_songs_found),
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
                        )
                    }
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        contentPadding = PaddingValues(bottom = 16.dp)
                    ) {
                        itemsIndexed(
                            items = state.filteredTracks,
                            key = { _, item -> item.id }
                        ) { index, track ->
                            GlassCard(
                                cornerRadius = 18.dp,
                                contentPadding = 10.dp,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        viewModel.playTrackFromList(state.filteredTracks, index)
                                        onNavigateToPlayer()
                                    }
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(50.dp)
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(MaterialTheme.colorScheme.surfaceVariant),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (track.albumArtUri != null) {
                                            SubcomposeAsyncImage(
                                                model = track.albumArtUri,
                                                contentDescription = track.title,
                                                contentScale = ContentScale.Crop,
                                                modifier = Modifier.fillMaxSize(),
                                                error = { Icon(Icons.Rounded.MusicNote, null) }
                                            )
                                        } else {
                                            Icon(Icons.Rounded.MusicNote, contentDescription = null)
                                        }
                                    }

                                    Spacer(modifier = Modifier.width(12.dp))

                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = track.title,
                                            style = MaterialTheme.typography.titleMedium,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = track.artist,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }

                                    if (state.playlists.isNotEmpty()) {
                                        IconButton(onClick = { trackForPlaylistDialog = track }) {
                                            Icon(
                                                imageVector = Icons.Rounded.PlaylistAdd,
                                                contentDescription = stringResource(R.string.add_to_playlist),
                                                tint = MaterialTheme.colorScheme.secondary
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    trackForPlaylistDialog?.let { selectedTrack ->
        AlertDialog(
            onDismissRequest = { trackForPlaylistDialog = null },
            title = { Text(stringResource(R.string.add_to_playlist)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    state.playlists.forEach { playlist ->
                        TextButton(
                            onClick = {
                                viewModel.addTrackToPlaylist(playlist.id, selectedTrack.id)
                                trackForPlaylistDialog = null
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(text = playlist.name, modifier = Modifier.fillMaxWidth())
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { trackForPlaylistDialog = null }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }
}
