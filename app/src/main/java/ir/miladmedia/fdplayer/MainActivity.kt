package ir.miladmedia.fdplayer

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.LibraryMusic
import androidx.compose.material.icons.rounded.PlayCircle
import androidx.compose.material.icons.rounded.QueueMusic
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.core.os.LocaleListCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import dagger.hilt.android.AndroidEntryPoint
import ir.miladmedia.fdplayer.domain.model.UserPreferences
import ir.miladmedia.fdplayer.domain.repository.SettingsRepository
import ir.miladmedia.fdplayer.service.BluetoothAutoplayService
import ir.miladmedia.fdplayer.ui.downloads.DownloadsScreen
import ir.miladmedia.fdplayer.ui.library.LibraryScreen
import ir.miladmedia.fdplayer.ui.player.PlayerScreen
import ir.miladmedia.fdplayer.ui.playlists.PlaylistsScreen
import ir.miladmedia.fdplayer.ui.settings.SettingsScreen
import ir.miladmedia.fdplayer.ui.theme.FDPlayerTheme
import javax.inject.Inject

private sealed class BottomTab(
    val route: String,
    val labelRes: Int,
    val icon: ImageVector
) {
    data object Player : BottomTab("player", R.string.tab_player, Icons.Rounded.PlayCircle)
    data object Library : BottomTab("library", R.string.tab_library, Icons.Rounded.LibraryMusic)
    data object Downloads : BottomTab("downloads", R.string.tab_downloads, Icons.Rounded.Download)
    data object Playlists : BottomTab("playlists", R.string.tab_playlists, Icons.Rounded.QueueMusic)
    data object Settings : BottomTab("settings", R.string.tab_settings, Icons.Rounded.Settings)
}

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    @Inject
    lateinit var settingsRepository: SettingsRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val prefs by settingsRepository.observePreferences()
                .collectAsStateWithLifecycle(initialValue = UserPreferences())

            LaunchedEffect(prefs.languageTag) {
                val currentLocales = AppCompatDelegate.getApplicationLocales()
                if (currentLocales.isEmpty) {
                    AppCompatDelegate.setApplicationLocales(
                        LocaleListCompat.forLanguageTags(prefs.languageTag)
                    )
                }
            }

            FDPlayerTheme(
                themeMode = prefs.themeMode,
                customPaletteIndex = prefs.customPaletteIndex
            ) {
                FDPlayerMainApp()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        BluetoothAutoplayService.startIfPermitted(this)
    }
}

@Composable
private fun FDPlayerMainApp() {
    val navController = rememberNavController()
    val tabs = listOf(
        BottomTab.Player,
        BottomTab.Library,
        BottomTab.Downloads,
        BottomTab.Playlists,
        BottomTab.Settings
    )

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            NavigationBar {
                tabs.forEach { tab ->
                    val label = stringResource(tab.labelRes)
                    NavigationBarItem(
                        selected = currentRoute == tab.route,
                        onClick = {
                            navController.navigate(tab.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = { Icon(tab.icon, contentDescription = label) },
                        label = {
                            Text(
                                text = label,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = BottomTab.Player.route,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            composable(BottomTab.Player.route) {
                PlayerScreen()
            }
            composable(BottomTab.Library.route) {
                LibraryScreen(
                    onNavigateToPlayer = {
                        navController.navigate(BottomTab.Player.route) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                )
            }
            composable(BottomTab.Downloads.route) {
                DownloadsScreen()
            }
            composable(BottomTab.Playlists.route) {
                PlaylistsScreen(
                    onNavigateToPlayer = {
                        navController.navigate(BottomTab.Player.route) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                )
            }
            composable(BottomTab.Settings.route) {
                SettingsScreen()
            }
        }
    }
}
