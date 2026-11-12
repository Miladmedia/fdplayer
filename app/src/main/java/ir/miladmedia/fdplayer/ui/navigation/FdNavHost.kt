package ir.miladmedia.fdplayer.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PlaylistPlay
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavHostController
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import ir.miladmedia.fdplayer.R
import ir.miladmedia.fdplayer.ui.bluetooth.BluetoothRulesRoute
import ir.miladmedia.fdplayer.ui.downloads.DownloadsRoute
import ir.miladmedia.fdplayer.ui.library.LibraryRoute
import ir.miladmedia.fdplayer.ui.player.PlayerRoute
import ir.miladmedia.fdplayer.ui.playlists.PlaylistDetailRoute
import ir.miladmedia.fdplayer.ui.playlists.PlaylistsRoute
import ir.miladmedia.fdplayer.ui.settings.SettingsRoute
import ir.miladmedia.fdplayer.ui.sources.SourcesRoute

/** Route constants for every destination in the app. */
object Routes {
    const val PLAYER = "player"
    const val LIBRARY = "library"
    const val DOWNLOADS = "downloads"
    const val PLAYLISTS = "playlists"
    const val PLAYLIST_DETAIL = "playlists/{playlistId}"
    const val SETTINGS = "settings"
    const val SOURCES = "settings/sources"
    const val BLUETOOTH_RULES = "settings/bluetooth"

    fun playlistDetail(id: Long) = "playlists/$id"
}

private data class TabItem(val route: String, val labelRes: Int, val icon: ImageVector)

private val tabs = listOf(
    TabItem(Routes.PLAYER, R.string.tab_player, Icons.Filled.PlayArrow),
    TabItem(Routes.LIBRARY, R.string.tab_library, Icons.Filled.LibraryMusic),
    TabItem(Routes.DOWNLOADS, R.string.tab_downloads, Icons.Filled.Download),
    TabItem(Routes.PLAYLISTS, R.string.tab_playlists, Icons.Filled.PlaylistPlay),
    TabItem(Routes.SETTINGS, R.string.tab_settings, Icons.Filled.Settings)
)

/** Top-level shell: bottom navigation bar wrapping the NavHost. */
@Composable
fun FdNavHost() {
    val navController = rememberNavController()
    val backStack by navController.currentBackStackEntryAsState()
    val currentRoute = backStack?.destination?.route

    Scaffold(
        bottomBar = {
            NavigationBar {
                tabs.forEach { tab ->
                    val selected = currentRoute == tab.route ||
                        (tab.route == Routes.SETTINGS && currentRoute?.startsWith("settings") == true) ||
                        (tab.route == Routes.PLAYLISTS && currentRoute?.startsWith("playlists") == true)
                    NavigationBarItem(
                        selected = selected,
                        onClick = { navController.navigateToTab(tab.route) },
                        icon = { Icon(tab.icon, contentDescription = null) },
                        label = { Text(stringResource(tab.labelRes)) }
                    )
                }
            }
        }
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = Routes.PLAYER,
            modifier = Modifier.padding(padding)
        ) {
            composable(Routes.PLAYER) {
                PlayerRoute()
            }
            composable(Routes.LIBRARY) {
                LibraryRoute()
            }
            composable(Routes.DOWNLOADS) {
                DownloadsRoute()
            }
            composable(Routes.PLAYLISTS) {
                PlaylistsRoute(
                    onOpen = { id -> navController.navigate(Routes.playlistDetail(id)) }
                )
            }
            composable(
                Routes.PLAYLIST_DETAIL,
                arguments = listOf(navArgument("playlistId") { type = NavType.LongType })
            ) { entry ->
                PlaylistDetailRoute(
                    playlistId = entry.arguments?.getLong("playlistId") ?: 0L,
                    onBack = { navController.popBackStack() }
                )
            }
            composable(Routes.SETTINGS) {
                SettingsRoute(
                    onOpenSources = { navController.navigate(Routes.SOURCES) },
                    onOpenBluetooth = { navController.navigate(Routes.BLUETOOTH_RULES) }
                )
            }
            composable(Routes.SOURCES) {
                SourcesRoute(onBack = { navController.popBackStack() })
            }
            composable(Routes.BLUETOOTH_RULES) {
                BluetoothRulesRoute(onBack = { navController.popBackStack() })
            }
        }
    }
}

private fun NavHostController.navigateToTab(route: String) {
    navigate(route) {
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}
