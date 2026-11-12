package ir.miladmedia.fdplayer.ui.bluetooth

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Switch
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import ir.miladmedia.fdplayer.R
import ir.miladmedia.fdplayer.domain.model.BondedDevice
import ir.miladmedia.fdplayer.domain.model.BtRule
import ir.miladmedia.fdplayer.domain.model.BtSchedule
import ir.miladmedia.fdplayer.domain.model.Playlist
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

private val WEEKDAY_LABELS = listOf(
    R.string.day_sun,
    R.string.day_mon,
    R.string.day_tue,
    R.string.day_wed,
    R.string.day_thu,
    R.string.day_fri,
    R.string.day_sat
)

@Composable
fun BluetoothRulesRoute(
    onBack: () -> Unit,
    viewModel: BluetoothRulesViewModel = hiltViewModel()
) {
    val rules by viewModel.rules.collectAsState()
    val devices by viewModel.devices.collectAsState()
    val playlists by viewModel.playlists.collectAsState()
    BluetoothRulesScreen(
        devices = devices,
        rules = rules,
        playlists = playlists,
        onBack = onBack,
        onRefresh = viewModel::refreshDevices,
        onSetEnabled = viewModel::setEnabled,
        onSaveRule = viewModel::saveRule,
        schedulesOf = viewModel::schedules,
        onAddSchedule = viewModel::addSchedule,
        onRemoveSchedule = viewModel::removeSchedule
    )
}

@Composable
fun BluetoothRulesScreen(
    devices: List<BondedDevice>,
    rules: List<BtRule>,
    playlists: List<Playlist>,
    onBack: () -> Unit,
    onRefresh: () -> Unit,
    onSetEnabled: (BondedDevice, Boolean) -> Unit,
    onSaveRule: (BtRule) -> Unit,
    schedulesOf: (String) -> Flow<List<BtSchedule>>,
    onAddSchedule: (String, Set<Int>, String, String) -> Boolean,
    onRemoveSchedule: (Long) -> Unit
) {
    var editing by remember { mutableStateOf<String?>(null) }

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
                text = stringResource(R.string.bt_monitor),
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(horizontal = 4.dp)
            )
        }

        if (devices.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = stringResource(R.string.bt_no_devices),
                        style = MaterialTheme.typography.bodyLarge
                    )
                    TextButton(onClick = onRefresh) {
                        Text(stringResource(R.string.bt_refresh))
                    }
                }
            }
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(devices, key = { it.address }) { device ->
                    val rule = rules.firstOrNull { it.address == device.address }
                    ListItem(
                        headlineContent = { Text(device.name) },
                        supportingContent = { Text(device.address, style = MaterialTheme.typography.bodySmall) },
                        modifier = if (rule != null) {
                            Modifier.clickable { editing = device.address }
                        } else {
                            Modifier
                        },
                        trailingContent = {
                            Switch(
                                checked = rule != null,
                                onCheckedChange = { onSetEnabled(device, it) }
                            )
                        }
                    )
                    HorizontalDivider()
                }
            }
        }
    }

    val address = editing
    val scheduleFlow = remember(address) { address?.let { schedulesOf(it) } ?: flowOf(emptyList()) }
    if (address != null) {
        val device = devices.firstOrNull { it.address == address }
        val rule = rules.firstOrNull { it.address == address }
        if (device != null && rule != null) {
            RuleEditorDialog(
                rule = rule,
                playlists = playlists,
                schedules = scheduleFlow,
                onSave = {
                    onSaveRule(it)
                    editing = null
                },
                onDismiss = { editing = null },
                onAddSchedule = { days, start, end -> onAddSchedule(address, days, start, end) },
                onRemoveSchedule = onRemoveSchedule
            )
        } else {
            editing = null
        }
    }
}

@Composable
private fun RuleEditorDialog(
    rule: BtRule,
    playlists: List<Playlist>,
    schedules: Flow<List<BtSchedule>>,
    onSave: (BtRule) -> Unit,
    onDismiss: () -> Unit,
    onAddSchedule: (Set<Int>, String, String) -> Boolean,
    onRemoveSchedule: (Long) -> Unit
) {
    var playlistId by remember(rule) { mutableStateOf(rule.playlistId) }
    var shuffle by remember(rule) { mutableStateOf(rule.shuffle) }
    var latencyText by remember(rule) { mutableStateOf(rule.latencySec.toString()) }
    val scheduleList by schedules.collectAsState(initial = emptyList())

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(rule.name) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = stringResource(R.string.bt_playlist),
                    style = MaterialTheme.typography.titleSmall
                )
                ChoiceRow(
                    label = stringResource(R.string.no_playlist),
                    selected = playlistId == null,
                    onSelect = { playlistId = null }
                )
                playlists.forEach { playlist ->
                    ChoiceRow(
                        label = playlist.name,
                        selected = playlistId == playlist.id,
                        onSelect = { playlistId = playlist.id }
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(stringResource(R.string.bt_shuffle))
                    Switch(checked = shuffle, onCheckedChange = { shuffle = it })
                }

                OutlinedTextField(
                    value = latencyText,
                    onValueChange = { latencyText = it.filter { c -> c.isDigit() }.take(3) },
                    label = { Text(stringResource(R.string.bt_latency)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )

                HorizontalDivider()
                Text(
                    text = stringResource(R.string.bt_schedule),
                    style = MaterialTheme.typography.titleSmall
                )
                if (scheduleList.isEmpty()) {
                    Text(
                        text = stringResource(R.string.bt_schedule_any),
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                scheduleList.forEach { schedule ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${stringResource(WEEKDAY_LABELS[schedule.weekday - 1])}  " +
                                "${formatMinute(schedule.startMinute)} - ${formatMinute(schedule.endMinute)}",
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(onClick = { onRemoveSchedule(schedule.id) }) {
                            Icon(Icons.Filled.Delete, contentDescription = stringResource(R.string.remove))
                        }
                    }
                }
                ScheduleAdder(onAdd = onAddSchedule)
            }
        },
        confirmButton = {
            TextButton(onClick = {
                onSave(
                    rule.copy(
                        playlistId = playlistId,
                        shuffle = shuffle,
                        latencySec = latencyText.toIntOrNull() ?: 0
                    )
                )
            }) {
                Text(stringResource(R.string.save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
        }
    )
}

@Composable
private fun ChoiceRow(label: String, selected: Boolean, onSelect: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(selected = selected, onClick = onSelect)
        Text(label)
    }
}

@Composable
private fun ScheduleAdder(onAdd: (Set<Int>, String, String) -> Boolean) {
    var days by remember { mutableStateOf(setOf(1)) }
    var start by remember { mutableStateOf("08:00") }
    var end by remember { mutableStateOf("18:00") }
    var rejected by remember { mutableStateOf(false) }

    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            WEEKDAY_LABELS.forEachIndexed { index, label ->
                val weekday = index + 1
                FilterChip(
                    selected = weekday in days,
                    onClick = {
                        rejected = false
                        days = if (weekday in days) days - weekday else days + weekday
                    },
                    label = { Text(stringResource(label)) }
                )
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = start,
                onValueChange = { start = it; rejected = false },
                label = { Text(stringResource(R.string.bt_start)) },
                singleLine = true,
                isError = rejected,
                modifier = Modifier.width(120.dp)
            )
            OutlinedTextField(
                value = end,
                onValueChange = { end = it; rejected = false },
                label = { Text(stringResource(R.string.bt_end)) },
                singleLine = true,
                isError = rejected,
                modifier = Modifier.width(120.dp)
            )
        }
        if (rejected) {
            Text(
                text = stringResource(
                    if (days.isEmpty()) R.string.bt_pick_day else R.string.invalid_time
                ),
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall
            )
        }
        OutlinedButton(onClick = {
            val ok = onAdd(days, start, end)
            rejected = !ok
        }) {
            Text(stringResource(R.string.bt_add_schedule))
        }
    }
}

private fun formatMinute(minute: Int): String =
    "%02d:%02d".format(minute / 60, minute % 60)
