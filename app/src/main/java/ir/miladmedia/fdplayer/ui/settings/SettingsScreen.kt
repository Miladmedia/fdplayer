package ir.miladmedia.fdplayer.ui.settings

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BatteryChargingFull
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.HelpOutline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ir.miladmedia.fdplayer.R
import ir.miladmedia.fdplayer.domain.model.AppThemeMode
import ir.miladmedia.fdplayer.domain.model.CustomPalettes
import ir.miladmedia.fdplayer.ui.components.FirstTimeTipCard
import ir.miladmedia.fdplayer.ui.components.GlassCard

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        contentPadding = PaddingValues(bottom = 24.dp)
    ) {
        item {
            Text(
                text = stringResource(R.string.settings_title),
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onBackground
            )
        }

        item {
            FirstTimeTipCard(
                visible = state.showFirstTimeTip,
                tipText = stringResource(R.string.tip_settings),
                onDismiss = { viewModel.dismissTip() }
            )
        }

        // 4-Mode Theme Selector + 8 Custom Palettes
        item {
            GlassCard(cornerRadius = 22.dp, contentPadding = 16.dp) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = stringResource(R.string.theme_section),
                        style = MaterialTheme.typography.titleMedium
                    )

                    val themeOptions = listOf(
                        AppThemeMode.DARK to stringResource(R.string.theme_dark),
                        AppThemeMode.LIGHT to stringResource(R.string.theme_light),
                        AppThemeMode.SYSTEM to stringResource(R.string.theme_system),
                        AppThemeMode.CUSTOM to stringResource(R.string.theme_custom)
                    )

                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        themeOptions.forEach { (mode, label) ->
                            FilterChip(
                                selected = state.preferences.themeMode == mode,
                                onClick = { viewModel.setThemeMode(mode) },
                                label = { Text(label) }
                            )
                        }
                    }

                    if (state.preferences.themeMode == AppThemeMode.CUSTOM) {
                        Text(
                            text = stringResource(R.string.custom_palette_title),
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            CustomPalettes.forEach { palette ->
                                val selected = state.preferences.customPaletteIndex == palette.index
                                Box(
                                    modifier = Modifier
                                        .size(46.dp)
                                        .clip(CircleShape)
                                        .background(
                                            Brush.linearGradient(
                                                listOf(
                                                    Color(palette.primaryHex),
                                                    Color(palette.secondaryHex)
                                                )
                                            )
                                        )
                                        .border(
                                            width = if (selected) 3.dp else 1.dp,
                                            color = if (selected) {
                                                MaterialTheme.colorScheme.onBackground
                                            } else {
                                                Color.White.copy(alpha = 0.4f)
                                            },
                                            shape = CircleShape
                                        )
                                        .clickable { viewModel.setCustomPaletteIndex(palette.index) },
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (selected) {
                                        Icon(
                                            imageVector = Icons.Rounded.Check,
                                            contentDescription = palette.nameEn,
                                            tint = Color.White
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Per-App Language Selector (Persian default, Arabic, English)
        item {
            GlassCard(cornerRadius = 22.dp, contentPadding = 16.dp) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = stringResource(R.string.language_section),
                        style = MaterialTheme.typography.titleMedium
                    )
                    val languages = listOf(
                        "fa" to stringResource(R.string.lang_fa),
                        "ar" to stringResource(R.string.lang_ar),
                        "en" to stringResource(R.string.lang_en)
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        languages.forEach { (tag, label) ->
                            FilterChip(
                                selected = state.preferences.languageTag.startsWith(tag),
                                onClick = { viewModel.setLanguage(tag) },
                                label = { Text(label) }
                            )
                        }
                    }
                }
            }
        }

        // Battery Unrestricted Guidance for Bluetooth Autoplay
        item {
            GlassCard(cornerRadius = 22.dp, contentPadding = 16.dp) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Rounded.BatteryChargingFull,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.secondary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = stringResource(R.string.battery_tip_title),
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                    Text(
                        text = stringResource(R.string.battery_tip_desc),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    FilledTonalButton(
                        onClick = {
                            val intent = Intent(
                                Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                                Uri.fromParts("package", context.packageName, null)
                            )
                            context.startActivity(intent)
                        }
                    ) {
                        Text(stringResource(R.string.open_battery_settings))
                    }
                }
            }
        }

        // Colloquial App Guide Dialog Trigger + Brand Footer
        item {
            Button(
                onClick = { viewModel.openGuideDialog() },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Rounded.HelpOutline, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(stringResource(R.string.guide_button))
            }
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = "${stringResource(R.string.app_brand)} • v1.0.0",
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
            )
        }
    }

    if (state.showGuideDialog) {
        AlertDialog(
            onDismissRequest = { viewModel.closeGuideDialog() },
            title = { Text(stringResource(R.string.guide_dialog_title)) },
            text = { Text(stringResource(R.string.guide_dialog_body)) },
            confirmButton = {
                TextButton(onClick = { viewModel.closeGuideDialog() }) {
                    Text(stringResource(R.string.got_it))
                }
            }
        )
    }
}
