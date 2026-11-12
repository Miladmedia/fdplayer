package ir.miladmedia.fdplayer.ui.settings

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import ir.miladmedia.fdplayer.domain.model.AppSettings
import ir.miladmedia.fdplayer.domain.model.ThemeMode
import ir.miladmedia.fdplayer.domain.repository.SettingsRepository
import ir.miladmedia.fdplayer.service.BluetoothMonitorService
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class SettingsViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    val settings: StateFlow<AppSettings> = settingsRepository.settings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AppSettings())

    fun setTheme(mode: ThemeMode) = update { it.copy(theme = mode) }

    fun setLanguage(tag: String) = update { it.copy(language = tag) }

    fun setWifiOnly(enabled: Boolean) = update { it.copy(wifiOnly = enabled) }

    fun setAudioFocus(enabled: Boolean) = update { it.copy(audioFocus = enabled) }

    fun setKeepScreenOn(enabled: Boolean) = update { it.copy(keepScreenOnDriving = enabled) }

    fun setStorageCap(mb: Int) = update { it.copy(storageCapMb = mb.coerceAtLeast(0)) }

    fun setSearchLimit(limit: Int) = update { it.copy(searchLimit = limit.coerceIn(1, 500)) }

    /** Takes a persistable read/write grant for the chosen tree so downloads survive restarts. */
    fun setStorageFolder(uri: Uri) {
        val flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
        context.contentResolver.takePersistableUriPermission(uri, flags)
        update { it.copy(storageTreeUri = uri.toString()) }
    }

    fun resetStorageFolder() = update { it.copy(storageTreeUri = null) }

    /** Persists the toggle and starts or stops the Bluetooth monitor service to match it. */
    fun setBtMonitor(enabled: Boolean) {
        update { it.copy(btMonitorEnabled = enabled) }
        val intent = Intent(context, BluetoothMonitorService::class.java)
        if (enabled) {
            ContextCompat.startForegroundService(context, intent)
        } else {
            context.stopService(intent)
        }
    }

    fun setOnboardingSeen(seen: Boolean) = update { it.copy(onboardingSeen = seen) }

    private fun update(transform: (AppSettings) -> AppSettings) {
        viewModelScope.launch { settingsRepository.update(transform) }
    }
}
