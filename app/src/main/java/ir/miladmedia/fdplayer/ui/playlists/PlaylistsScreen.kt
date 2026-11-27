package ir.miladmedia.fdplayer.ui.playlists

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Bluetooth
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ir.miladmedia.fdplayer.R
import ir.miladmedia.fdplayer.domain.model.BluetoothRule
import ir.miladmedia.fdplayer.domain.model.Playlist
import ir.miladmedia.fdplayer.ui.components.FirstTimeTipCard
import ir.miladmedia.fdplayer.ui.components.GlassCard
import java.util.Calendar
import java.util.Locale

@Composable
fun PlaylistsScreen(
    onNavigateToPlayer: () -> Unit,
    viewModel: PlaylistsViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var showCreatePlaylistDialog by remember { mutableStateOf(false) }
    var showAddRuleDialog by remember { mutableStateOf(false) }

    val btPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        viewModel.onBluetoothPermissionUpdated(granted)
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(bottom = 24.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.playlists_title),
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onBackground
                )
                FilledTonalButton(onClick = { showCreatePlaylistDialog = true }) {
                    Icon(Icons.Rounded.Add, contentDescription = null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(stringResource(R.string.new_playlist))
                }
            }
        }

        item {
            FirstTimeTipCard(
                visible = state.showFirstTimeTip,
                tipText = stringResource(R.string.tip_playlists),
                onDismiss = { viewModel.dismissTip() }
            )
        }

        // User Playlists Section
        items(state.playlists, key = { "pl_${it.id}" }) { playlist ->
            GlassCard(cornerRadius = 20.dp, contentPadding = 14.dp) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = playlist.name,
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = stringResource(R.string.songs_count, playlist.trackIds.size),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    IconButton(
                        onClick = {
                            if (playlist.trackIds.isNotEmpty()) {
                                viewModel.playPlaylist(playlist)
                                onNavigateToPlayer()
                            }
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.PlayArrow,
                            contentDescription = stringResource(R.string.play_all),
                            tint = MaterialTheme.colorScheme.secondary
                        )
                    }

                    IconButton(onClick = { viewModel.deletePlaylist(playlist.id) }) {
                        Icon(
                            imageVector = Icons.Rounded.Delete,
                            contentDescription = stringResource(R.string.delete),
                            tint = MaterialTheme.colorScheme.tertiary
                        )
                    }
                }
            }
        }

        // Bluetooth Autoplay Section
        item {
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Rounded.Bluetooth,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.secondary
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = stringResource(R.string.bt_autoplay_section),
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }

                FilledTonalButton(
                    onClick = {
                        viewModel.refreshPairedDevices()
                        showAddRuleDialog = true
                    }
                ) {
                    Icon(Icons.Rounded.Add, contentDescription = null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(stringResource(R.string.add_bt_rule))
                }
            }
        }

        if (!state.hasBluetoothPermission && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            item {
                GlassCard(cornerRadius = 18.dp, contentPadding = 14.dp) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = stringResource(R.string.bt_permission_needed),
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Button(
                            onClick = {
                                btPermissionLauncher.launch(Manifest.permission.BLUETOOTH_CONNECT)
                            }
                        ) {
                            Text(stringResource(R.string.grant_bt_permission))
                        }
                    }
                }
            }
        }

        items(state.rules, key = { "rule_${it.id}" }) { rule ->
            val targetPlaylistName = state.playlists.find { it.id == rule.playlistId }?.name
                ?: stringResource(R.string.current_queue_fallback)

            GlassCard(cornerRadius = 20.dp, contentPadding = 14.dp) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = rule.deviceName,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        val timeText = String.format(
                            Locale.US,
                            "%02d:%02d - %02d:%02d",
                            rule.startHour,
                            rule.startMinute,
                            rule.endHour,
                            rule.endMinute
                        )
                        Text(
                            text = "$timeText • $targetPlaylistName",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Switch(
                        checked = rule.enabled,
                        onCheckedChange = { viewModel.toggleBluetoothRule(rule.id, it) }
                    )

                    IconButton(onClick = { viewModel.deleteBluetoothRule(rule.id) }) {
                        Icon(
                            imageVector = Icons.Rounded.Delete,
                            contentDescription = stringResource(R.string.delete),
                            tint = MaterialTheme.colorScheme.tertiary
                        )
                    }
                }
            }
        }
    }

    if (showCreatePlaylistDialog) {
        var nameInput by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showCreatePlaylistDialog = false },
            title = { Text(stringResource(R.string.new_playlist)) },
            text = {
                OutlinedTextField(
                    value = nameInput,
                    onValueChange = { nameInput = it },
                    placeholder = { Text(stringResource(R.string.playlist_name_hint)) },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp)
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.createPlaylist(nameInput)
                        showCreatePlaylistDialog = false
                    }
                ) {
                    Text(stringResource(R.string.create))
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreatePlaylistDialog = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

    if (showAddRuleDialog) {
        AddBluetoothRuleDialog(
            pairedDevices = state.pairedDevices,
            playlists = state.playlists,
            allDevicesLabel = stringResource(R.string.all_bluetooth_devices),
            currentQueueLabel = stringResource(R.string.current_queue_fallback),
            onDismiss = { showAddRuleDialog = false },
            onSave = { rule ->
                viewModel.saveBluetoothRule(rule)
                showAddRuleDialog = false
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun AddBluetoothRuleDialog(
    pairedDevices: List<PairedBtDevice>,
    playlists: List<Playlist>,
    allDevicesLabel: String,
    currentQueueLabel: String,
    onDismiss: () -> Unit,
    onSave: (BluetoothRule) -> Unit
) {
    var selectedDeviceAddress by remember { mutableStateOf<String?>(null) }
    var selectedDeviceName by remember { mutableStateOf(allDevicesLabel) }
    var deviceDropdownExpanded by remember { mutableStateOf(false) }

    var selectedPlaylistId by remember { mutableStateOf<Long?>(null) }
    var selectedPlaylistName by remember { mutableStateOf(currentQueueLabel) }
    var playlistDropdownExpanded by remember { mutableStateOf(false) }

    var startHour by remember { mutableIntStateOf(0) }
    var startMinute by remember { mutableIntStateOf(0) }
    var endHour by remember { mutableIntStateOf(23) }
    var endMinute by remember { mutableIntStateOf(59) }

    var pickingStartTime by remember { mutableStateOf(false) }
    var pickingEndTime by remember { mutableStateOf(false) }

    var selectedDays by remember {
        mutableStateOf(
            setOf(
                Calendar.SATURDAY,
                Calendar.SUNDAY,
                Calendar.MONDAY,
                Calendar.TUESDAY,
                Calendar.WEDNESDAY,
                Calendar.THURSDAY,
                Calendar.FRIDAY
            )
        )
    }

    var shuffle by remember { mutableStateOf(false) }
    var latencySec by remember { mutableIntStateOf(0) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.add_bt_rule)) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Device Picker (All Devices or specific paired device)
                ExposedDropdownMenuBox(
                    expanded = deviceDropdownExpanded,
                    onExpandedChange = { deviceDropdownExpanded = !deviceDropdownExpanded }
                ) {
                    OutlinedTextField(
                        value = selectedDeviceName,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text(stringResource(R.string.select_bt_device)) },
                        trailingIcon = {
                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = deviceDropdownExpanded)
                        },
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = deviceDropdownExpanded,
                        onDismissRequest = { deviceDropdownExpanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text(allDevicesLabel) },
                            onClick = {
                                selectedDeviceAddress = null
                                selectedDeviceName = allDevicesLabel
                                deviceDropdownExpanded = false
                            }
                        )
                        pairedDevices.forEach { dev ->
                            DropdownMenuItem(
                                text = { Text("${dev.name} (${dev.address})") },
                                onClick = {
                                    selectedDeviceAddress = dev.address
                                    selectedDeviceName = dev.name
                                    deviceDropdownExpanded = false
                                }
                            )
                        }
                    }
                }

                // Target Playlist Picker
                ExposedDropdownMenuBox(
                    expanded = playlistDropdownExpanded,
                    onExpandedChange = { playlistDropdownExpanded = !playlistDropdownExpanded }
                ) {
                    OutlinedTextField(
                        value = selectedPlaylistName,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text(stringResource(R.string.target_playlist)) },
                        trailingIcon = {
                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = playlistDropdownExpanded)
                        },
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = playlistDropdownExpanded,
                        onDismissRequest = { playlistDropdownExpanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text(currentQueueLabel) },
                            onClick = {
                                selectedPlaylistId = null
                                selectedPlaylistName = currentQueueLabel
                                playlistDropdownExpanded = false
                            }
                        )
                        playlists.forEach { pl ->
                            DropdownMenuItem(
                                text = { Text(pl.name) },
                                onClick = {
                                    selectedPlaylistId = pl.id
                                    selectedPlaylistName = pl.name
                                    playlistDropdownExpanded = false
                                }
                            )
                        }
                    }
                }

                // Time Window Buttons
                Text(
                    text = stringResource(R.string.time_range),
                    style = MaterialTheme.typography.labelLarge
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = { pickingStartTime = true },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            String.format(
                                Locale.US,
                                "%s: %02d:%02d",
                                stringResource(R.string.start_time),
                                startHour,
                                startMinute
                            )
                        )
                    }
                    OutlinedButton(
                        onClick = { pickingEndTime = true },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            String.format(
                                Locale.US,
                                "%s: %02d:%02d",
                                stringResource(R.string.end_time),
                                endHour,
                                endMinute
                            )
                        )
                    }
                }

                // Weekdays Chips (Set<Int>)
                Text(
                    text = stringResource(R.string.weekdays),
                    style = MaterialTheme.typography.labelLarge
                )
                val dayItems = listOf(
                    Calendar.SATURDAY to stringResource(R.string.day_sat),
                    Calendar.SUNDAY to stringResource(R.string.day_sun),
                    Calendar.MONDAY to stringResource(R.string.day_mon),
                    Calendar.TUESDAY to stringResource(R.string.day_tue),
                    Calendar.WEDNESDAY to stringResource(R.string.day_wed),
                    Calendar.THURSDAY to stringResource(R.string.day_thu),
                    Calendar.FRIDAY to stringResource(R.string.day_fri)
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    dayItems.forEach { (dayConst, label) ->
                        val selected = dayConst in selectedDays
                        FilterChip(
                            selected = selected,
                            onClick = {
                                selectedDays = if (selected) {
                                    selectedDays - dayConst
                                } else {
                                    selectedDays + dayConst
                                }
                            },
                            label = { Text(label) }
                        )
                    }
                }

                // Extra Latency Slider
                Text(
                    text = stringResource(R.string.startup_delay_sec, latencySec),
                    style = MaterialTheme.typography.labelLarge
                )
                Slider(
                    value = latencySec.toFloat(),
                    onValueChange = { latencySec = it.toInt() },
                    valueRange = 0f..15f,
                    steps = 14
                )

                // Shuffle Toggle
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { shuffle = !shuffle },
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(stringResource(R.string.shuffle_on_connect))
                    Switch(checked = shuffle, onCheckedChange = { shuffle = it })
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onSave(
                        BluetoothRule(
                            deviceAddress = selectedDeviceAddress,
                            deviceName = selectedDeviceName,
                            startHour = startHour,
                            startMinute = startMinute,
                            endHour = endHour,
                            endMinute = endMinute,
                            weekdays = selectedDays,
                            playlistId = selectedPlaylistId,
                            shuffle = shuffle,
                            latencySec = latencySec,
                            enabled = true
                        )
                    )
                }
            ) {
                Text(stringResource(R.string.save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        }
    )

    if (pickingStartTime) {
        val pickerState = rememberTimePickerState(initialHour = startHour, initialMinute = startMinute, is24Hour = true)
        AlertDialog(
            onDismissRequest = { pickingStartTime = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        startHour = pickerState.hour
                        startMinute = pickerState.minute
                        pickingStartTime = false
                    }
                ) { Text(stringResource(R.string.save)) }
            },
            dismissButton = {
                TextButton(onClick = { pickingStartTime = false }) { Text(stringResource(R.string.cancel)) }
            },
            text = { TimePicker(state = pickerState) }
        )
    }

    if (pickingEndTime) {
        val pickerState = rememberTimePickerState(initialHour = endHour, initialMinute = endMinute, is24Hour = true)
        AlertDialog(
            onDismissRequest = { pickingEndTime = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        endHour = pickerState.hour
                        endMinute = pickerState.minute
                        pickingEndTime = false
                    }
                ) { Text(stringResource(R.string.save)) }
            },
            dismissButton = {
                TextButton(onClick = { pickingEndTime = false }) { Text(stringResource(R.string.cancel)) }
            },
            text = { TimePicker(state = pickerState) }
        )
    }
}
