package ir.miladmedia.fdplayer.ui.settings

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import ir.miladmedia.fdplayer.R
import ir.miladmedia.fdplayer.domain.model.AppSettings
import ir.miladmedia.fdplayer.domain.model.ThemeMode

private const val DEVELOPER_URL = "https://ble.ir/miladmedia_ir"

@Composable
fun SettingsRoute(
    onOpenSources: () -> Unit,
    onOpenBluetooth: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val settings by viewModel.settings.collectAsState()
    SettingsScreen(
        settings = settings,
        onTheme = viewModel::setTheme,
        onLanguage = viewModel::setLanguage,
        onWifiOnly = viewModel::setWifiOnly,
        onAudioFocus = viewModel::setAudioFocus,
        onKeepScreenOn = viewModel::setKeepScreenOn,
        onStorageCap = viewModel::setStorageCap,
        onSearchLimit = viewModel::setSearchLimit,
        onStorageFolder = viewModel::setStorageFolder,
        onResetStorageFolder = viewModel::resetStorageFolder,
        onBtMonitor = viewModel::setBtMonitor,
        onOpenSources = onOpenSources,
        onOpenBluetooth = onOpenBluetooth,
        onShowTutorial = { viewModel.setOnboardingSeen(false) }
    )
}

@Composable
fun SettingsScreen(
    settings: AppSettings,
    onTheme: (ThemeMode) -> Unit,
    onLanguage: (String) -> Unit,
    onWifiOnly: (Boolean) -> Unit,
    onAudioFocus: (Boolean) -> Unit,
    onKeepScreenOn: (Boolean) -> Unit,
    onStorageCap: (Int) -> Unit,
    onSearchLimit: (Int) -> Unit,
    onStorageFolder: (Uri) -> Unit,
    onResetStorageFolder: () -> Unit,
    onBtMonitor: (Boolean) -> Unit,
    onOpenSources: () -> Unit,
    onOpenBluetooth: () -> Unit,
    onShowTutorial: () -> Unit
) {
    val context = LocalContext.current
    var showTheme by remember { mutableStateOf(false) }
    var showLanguage by remember { mutableStateOf(false) }
    var showCap by remember { mutableStateOf(false) }
    var showLimit by remember { mutableStateOf(false) }

    val folderPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocumentTree()
    ) { uri -> uri?.let(onStorageFolder) }

    val themeLabel = when (settings.theme) {
        ThemeMode.SYSTEM -> stringResource(R.string.theme_system)
        ThemeMode.LIGHT -> stringResource(R.string.theme_light)
        ThemeMode.DARK -> stringResource(R.string.theme_dark)
        ThemeMode.RANDOM -> stringResource(R.string.theme_random)
    }
    val languageLabel = when (settings.language) {
        "fa" -> stringResource(R.string.lang_fa)
        "ar" -> stringResource(R.string.lang_ar)
        "en" -> stringResource(R.string.lang_en)
        else -> stringResource(R.string.lang_system)
    }

    LazyColumn(modifier = Modifier.fillMaxSize()) {
        item { SectionHeader(stringResource(R.string.settings_theme)) }
        item {
            ListItem(
                headlineContent = { Text(stringResource(R.string.settings_theme)) },
                supportingContent = { Text(themeLabel) },
                modifier = Modifier.clickable { showTheme = true }
            )
        }
        item {
            ListItem(
                headlineContent = { Text(stringResource(R.string.settings_language)) },
                supportingContent = { Text(languageLabel) },
                modifier = Modifier.clickable { showLanguage = true }
            )
        }

        item { HorizontalDivider() }
        item { SectionHeader(stringResource(R.string.sources)) }
        item {
            ListItem(
                headlineContent = { Text(stringResource(R.string.sources)) },
                modifier = Modifier.clickable { onOpenSources() }
            )
        }
        item {
            ListItem(
                headlineContent = { Text(stringResource(R.string.settings_search_limit)) },
                supportingContent = { Text(settings.searchLimit.toString()) },
                modifier = Modifier.clickable { showLimit = true }
            )
        }
        item {
            ListItem(
                headlineContent = { Text(stringResource(R.string.settings_wifi_only)) },
                trailingContent = { Switch(checked = settings.wifiOnly, onCheckedChange = onWifiOnly) }
            )
        }
        item {
            ListItem(
                headlineContent = { Text(stringResource(R.string.settings_storage)) },
                supportingContent = {
                    Text(settings.storageTreeUri ?: stringResource(R.string.settings_folder_default))
                },
                modifier = Modifier.clickable { folderPicker.launch(null) }
            )
        }
        if (settings.storageTreeUri != null) {
            item {
                TextButton(
                    onClick = onResetStorageFolder,
                    modifier = Modifier.padding(horizontal = 12.dp)
                ) { Text(stringResource(R.string.settings_folder_default)) }
            }
        }
        item {
            ListItem(
                headlineContent = { Text(stringResource(R.string.storage_cap)) },
                supportingContent = { Text(settings.storageCapMb.toString()) },
                modifier = Modifier.clickable { showCap = true }
            )
        }

        item { HorizontalDivider() }
        item { SectionHeader(stringResource(R.string.settings_audio_focus)) }
        item {
            ListItem(
                headlineContent = { Text(stringResource(R.string.settings_audio_focus)) },
                trailingContent = { Switch(checked = settings.audioFocus, onCheckedChange = onAudioFocus) }
            )
        }
        item {
            ListItem(
                headlineContent = { Text(stringResource(R.string.keep_screen_on)) },
                trailingContent = {
                    Switch(checked = settings.keepScreenOnDriving, onCheckedChange = onKeepScreenOn)
                }
            )
        }

        item { HorizontalDivider() }
        item { SectionHeader(stringResource(R.string.settings_bluetooth)) }
        item {
            ListItem(
                headlineContent = { Text(stringResource(R.string.bt_monitor)) },
                trailingContent = { Switch(checked = settings.btMonitorEnabled, onCheckedChange = onBtMonitor) }
            )
        }
        item {
            ListItem(
                headlineContent = { Text(stringResource(R.string.settings_bluetooth)) },
                modifier = Modifier.clickable { onOpenBluetooth() }
            )
        }

        item { HorizontalDivider() }
        item { SectionHeader(stringResource(R.string.about)) }
        item {
            ListItem(
                headlineContent = { Text(stringResource(R.string.settings_tutorial)) },
                modifier = Modifier.clickable { onShowTutorial() }
            )
        }
        item {
            ListItem(
                headlineContent = { Text(stringResource(R.string.developer)) },
                supportingContent = { Text("MiladMedia") },
                modifier = Modifier.clickable {
                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(DEVELOPER_URL)))
                }
            )
        }
    }

    if (showTheme) {
        ChoiceDialog(
            title = stringResource(R.string.settings_theme),
            options = listOf(
                ThemeMode.SYSTEM to stringResource(R.string.theme_system),
                ThemeMode.LIGHT to stringResource(R.string.theme_light),
                ThemeMode.DARK to stringResource(R.string.theme_dark),
                ThemeMode.RANDOM to stringResource(R.string.theme_random)
            ),
            selected = settings.theme,
            onSelect = { onTheme(it); showTheme = false },
            onDismiss = { showTheme = false }
        )
    }

    if (showLanguage) {
        ChoiceDialog(
            title = stringResource(R.string.settings_language),
            options = listOf(
                "" to stringResource(R.string.lang_system),
                "fa" to stringResource(R.string.lang_fa),
                "ar" to stringResource(R.string.lang_ar),
                "en" to stringResource(R.string.lang_en)
            ),
            selected = settings.language,
            onSelect = { onLanguage(it); showLanguage = false },
            onDismiss = { showLanguage = false }
        )
    }

    if (showCap) {
        NumberDialog(
            title = stringResource(R.string.storage_cap),
            initial = settings.storageCapMb,
            onConfirm = { onStorageCap(it); showCap = false },
            onDismiss = { showCap = false }
        )
    }

    if (showLimit) {
        NumberDialog(
            title = stringResource(R.string.settings_search_limit),
            initial = settings.searchLimit,
            onConfirm = { onSearchLimit(it); showLimit = false },
            onDismiss = { showLimit = false }
        )
    }
}

@Composable
private fun SectionHeader(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        fontWeight = FontWeight.Bold,
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 4.dp)
    )
}

@Composable
private fun <T> ChoiceDialog(
    title: String,
    options: List<Pair<T, String>>,
    selected: T,
    onSelect: (T) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column {
                options.forEach { (value, label) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelect(value) }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(selected = value == selected, onClick = null)
                        Text(label, modifier = Modifier.padding(start = 8.dp))
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
        }
    )
}

@Composable
private fun NumberDialog(
    title: String,
    initial: Int,
    onConfirm: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    var text by remember { mutableStateOf(initial.toString()) }
    val parsed = text.toIntOrNull()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it.filter(Char::isDigit).take(6) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
            )
        },
        confirmButton = {
            TextButton(
                onClick = { parsed?.let(onConfirm) },
                enabled = parsed != null
            ) { Text(stringResource(R.string.save)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
        }
    )
}
