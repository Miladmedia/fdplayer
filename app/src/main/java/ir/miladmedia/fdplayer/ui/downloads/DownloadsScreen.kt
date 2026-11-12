package ir.miladmedia.fdplayer.ui.downloads

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import ir.miladmedia.fdplayer.R
import ir.miladmedia.fdplayer.domain.model.DownloadItem
import ir.miladmedia.fdplayer.domain.model.DownloadStatus
import ir.miladmedia.fdplayer.domain.model.QueueResult
import ir.miladmedia.fdplayer.domain.model.SearchResult
import ir.miladmedia.fdplayer.ui.common.EmptyState

@Composable
fun DownloadsRoute(viewModel: DownloadsViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsState()
    DownloadsScreen(
        state = state,
        onFormChange = viewModel::updateForm,
        onSubmit = viewModel::submit,
        onSearch = viewModel::search,
        onPickResult = viewModel::pickResult,
        onCancel = viewModel::cancel,
        onRetry = viewModel::retry,
        onDelete = viewModel::delete,
        onClearFinished = viewModel::clearFinished,
        onDismissMessage = viewModel::dismissQueueMessage,
        onExtract = viewModel::extract,
        onDownloadAll = viewModel::queueAll,
        onDownloadSingle = viewModel::queueSingle
    )
}

@Composable
fun DownloadsScreen(
    state: DownloadsUiState,
    onFormChange: ((DownloadForm) -> DownloadForm) -> Unit,
    onSubmit: () -> Unit,
    onSearch: () -> Unit,
    onPickResult: (SearchResult) -> Unit,
    onCancel: (DownloadItem) -> Unit,
    onRetry: (DownloadItem) -> Unit,
    onDelete: (DownloadItem) -> Unit,
    onClearFinished: () -> Unit,
    onDismissMessage: () -> Unit,
    onExtract: () -> Unit,
    onDownloadAll: () -> Unit,
    onDownloadSingle: (SearchResult) -> Unit
) {
    val form = state.form
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = form.keyword,
                    onValueChange = { v -> onFormChange { it.copy(keyword = v) } },
                    label = { Text(stringResource(R.string.field_keyword)) },
                    placeholder = { Text(stringResource(R.string.search_hint)) },
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
                Button(
                    onClick = onSearch,
                    enabled = !state.searching,
                    modifier = Modifier.padding(start = 8.dp)
                ) { Text(stringResource(R.string.search)) }
            }
        }

        if (state.searching) {
            item { LinearProgressIndicator(modifier = Modifier.fillMaxWidth()) }
        }

        if (state.results.isNotEmpty()) {
            item {
                Text(
                    stringResource(R.string.search_results),
                    style = MaterialTheme.typography.titleSmall
                )
            }
            items(state.results, key = { it.url }) { r ->
                TextButton(onClick = { onPickResult(r) }, modifier = Modifier.fillMaxWidth()) {
                    Text(
                        if (r.artist.isBlank()) r.title else "${r.title} - ${r.artist}",
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        } else if (state.searchFailed && !state.searching) {
            item { Text(stringResource(R.string.empty_search)) }
        }

        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = form.url,
                    onValueChange = { v -> onFormChange { it.copy(url = v) } },
                    label = { Text(stringResource(R.string.field_url)) },
                    placeholder = { Text(stringResource(R.string.extract_hint)) },
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
                Button(
                    onClick = onExtract,
                    enabled = !state.extracting && form.url.isNotBlank(),
                    modifier = Modifier.padding(start = 8.dp)
                ) {
                    Text(stringResource(R.string.check_link))
                }
            }
        }

        if (state.extracting) {
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    LinearProgressIndicator(modifier = Modifier.weight(1f))
                    Text(
                        stringResource(R.string.checking_link),
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(start = 8.dp)
                    )
                }
            }
        }

        if (state.extracted.isNotEmpty()) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        stringResource(R.string.link_files),
                        style = MaterialTheme.typography.titleSmall
                    )
                    Button(onClick = onDownloadAll) {
                        Text(stringResource(R.string.download_all))
                    }
                }
            }
            items(state.extracted, key = { it.url }) { link ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        if (link.artist.isBlank()) link.title else "${link.title} - ${link.artist}",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.weight(1f).padding(end = 8.dp)
                    )
                    TextButton(onClick = { onDownloadSingle(link) }) {
                        Text(stringResource(R.string.download_single))
                    }
                }
            }
        } else if (state.extractChecked && !state.extracting) {
            item {
                Text(
                    stringResource(R.string.no_links_found),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }
        }
        item {
            OutlinedTextField(
                value = form.title,
                onValueChange = { v -> onFormChange { it.copy(title = v) } },
                label = { Text(stringResource(R.string.field_title)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        }
        item {
            OutlinedTextField(
                value = form.artist,
                onValueChange = { v -> onFormChange { it.copy(artist = v) } },
                label = { Text(stringResource(R.string.field_artist)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        }
        item {
            Button(onClick = onSubmit, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.download))
            }
        }

        state.lastQueue?.let { q ->
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(queueMessage(q), modifier = Modifier.weight(1f))
                    TextButton(onClick = onDismissMessage) { Text(stringResource(R.string.ok)) }
                }
            }
        }

        item {
            HorizontalDivider()
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                OutlinedButton(onClick = onClearFinished) {
                    Text(stringResource(R.string.clear_finished))
                }
            }
        }

        if (state.items.isEmpty()) {
            item { EmptyState(stringResource(R.string.empty_list)) }
        }

        items(state.items, key = { it.id }) { item ->
            DownloadRow(item, onCancel, onRetry, onDelete)
        }
    }
}

@Composable
private fun DownloadRow(
    item: DownloadItem,
    onCancel: (DownloadItem) -> Unit,
    onRetry: (DownloadItem) -> Unit,
    onDelete: (DownloadItem) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Text(
            if (item.artist.isBlank()) item.title else "${item.title} - ${item.artist}",
            style = MaterialTheme.typography.bodyLarge
        )
        Text(
            statusLabel(item.status) + (item.error?.let { " ($it)" } ?: ""),
            style = MaterialTheme.typography.bodySmall
        )
        if (item.status == DownloadStatus.DOWNLOADING && item.totalBytes > 0) {
            LinearProgressIndicator(
                progress = { (item.downloadedBytes.toFloat() / item.totalBytes).coerceIn(0f, 1f) },
                modifier = Modifier.fillMaxWidth()
            )
        }
        Row {
            when (item.status) {
                DownloadStatus.QUEUED, DownloadStatus.DOWNLOADING ->
                    TextButton(onClick = { onCancel(item) }) { Text(stringResource(R.string.cancel)) }
                DownloadStatus.FAILED, DownloadStatus.CANCELED ->
                    TextButton(onClick = { onRetry(item) }) { Text(stringResource(R.string.retry)) }
                else -> Unit
            }
            TextButton(onClick = { onDelete(item) }) { Text(stringResource(R.string.delete)) }
        }
    }
}

@Composable
private fun statusLabel(status: DownloadStatus): String = stringResource(
    when (status) {
        DownloadStatus.QUEUED -> R.string.status_queued
        DownloadStatus.DOWNLOADING -> R.string.status_downloading
        DownloadStatus.DONE -> R.string.status_done
        DownloadStatus.FAILED -> R.string.status_failed
        DownloadStatus.DUPLICATE -> R.string.status_duplicate
        DownloadStatus.CANCELED -> R.string.status_canceled
    }
)

@Composable
private fun queueMessage(q: QueueResult): String = when (q) {
    QueueResult.Queued -> stringResource(R.string.queued_ok)
    is QueueResult.QuotaExhausted -> stringResource(R.string.quota_exhausted, q.cap)
    QueueResult.DuplicateUrl -> stringResource(R.string.duplicate_url)
    QueueResult.DuplicateName -> stringResource(R.string.duplicate_name)
    QueueResult.InvalidUrl -> stringResource(R.string.invalid_url)
}
