package ir.miladmedia.fdplayer.ui.sources

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
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
import ir.miladmedia.fdplayer.domain.model.CheckResult
import ir.miladmedia.fdplayer.domain.model.Source
import ir.miladmedia.fdplayer.ui.common.EmptyState

@Composable
fun SourcesRoute(
    onBack: () -> Unit,
    viewModel: SourcesViewModel = hiltViewModel()
) {
    val sources by viewModel.sources.collectAsState()
    val testing by viewModel.testing.collectAsState()
    val results by viewModel.results.collectAsState()
    SourcesScreen(
        sources = sources,
        testing = testing,
        results = results,
        onBack = onBack,
        onAdd = viewModel::add,
        onRemove = viewModel::remove,
        onTest = viewModel::test
    )
}

@Composable
fun SourcesScreen(
    sources: List<Source>,
    testing: Set<Long>,
    results: Map<Long, List<CheckResult>>,
    onBack: () -> Unit,
    onAdd: (String, String) -> Boolean,
    onRemove: (Long) -> Unit,
    onTest: (Source) -> Unit
) {
    var showAdd by remember { mutableStateOf(false) }

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
                text = stringResource(R.string.sources),
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(horizontal = 4.dp)
            )
        }

        Box(modifier = Modifier.fillMaxSize()) {
            if (sources.isEmpty()) {
                EmptyState(
                    text = stringResource(R.string.empty_list),
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(sources, key = { it.id }) { source ->
                        val isTesting = source.id in testing
                        val checks = results[source.id].orEmpty()
                        ListItem(
                            headlineContent = { Text(source.name) },
                            supportingContent = {
                                Column {
                                    Text(source.url, style = MaterialTheme.typography.bodySmall)
                                    checks.forEach { check ->
                                        Text(
                                            text = checkLine(check),
                                            style = MaterialTheme.typography.bodySmall,
                                            color = if (check.passed) {
                                                MaterialTheme.colorScheme.primary
                                            } else {
                                                MaterialTheme.colorScheme.error
                                            }
                                        )
                                    }
                                }
                            },
                            trailingContent = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    if (isTesting) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.padding(end = 8.dp),
                                            strokeWidth = 2.dp
                                        )
                                    } else {
                                        TextButton(onClick = { onTest(source) }) {
                                            Text(stringResource(R.string.test_source))
                                        }
                                    }
                                    IconButton(onClick = { onRemove(source.id) }) {
                                        Icon(
                                            Icons.Filled.Delete,
                                            contentDescription = stringResource(R.string.remove)
                                        )
                                    }
                                }
                            }
                        )
                    }
                }
            }

            FloatingActionButton(
                onClick = { showAdd = true },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(16.dp)
            ) {
                Icon(Icons.Filled.Add, contentDescription = stringResource(R.string.add_source))
            }
        }
    }

    if (showAdd) {
        AddSourceDialog(
            onAdd = { name, url ->
                val ok = onAdd(name, url)
                if (ok) showAdd = false
                ok
            },
            onDismiss = { showAdd = false }
        )
    }
}

@Composable
private fun checkLine(check: CheckResult): String {
    val label = stringResource(if (check.passed) R.string.test_pass else R.string.test_fail)
    return if (check.reason.isBlank()) label else "$label: ${check.reason}"
}

@Composable
private fun AddSourceDialog(
    onAdd: (String, String) -> Boolean,
    onDismiss: () -> Unit
) {
    var name by remember { mutableStateOf("") }
    var url by remember { mutableStateOf("") }
    var rejected by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.add_source)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it; rejected = false },
                    label = { Text(stringResource(R.string.source_name)) },
                    singleLine = true
                )
                OutlinedTextField(
                    value = url,
                    onValueChange = { url = it; rejected = false },
                    label = { Text(stringResource(R.string.source_url)) },
                    singleLine = true,
                    isError = rejected
                )
                if (rejected) {
                    Text(
                        text = stringResource(R.string.invalid_url),
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { rejected = !onAdd(name, url) }) {
                Text(stringResource(R.string.save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
        }
    )
}
